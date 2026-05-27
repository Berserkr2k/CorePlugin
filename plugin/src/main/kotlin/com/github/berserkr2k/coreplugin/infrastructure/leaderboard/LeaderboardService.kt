package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import com.github.berserkr2k.coreplugin.infrastructure.database.DatabaseService
import com.github.berserkr2k.coreplugin.common.LegacyPlaceholderBridge
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.TextDisplay
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin
import org.bukkit.util.EulerAngle
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.event.Listener
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture

class LeaderboardService(
    private val plugin: Plugin,
    private val configManager: com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager,
    private val messagesConfig: MessagesConfig,
    private val databaseService: DatabaseService,
    private val placeholderBridge: LegacyPlaceholderBridge
) : Listener {
    private val leaderboardKey = NamespacedKey(plugin, "leaderboard_id")
    private val rankKey = NamespacedKey(plugin, "leaderboard_rank")
    private val miniMessage = MiniMessage.miniMessage()
    
    val activeArmorStands = ConcurrentHashMap<String, UUID>()
    var leaderboardConfig = LeaderboardConfig()
        private set

    init {
        // Carga la configuración modular asíncrona de podios
        configManager.loadModuleConfig("leaderboards.conf", LeaderboardConfig::class.java, LeaderboardConfig())
            .thenAccept { config ->
                this.leaderboardConfig = config
                
                // Esperar a que la base de datos se inicialice completamente
                databaseService.initFuture.thenAccept {
                    // Configura e inicializa la tabla SQL de puntuaciones
                    setupDatabaseTable()

                    // Spawn/Find de todos los podios registrados
                    Bukkit.getAsyncScheduler().runNow(plugin) { _ ->
                        spawnAllPersistedLeaderboards()
                    }

                    // Inicia la tarea repetitiva de actualización y refresco de podios (cada 60 segundos)
                    Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _ ->
                        for (player in Bukkit.getOnlinePlayers()) {
                            updatePlayerStats(player)
                        }
                        refreshAllLeaderboards()
                    }, 5, 60, java.util.concurrent.TimeUnit.SECONDS)
                }
            }
    }

    /**
     * Crea la tabla de caché de puntuaciones en base de datos si no existe.
     */
    private fun setupDatabaseTable() {
        try {
            databaseService.getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.execute("CREATE TABLE IF NOT EXISTS player_scores (uuid VARCHAR(36), username VARCHAR(16), leaderboard_id VARCHAR(32), score DOUBLE, PRIMARY KEY (uuid, leaderboard_id))")
                }
            }
        } catch (e: Exception) {
            plugin.logger.severe("Fallo al inicializar la tabla player_scores: ${e.message}")
        }
    }

    /**
     * Escanea y spawnea todos los podios configurados de forma Folia-safe.
     */
    fun spawnAllPersistedLeaderboards() {
        leaderboardConfig.positions.forEach { (key, persisted) ->
            val world = Bukkit.getWorld(persisted.world) ?: return@forEach
            val loc = Location(world, persisted.x, persisted.y, persisted.z, persisted.yaw, persisted.pitch)
            spawnOrFindLeaderboard(loc, persisted.leaderboardId, persisted.rank)
        }
    }

    /**
     * Hace aparecer o localiza un ArmorStand de clasificación en una coordenada específica.
     * Escanea espacialmente para evitar duplicados en reinicios del servidor.
     */
    fun spawnOrFindLeaderboard(location: Location, leaderboardId: String, rank: Int) {
        Bukkit.getRegionScheduler().execute(plugin, location) {
            val existingStand = location.world.getNearbyEntities(location, 1.5, 1.5, 1.5) { entity ->
                entity is ArmorStand &&
                entity.persistentDataContainer.get(leaderboardKey, PersistentDataType.STRING) == leaderboardId &&
                (entity.persistentDataContainer.get(rankKey, PersistentDataType.INTEGER) ?: 1) == rank
            }.firstOrNull() as? ArmorStand

            val stand = if (existingStand != null) {
                plugin.logger.info("Encontrado podio ArmorStand existente para '$leaderboardId' (Rank $rank)")
                existingStand
            } else {
                plugin.logger.info("Spawneando nuevo podio ArmorStand para '$leaderboardId' (Rank $rank)")
                val s = location.world.spawnEntity(location, EntityType.ARMOR_STAND) as ArmorStand
                s.setArms(true)
                s.setBasePlate(true)
                s.setGravity(false)
                s.isCustomNameVisible = false
                s.persistentDataContainer.set(leaderboardKey, PersistentDataType.STRING, leaderboardId)
                s.persistentDataContainer.set(rankKey, PersistentDataType.INTEGER, rank)

                s.rightArmPose = EulerAngle(Math.toRadians(-15.0), 0.0, Math.toRadians(10.0))
                s.leftArmPose = EulerAngle(Math.toRadians(-15.0), 0.0, Math.toRadians(-10.0))
                s
            }

            val display = stand.passengers.filterIsInstance<TextDisplay>().firstOrNull()
                ?: (location.world.spawnEntity(location.clone().add(0.0, 2.1, 0.0), EntityType.TEXT_DISPLAY) as TextDisplay).also {
                    it.setGravity(false)
                    it.billboard = Display.Billboard.CENTER
                    stand.addPassenger(it)
                }
            display.text(miniMessage.deserialize(messagesConfig.leaderboards["loading"] ?: "<gold>Cargando...</gold>"))

            activeArmorStands["${leaderboardId}_$rank"] = stand.uniqueId
        }
    }

    /**
     * Registra un nuevo podio persistente en la configuración de leaderboards.
     */
    fun registerLeaderboard(id: String, rank: Int, loc: Location): CompletableFuture<Void> {
        val key = "${id}_$rank"
        val newPositions = leaderboardConfig.positions.toMutableMap()
        newPositions[key] = LeaderboardConfig.PersistedLeaderboard(
            leaderboardId = id,
            rank = rank,
            world = loc.world.name,
            x = loc.x,
            y = loc.y,
            z = loc.z,
            yaw = loc.yaw,
            pitch = loc.pitch
        )
        leaderboardConfig = LeaderboardConfig(newPositions, leaderboardConfig.settings)
        return configManager.saveModuleConfig("leaderboards.conf", LeaderboardConfig::class.java, leaderboardConfig)
    }

    /**
     * Resuelve las puntuaciones del jugador y las persiste en la base de datos asíncronamente.
     */
    fun updatePlayerStats(player: Player) {
        leaderboardConfig.settings.forEach { (id, setting) ->
            if (setting.placeholder.isNotEmpty()) {
                val raw = placeholderBridge.parsePlaceholder(player, setting.placeholder)
                val score = raw.replace(",", "").toDoubleOrNull() ?: 0.0
                saveScore(player.uniqueId, player.name, id, score)
            }
        }
    }

    private fun saveScore(uuid: UUID, username: String, leaderboardId: String, score: Double) {
        Bukkit.getAsyncScheduler().runNow(plugin) { _ ->
            try {
                databaseService.getConnection().use { conn ->
                    val selectSql = "SELECT score FROM player_scores WHERE uuid = ? AND leaderboard_id = ?"
                    var exists = false
                    conn.prepareStatement(selectSql).use { selectStmt ->
                        selectStmt.setString(1, uuid.toString())
                        selectStmt.setString(2, leaderboardId)
                        selectStmt.executeQuery().use { rs ->
                            if (rs.next()) exists = true
                        }
                    }
                    if (exists) {
                        val updateSql = "UPDATE player_scores SET score = ?, username = ? WHERE uuid = ? AND leaderboard_id = ?"
                        conn.prepareStatement(updateSql).use { updateStmt ->
                            updateStmt.setDouble(1, score)
                            updateStmt.setString(2, username)
                            updateStmt.setString(3, uuid.toString())
                            updateStmt.setString(4, leaderboardId)
                            updateStmt.executeUpdate()
                        }
                    } else {
                        val insertSql = "INSERT INTO player_scores (uuid, username, leaderboard_id, score) VALUES (?, ?, ?, ?)"
                        conn.prepareStatement(insertSql).use { insertStmt ->
                            insertStmt.setString(1, uuid.toString())
                            insertStmt.setString(2, username)
                            insertStmt.setString(3, leaderboardId)
                            insertStmt.setDouble(4, score)
                            insertStmt.executeUpdate()
                        }
                    }
                }
            } catch (e: Exception) {
                plugin.logger.severe("Error al guardar puntuación en la base de datos: ${e.message}")
            }
        }
    }

    /**
     * Consulta asíncronamente el top 10 para una clasificación.
     */
    fun getTop10(leaderboardId: String): CompletableFuture<List<Map.Entry<String, Double>>> {
        val future = CompletableFuture<List<Map.Entry<String, Double>>>()
        Bukkit.getAsyncScheduler().runNow(plugin) { _ ->
            val list = mutableListOf<Map.Entry<String, Double>>()
            try {
                databaseService.getConnection().use { conn ->
                    val sql = "SELECT username, score FROM player_scores WHERE leaderboard_id = ? ORDER BY score DESC LIMIT 10"
                    conn.prepareStatement(sql).use { stmt ->
                        stmt.setString(1, leaderboardId)
                        stmt.executeQuery().use { rs ->
                            while (rs.next()) {
                                val username = rs.getString("username")
                                val score = rs.getDouble("score")
                                list.add(java.util.AbstractMap.SimpleEntry(username, score))
                            }
                        }
                    }
                }
                future.complete(list)
            } catch (e: Exception) {
                plugin.logger.severe("Error al consultar el top 10 en la base de datos: ${e.message}")
                future.complete(emptyList())
            }
        }
        return future
    }

    /**
     * Refresca todos los podios configurados.
     */
    fun refreshAllLeaderboards() {
        leaderboardConfig.settings.keys.forEach { id ->
            getTop10(id).thenAccept { topData ->
                refreshLeaderboard(id, topData)
            }
        }
    }

    /**
     * Actualiza físicamente los stands de un ID basándose en el top de base de datos.
     */
    fun refreshLeaderboard(leaderboardId: String, topData: List<Map.Entry<String, Double>>): CompletableFuture<Void> {
        val futures = mutableListOf<CompletableFuture<Void>>()
        
        activeArmorStands.forEach { (key, uuid) ->
            if (!key.startsWith("${leaderboardId}_")) return@forEach
            val rankString = key.substringAfter("${leaderboardId}_")
            val rank = rankString.toIntOrNull() ?: return@forEach

            val stand = Bukkit.getEntity(uuid) as? ArmorStand ?: return@forEach
            
            val future = CompletableFuture.runAsync({
                val entry = if (rank <= topData.size) topData[rank - 1] else null
                val playerName = entry?.key

                val profile = if (playerName != null) {
                    try {
                        val p = Bukkit.createProfile(playerName)
                        p.complete()
                        p
                    } catch (e: Exception) {
                        null
                    }
                } else null

                Bukkit.getRegionScheduler().execute(plugin, stand.location) {
                    // Actualizar cabeza
                    if (profile != null) {
                        val skull = ItemStack(Material.PLAYER_HEAD)
                        val skullMeta = skull.itemMeta as SkullMeta
                        skullMeta.playerProfile = profile
                        skull.itemMeta = skullMeta
                        stand.setItem(EquipmentSlot.HEAD, skull)
                    } else {
                        stand.setItem(EquipmentSlot.HEAD, ItemStack(Material.AIR))
                    }

                    // Actualizar holograma pasajero
                    val display = stand.passengers.filterIsInstance<TextDisplay>().firstOrNull()
                    if (display != null) {
                        val text = if (entry != null) {
                            val header = (messagesConfig.leaderboards["header"] ?: "<gold><bold>TOP <rank_id></bold></gold>")
                                .replace("<rank_id>", rank.toString())
                                .replace("<top_id>", leaderboardId.uppercase())
                            val row = (messagesConfig.leaderboards["row-format"] ?: "#<pos> <player> - <balance>")
                                .replace("<pos>", rank.toString())
                                .replace("<player>", entry.key)
                                .replace("<balance>", String.format("%.2f", entry.value))
                            "$header\n$row"
                        } else {
                            val header = (messagesConfig.leaderboards["header"] ?: "<gold><bold>TOP <rank_id></bold></gold>")
                                .replace("<rank_id>", rank.toString())
                                .replace("<top_id>", leaderboardId.uppercase())
                            "$header\n<gray>Vacante</gray>"
                        }
                        display.text(miniMessage.deserialize(text))
                    }
                }
            })
            futures.add(future)
        }
        return CompletableFuture.allOf(*futures.toTypedArray())
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        updatePlayerStats(event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        updatePlayerStats(event.player)
    }

    fun shutdown() {
        activeArmorStands.clear()
    }
}
