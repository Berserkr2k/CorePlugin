package com.github.berserkr2k.coreplugin.domain.user

import com.github.berserkr2k.coreplugin.infrastructure.database.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger

class ProfileRegistry(
    private val databaseService: DatabaseService,
    private val logger: Logger
) {
    private val profiles = ConcurrentHashMap<UUID, UserProfile>()

    fun getProfile(uuid: UUID): UserProfile? {
        return profiles[uuid]
    }

    fun getActiveProfiles(): Collection<UserProfile> {
        return profiles.values
    }

    /**
     * Carga de forma asíncrona un perfil de usuario. Si ya está en caché, lo retorna directamente.
     * Si no existe, realiza un INSERT en core_users y carga su saldo, cooldowns y estelas.
     */
    fun loadProfile(uuid: UUID, username: String): CompletableFuture<UserProfile> {
        val cached = profiles[uuid]
        if (cached != null) {
            return CompletableFuture.completedFuture(cached)
        }

        return CompletableFuture.supplyAsync {
            databaseService.getConnection().use { conn ->
                val originalAutoCommit = conn.autoCommit
                conn.autoCommit = false
                try {
                    var internalId = -1
                    var chatColor: String? = null
                    var socialSpy = false
                    // 1. Intentar obtener el id del usuario por su UUID
                    conn.prepareStatement("SELECT id, username, chat_color, social_spy FROM core_users WHERE uuid = ?").use { stmt ->
                        stmt.setString(1, uuid.toString())
                        stmt.executeQuery().use { rs ->
                            if (rs.next()) {
                                internalId = rs.getInt("id")
                                val dbUsername = rs.getString("username")
                                chatColor = rs.getString("chat_color")
                                socialSpy = rs.getInt("social_spy") == 1
                                if (dbUsername != username) {
                                    // Actualizar nombre si cambió
                                    conn.prepareStatement("UPDATE core_users SET username = ?, last_login = CURRENT_TIMESTAMP WHERE id = ?").use { updateStmt ->
                                        updateStmt.setString(1, username)
                                        updateStmt.setInt(2, internalId)
                                        updateStmt.executeUpdate()
                                    }
                                } else {
                                    conn.prepareStatement("UPDATE core_users SET last_login = CURRENT_TIMESTAMP WHERE id = ?").use { updateStmt ->
                                        updateStmt.setInt(1, internalId)
                                        updateStmt.executeUpdate()
                                    }
                                }
                            }
                        }
                    }

                    // Si no existe, crearlo
                    if (internalId == -1) {
                        val insertSql = "INSERT INTO core_users (uuid, username) VALUES (?, ?)"
                        conn.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS).use { stmt ->
                            stmt.setString(1, uuid.toString())
                            stmt.setString(2, username)
                            stmt.executeUpdate()
                            stmt.generatedKeys.use { gk ->
                                if (gk.next()) {
                                    internalId = gk.getInt(1)
                                } else {
                                    throw java.sql.SQLException("No se pudo obtener el ID autogenerado para el nuevo usuario.")
                                }
                            }
                        }
                    }

                    // 2. Cargar saldos de economías
                    val economies = ConcurrentHashMap<String, java.math.BigDecimal>()
                    conn.prepareStatement("SELECT currency_id, balance FROM core_economies WHERE user_id = ?").use { stmt ->
                        stmt.setInt(1, internalId)
                        stmt.executeQuery().use { rs ->
                            while (rs.next()) {
                                economies[rs.getString("currency_id").lowercase()] = rs.getBigDecimal("balance")
                            }
                        }
                    }

                    // 3. Cargar cooldowns de kits
                    val kitCooldowns = ConcurrentHashMap<String, Long>()
                    conn.prepareStatement("SELECT kit_id, last_claimed FROM core_kits_cooldowns WHERE user_id = ?").use { stmt ->
                        stmt.setInt(1, internalId)
                        stmt.executeQuery().use { rs ->
                            while (rs.next()) {
                                kitCooldowns[rs.getString("kit_id").lowercase()] = rs.getLong("last_claimed")
                            }
                        }
                    }

                    // 4. Cargar estela de proyectil activa
                    var activeTrailId: String? = null
                    conn.prepareStatement("SELECT trail_id FROM core_player_projectile_trails WHERE user_id = ?").use { stmt ->
                        stmt.setInt(1, internalId)
                        stmt.executeQuery().use { rs ->
                            if (rs.next()) {
                                activeTrailId = rs.getString("trail_id")
                            }
                        }
                    }

                    conn.commit()

                    val profile = UserProfile(internalId, uuid, username, economies, kitCooldowns, activeTrailId, chatColor, socialSpy)
                    profiles[uuid] = profile
                    profile
                } catch (e: Exception) {
                    conn.rollback()
                    throw e
                } finally {
                    try {
                        conn.autoCommit = originalAutoCommit
                    } catch (ignored: Exception) {}
                }
            }
        }
    }

    fun acquireSyncLock(uuid: UUID) {
        val sql = "INSERT INTO player_sync_locks (uuid, locked_at) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET locked_at = excluded.locked_at"
        databaseService.execute(sql) { stmt ->
            stmt.setString(1, uuid.toString())
            stmt.setLong(2, System.currentTimeMillis())
        }
    }

    fun releaseSyncLock(uuid: UUID) {
        databaseService.execute("DELETE FROM player_sync_locks WHERE uuid = ?") { stmt ->
            stmt.setString(1, uuid.toString())
        }
    }

    fun isSyncLocked(uuid: UUID): Boolean {
        val lockedAt = databaseService.querySingle(
            "SELECT locked_at FROM player_sync_locks WHERE uuid = ?",
            preparer = { stmt -> stmt.setString(1, uuid.toString()) },
            mapper = { rs -> rs.getLong("locked_at") }
        ) ?: return false

        // Expirar lock si supera los 5 segundos de antigüedad
        if (System.currentTimeMillis() - lockedAt < 5000L) {
            return true
        } else {
            releaseSyncLock(uuid)
            return false
        }
    }

    /**
     * Guarda la información de un usuario y lo remueve del caché en memoria.
     * Adquiere un bloqueo temporal para sincronización cross-server.
     */
    fun unloadAndSave(uuid: UUID): CompletableFuture<Void> {
        val profile = profiles.remove(uuid) ?: return CompletableFuture.completedFuture(null)
        
        acquireSyncLock(uuid)
        
        val saveFuture = if (profile.isDirty) {
            flushProfiles(listOf(profile))
        } else {
            CompletableFuture.completedFuture(null)
        }
        
        return saveFuture.thenRun {
            releaseSyncLock(uuid)
        }
    }

    /**
     * Guarda en lote (Batch Execution) todos los perfiles marcados como sucios (dirty) sin removerlos de memoria.
     */
    fun flushAllActive(): CompletableFuture<Void> {
        val dirtyProfiles = profiles.values.filter { it.isDirty }
        if (dirtyProfiles.isEmpty()) {
            return CompletableFuture.completedFuture(null)
        }
        return flushProfiles(dirtyProfiles)
    }

    private fun flushProfiles(profilesToFlush: List<UserProfile>): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            // PostgreSQL y SQLite (3.24+) soportan ON CONFLICT DO UPDATE
            val userUpdateSql = "UPDATE core_users SET chat_color = ?, social_spy = ? WHERE id = ?"
            val economySql = "INSERT INTO core_economies (user_id, currency_id, balance) VALUES (?, ?, ?) ON CONFLICT(user_id, currency_id) DO UPDATE SET balance = excluded.balance"
            val cooldownSql = "INSERT INTO core_kits_cooldowns (user_id, kit_id, last_claimed) VALUES (?, ?, ?) ON CONFLICT(user_id, kit_id) DO UPDATE SET last_claimed = excluded.last_claimed"
            val trailUpsertSql = "INSERT INTO core_player_projectile_trails (user_id, trail_id) VALUES (?, ?) ON CONFLICT(user_id) DO UPDATE SET trail_id = excluded.trail_id"

            val trailDeleteSql = "DELETE FROM core_player_projectile_trails WHERE user_id = ?"

            databaseService.getConnection().use { conn ->
                val originalAutoCommit = conn.autoCommit
                conn.autoCommit = false
                try {
                    // 0. Guardar datos de usuario (chatColor, socialSpy)
                    conn.prepareStatement(userUpdateSql).use { stmt ->
                        for (profile in profilesToFlush) {
                            stmt.setString(1, profile.chatColor)
                            stmt.setInt(2, if (profile.socialSpy) 1 else 0)
                            stmt.setInt(3, profile.internalId)
                            stmt.addBatch()
                        }
                        stmt.executeBatch()
                    }

                    // 1. Guardar balances
                    conn.prepareStatement(economySql).use { stmt ->
                        for (profile in profilesToFlush) {
                            for ((currencyId, balance) in profile.economies) {
                                stmt.setInt(1, profile.internalId)
                                stmt.setString(2, currencyId)
                                stmt.setBigDecimal(3, balance)
                                stmt.addBatch()
                            }
                        }
                        stmt.executeBatch()
                    }

                    // 2. Guardar cooldowns
                    conn.prepareStatement(cooldownSql).use { stmt ->
                        for (profile in profilesToFlush) {
                            for ((kitId, lastClaimed) in profile.kitCooldowns) {
                                stmt.setInt(1, profile.internalId)
                                stmt.setString(2, kitId)
                                stmt.setLong(3, lastClaimed)
                                stmt.addBatch()
                            }
                        }
                        stmt.executeBatch()
                    }

                    // 3. Guardar estelas
                    val (toUpsert, toDelete) = profilesToFlush.partition { it.activeTrailId != null }

                    if (toUpsert.isNotEmpty()) {
                        conn.prepareStatement(trailUpsertSql).use { stmt ->
                            for (profile in toUpsert) {
                                stmt.setInt(1, profile.internalId)
                                stmt.setString(2, profile.activeTrailId!!)
                                stmt.addBatch()
                            }
                            stmt.executeBatch()
                        }
                    }

                    if (toDelete.isNotEmpty()) {
                        conn.prepareStatement(trailDeleteSql).use { stmt ->
                            for (profile in toDelete) {
                                stmt.setInt(1, profile.internalId)
                                stmt.addBatch()
                            }
                            stmt.executeBatch()
                        }
                    }

                    conn.commit()
                    profilesToFlush.forEach { it.clearDirty() }
                } catch (e: Exception) {
                    conn.rollback()
                    logger.severe("Error durante flush de perfiles: ${e.message}")
                    e.printStackTrace()
                    throw e
                } finally {
                    try {
                        conn.autoCommit = originalAutoCommit
                    } catch (ignored: Exception) {}
                }
            }
        }
    }
}
