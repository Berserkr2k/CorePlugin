package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import com.github.berserkr2k.coreplugin.infrastructure.database.DatabaseService
import com.github.berserkr2k.coreplugin.infrastructure.database.*
import com.github.berserkr2k.coreplugin.common.LegacyPlaceholderBridge
import com.github.berserkr2k.coreplugin.common.gui.ItemBuilder
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
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture
import com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry

data class ActivePodium(val uuid: UUID, val location: Location)

class LeaderboardService(
    private val plugin: Plugin,
    private val configManager: com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager,
    private val messagesConfig: MessagesConfig,
    private val databaseService: DatabaseService,
    private val placeholderBridge: LegacyPlaceholderBridge,
    private val profileRegistry: ProfileRegistry
) : Listener {
    private val leaderboardKey = NamespacedKey(plugin, "leaderboard_id")
    private val rankKey = NamespacedKey(plugin, "leaderboard_rank")
    private val miniMessage = MiniMessage.miniMessage()
    
    val leaderboards = ConcurrentHashMap<String, CustomLeaderboardConfig>()
    val activePodiums = ConcurrentHashMap<String, ActivePodium>()
    
    private val leaderboardsFolder = plugin.dataFolder.resolve("leaderboards")

    init {
        // Inicializar directorio e cargar clasificacións
        setupLeaderboardsFolder()
        loadAllLeaderboards()
        
        databaseService.initFuture.thenAccept {
            // Spawn de podios persistidos
            Bukkit.getAsyncScheduler().runNow(plugin) { _ ->
                spawnAllPersistedLeaderboards()
            }

            // Tarea de actualización y refresco de podios (cada 60s)
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _ ->
                for (player in Bukkit.getOnlinePlayers()) {
                    updatePlayerStats(player)
                }
                refreshAllLeaderboards()
            }, 5, 60, java.util.concurrent.TimeUnit.SECONDS)
        }

        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    private fun setupLeaderboardsFolder() {
        if (!leaderboardsFolder.exists()) {
            leaderboardsFolder.mkdirs()
        }

        // Crear clasificaciones por defecto si está vacía
        val defaultCreditsFile = leaderboardsFolder.resolve("credits.conf")
        if (!defaultCreditsFile.exists()) {
            configManager.loadModuleConfig("leaderboards/credits.conf", CustomLeaderboardConfig::class.java, CustomLeaderboardConfig(
                id = "credits",
                placeholder = "%coreplugin_balance_credits%",
                displayName = "<gold><bold>TOP CRÉDITOS</bold></gold>"
            )).join()
        }

        val defaultKillsFile = leaderboardsFolder.resolve("kills.conf")
        if (!defaultKillsFile.exists()) {
            configManager.loadModuleConfig("leaderboards/kills.conf", CustomLeaderboardConfig::class.java, CustomLeaderboardConfig(
                id = "kills",
                placeholder = "%statistic_player_kills%",
                displayName = "<red><bold>TOP KILLS</bold></gold>"
            )).join()
        }
    }

    fun loadAllLeaderboards() {
        leaderboards.clear()
        val files = leaderboardsFolder.listFiles { _, name -> name.endsWith(".conf") } ?: emptyArray()

        for (file in files) {
            val id = file.nameWithoutExtension.lowercase()
            try {
                val config = configManager.loadModuleConfig("leaderboards/${file.name}", CustomLeaderboardConfig::class.java, CustomLeaderboardConfig(id = id)).join()
                leaderboards[id] = config
            } catch (e: Exception) {
                plugin.logger.severe("Error al cargar la clasificación desde ${file.name}: ${e.message}")
            }
        }
    }



    fun spawnAllPersistedLeaderboards() {
        leaderboards.forEach { (id, config) ->
            config.podiums.forEach { (rankStr, persisted) ->
                val rank = rankStr.toIntOrNull() ?: 1
                val world = Bukkit.getWorld(persisted.world) ?: return@forEach
                val loc = Location(world, persisted.x, persisted.y, persisted.z, persisted.yaw, persisted.pitch)
                spawnOrFindLeaderboard(loc, id, rank)
            }
        }
    }

    fun spawnOrFindLeaderboard(location: Location, leaderboardId: String, rank: Int) {
        val key = "${leaderboardId}_$rank"
        
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

            // Migrar / Limpiar antiguos pasajeros displays si existen
            stand.passengers.forEach { passenger ->
                passenger.remove()
            }

            val displayTypeKey = NamespacedKey(plugin, "leaderboard_display_type")

            // Buscar TODOS los displays flotantes del leaderboard dentro de la caja del podio
            val nearbyDisplays = location.world.getNearbyEntities(location, 0.8, 3.5, 0.8) { entity ->
                entity is TextDisplay &&
                entity.persistentDataContainer.get(leaderboardKey, PersistentDataType.STRING) == leaderboardId
            }.filterIsInstance<TextDisplay>()

            // Eliminar displays con rank incorrecto (auto-limpieza de podios antiguos / cambiados)
            nearbyDisplays.filter { 
                (it.persistentDataContainer.get(rankKey, PersistentDataType.INTEGER) ?: 1) != rank 
            }.forEach { it.remove() }

            val correctDisplays = nearbyDisplays.filter { 
                (it.persistentDataContainer.get(rankKey, PersistentDataType.INTEGER) ?: 1) == rank 
            }

            val entryDisplays = correctDisplays.filter { 
                it.persistentDataContainer.get(displayTypeKey, PersistentDataType.STRING) == "entry" 
            }
            val headerDisplays = correctDisplays.filter { 
                it.persistentDataContainer.get(displayTypeKey, PersistentDataType.STRING) == "header" 
            }

            // 1. Holograma del Registro (Puesto y Puntos) - Siempre presente
            val entryDisplay = if (entryDisplays.isNotEmpty()) {
                val kept = entryDisplays.first()
                entryDisplays.drop(1).forEach { it.remove() }
                kept
            } else {
                (location.world.spawnEntity(location.clone().add(0.0, 2.1, 0.0), EntityType.TEXT_DISPLAY) as TextDisplay).also {
                    it.setGravity(false)
                    it.billboard = Display.Billboard.CENTER
                    it.persistentDataContainer.set(leaderboardKey, PersistentDataType.STRING, leaderboardId)
                    it.persistentDataContainer.set(rankKey, PersistentDataType.INTEGER, rank)
                    it.persistentDataContainer.set(displayTypeKey, PersistentDataType.STRING, "entry")
                }
            }
            entryDisplay.text(miniMessage.deserialize(messagesConfig.leaderboards["loading"] ?: "<gold>Cargando...</gold>"))

            // 2. Holograma del Encabezado - Solo para Ranks <= headerAboveRank (e.g. Rank 1)
            val config = leaderboards[leaderboardId.lowercase()] ?: CustomLeaderboardConfig(id = leaderboardId)
            val headerAboveRank = config.headerAboveRank

            if (rank <= headerAboveRank) {
                val headerDisplay = if (headerDisplays.isNotEmpty()) {
                    val kept = headerDisplays.first()
                    headerDisplays.drop(1).forEach { it.remove() }
                    kept
                } else {
                    (location.world.spawnEntity(location.clone().add(0.0, 2.45, 0.0), EntityType.TEXT_DISPLAY) as TextDisplay).also {
                        it.setGravity(false)
                        it.billboard = Display.Billboard.CENTER
                        it.persistentDataContainer.set(leaderboardKey, PersistentDataType.STRING, leaderboardId)
                        it.persistentDataContainer.set(rankKey, PersistentDataType.INTEGER, rank)
                        it.persistentDataContainer.set(displayTypeKey, PersistentDataType.STRING, "header")
                    }
                }
                headerDisplay.text(miniMessage.deserialize(messagesConfig.leaderboards["loading"] ?: "<gold>Cargando...</gold>"))
            } else {
                headerDisplays.forEach { it.remove() }
            }

            activePodiums[key] = ActivePodium(stand.uniqueId, location.clone())
        }
    }

    fun registerLeaderboard(id: String, rank: Int, loc: Location): CompletableFuture<Void> {
        val leaderboardId = id.lowercase()
        return CompletableFuture.runAsync({
            val config = leaderboards[leaderboardId] ?: CustomLeaderboardConfig(id = leaderboardId)
            val updatedPodiums = config.podiums.toMutableMap()
            updatedPodiums[rank.toString()] = PersistedPodium(
                world = loc.world.name,
                x = loc.x,
                y = loc.y,
                z = loc.z,
                yaw = loc.yaw,
                pitch = loc.pitch
            )
            val updatedConfig = config.copy(podiums = updatedPodiums)
            leaderboards[leaderboardId] = updatedConfig

            try {
                configManager.saveModuleConfig("leaderboards/$leaderboardId.conf", CustomLeaderboardConfig::class.java, updatedConfig).join()
            } catch (e: Exception) {
                plugin.logger.severe("Fallo al guardar clasificación $leaderboardId: ${e.message}")
            }

            spawnOrFindLeaderboard(loc, leaderboardId, rank)
        }, { command -> Bukkit.getAsyncScheduler().runNow(plugin) { _ -> command.run() } })
    }

    fun unregisterLeaderboard(id: String, rank: Int): CompletableFuture<Boolean> {
        val leaderboardId = id.lowercase()
        val key = "${leaderboardId}_$rank"
        val activePodium = activePodiums.remove(key)

        return CompletableFuture.supplyAsync({
            val config = leaderboards[leaderboardId] ?: return@supplyAsync false
            val updatedPodiums = config.podiums.toMutableMap()
            if (updatedPodiums.remove(rank.toString()) == null) {
                return@supplyAsync false
            }
            
            val updatedConfig = config.copy(podiums = updatedPodiums)
            leaderboards[leaderboardId] = updatedConfig

            try {
                configManager.saveModuleConfig("leaderboards/$leaderboardId.conf", CustomLeaderboardConfig::class.java, updatedConfig).join()
            } catch (e: Exception) {
                plugin.logger.severe("Fallo al guardar clasificación $leaderboardId tras remover podio: ${e.message}")
            }

            if (activePodium != null) {
                Bukkit.getRegionScheduler().execute(plugin, activePodium.location) {
                    val stand = Bukkit.getEntity(activePodium.uuid) as? ArmorStand
                    if (stand != null) {
                        stand.passengers.forEach { it.remove() }
                        stand.remove()
                    }
                    
                    // Buscar y eliminar los displays flotantes a su alrededor
                    activePodium.location.world.getNearbyEntities(activePodium.location, 2.0, 3.0, 2.0) { entity ->
                        entity is TextDisplay &&
                        entity.persistentDataContainer.get(leaderboardKey, PersistentDataType.STRING) == leaderboardId &&
                        (entity.persistentDataContainer.get(rankKey, PersistentDataType.INTEGER) ?: 1) == rank
                    }.forEach { it.remove() }
                }
            }
            true
        }, { command -> Bukkit.getAsyncScheduler().runNow(plugin) { _ -> command.run() } })
    }

    fun updatePlayerStats(player: Player) {
        leaderboards.forEach { (id, config) ->
            if (config.placeholder.isNotEmpty()) {
                val raw = placeholderBridge.parsePlaceholder(player, config.placeholder)
                val score = raw.replace(",", "").toDoubleOrNull() ?: 0.0
                saveScore(player.uniqueId, player.name, id, score)
            }
        }
    }

    private fun saveScore(uuid: UUID, username: String, leaderboardId: String, score: Double) {
        val profile = profileRegistry.getProfile(uuid)
        val userIdFuture = if (profile != null) {
            CompletableFuture.completedFuture(profile.internalId)
        } else {
            databaseService.querySingleAsync(
                "SELECT id FROM core_users WHERE uuid = ?",
                preparer = { stmt -> stmt.setString(1, uuid.toString()) },
                mapper = { rs -> rs.getInt("id") }
            ).thenCompose { id ->
                if (id != null) {
                    CompletableFuture.completedFuture(id)
                } else {
                    CompletableFuture.supplyAsync {
                        databaseService.transaction { conn ->
                            val insertSql = "INSERT INTO core_users (uuid, username) VALUES (?, ?)"
                            conn.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS).use { stmt ->
                                stmt.setString(1, uuid.toString())
                                stmt.setString(2, username)
                                stmt.executeUpdate()
                                stmt.generatedKeys.use { gk ->
                                    if (gk.next()) gk.getInt(1) else throw java.sql.SQLException("No key")
                                }
                            }
                        }
                    }
                }
            }
        }

        userIdFuture.thenAccept { userId ->
            val isMySQL = databaseService.config.driver.equals("mysql", ignoreCase = true)
            val upsertSql = if (isMySQL) {
                "INSERT INTO player_scores (user_id, leaderboard_id, score) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE score = VALUES(score)"
            } else {
                "INSERT INTO player_scores (user_id, leaderboard_id, score) VALUES (?, ?, ?) ON CONFLICT(user_id, leaderboard_id) DO UPDATE SET score = excluded.score"
            }
            databaseService.executeAsync(upsertSql) { stmt ->
                stmt.setInt(1, userId)
                stmt.setString(2, leaderboardId)
                stmt.setDouble(3, score)
            }
        }.exceptionally { e ->
            plugin.logger.severe("Error al guardar puntuación en la base de datos: ${e.message}")
            null
        }
    }

    fun getTop10(leaderboardId: String): CompletableFuture<List<Map.Entry<String, Double>>> {
        val sql = """
            SELECT u.username, s.score FROM player_scores s
            JOIN core_users u ON s.user_id = u.id
            WHERE s.leaderboard_id = ? 
            ORDER BY s.score DESC 
            LIMIT 10
        """.trimIndent()
        return databaseService.queryAsync(
            sql,
            preparer = { stmt -> stmt.setString(1, leaderboardId) },
            mapper = { rs ->
                val username = rs.getString("username")
                val score = rs.getDouble("score")
                java.util.AbstractMap.SimpleEntry(username, score) as Map.Entry<String, Double>
            }
        ).exceptionally { e ->
            plugin.logger.severe("Error al consultar el top 10 en la base de datos: ${e.message}")
            emptyList()
        }
    }

    fun refreshAllLeaderboards() {
        leaderboards.keys.forEach { id ->
            getTop10(id).thenAccept { topData ->
                refreshLeaderboard(id, topData)
            }
        }
    }

    fun refreshLeaderboard(leaderboardId: String, topData: List<Map.Entry<String, Double>>): CompletableFuture<Void> {
        val futures = mutableListOf<CompletableFuture<Void>>()
        val config = leaderboards[leaderboardId.lowercase()] ?: CustomLeaderboardConfig(id = leaderboardId)
        val headerAboveRank = config.headerAboveRank
        
        activePodiums.forEach { (key, activePodium) ->
            if (!key.startsWith("${leaderboardId}_")) return@forEach
            val rankString = key.substringAfter("${leaderboardId}_")
            val rank = rankString.toIntOrNull() ?: return@forEach

            val entry = if (rank <= topData.size) topData[rank - 1] else null
            val playerName = entry?.key

            val future = CompletableFuture.runAsync({
                val profile = if (playerName != null) {
                    try {
                        val p = Bukkit.createProfile(playerName)
                        p.complete()
                        p
                    } catch (e: Exception) {
                        null
                    }
                } else null

                // Actualizar la entidad con seguridad asíncrona Folia-ready en el hilo de su región
                Bukkit.getRegionScheduler().execute(plugin, activePodium.location) {
                    val stand = Bukkit.getEntity(activePodium.uuid) as? ArmorStand ?: return@execute
                    
                    // Actualizar cabeza usando ItemBuilder
                    if (profile != null) {
                        val skull = ItemBuilder(Material.PLAYER_HEAD)
                            .skullProfile(profile)
                            .build()
                        stand.setItem(EquipmentSlot.HEAD, skull)
                    } else {
                        stand.setItem(EquipmentSlot.HEAD, ItemStack(Material.AIR))
                    }

                    // Limpiar antiguos pasajeros displays si existen
                    stand.passengers.forEach { it.remove() }

                    val displayTypeKey = NamespacedKey(plugin, "leaderboard_display_type")

                    // Buscar TODOS los displays flotantes del leaderboard dentro de la caja del podio
                    val nearbyDisplays = stand.location.world.getNearbyEntities(stand.location, 0.8, 3.5, 0.8) { entity ->
                        entity is TextDisplay &&
                        entity.persistentDataContainer.get(leaderboardKey, PersistentDataType.STRING) == leaderboardId
                    }.filterIsInstance<TextDisplay>()

                    // Eliminar displays con rank incorrecto
                    nearbyDisplays.filter { 
                        (it.persistentDataContainer.get(rankKey, PersistentDataType.INTEGER) ?: 1) != rank 
                    }.forEach { it.remove() }

                    val correctDisplays = nearbyDisplays.filter { 
                        (it.persistentDataContainer.get(rankKey, PersistentDataType.INTEGER) ?: 1) == rank 
                    }

                    val entryDisplays = correctDisplays.filter { 
                        it.persistentDataContainer.get(displayTypeKey, PersistentDataType.STRING) == "entry" 
                    }
                    val headerDisplays = correctDisplays.filter { 
                        it.persistentDataContainer.get(displayTypeKey, PersistentDataType.STRING) == "header" 
                    }

                    // 1. Actualizar holograma del Registro (Puesto y Puntos)
                    val entryDisplay = if (entryDisplays.isNotEmpty()) {
                        val kept = entryDisplays.first()
                        entryDisplays.drop(1).forEach { it.remove() }
                        kept
                    } else {
                        (stand.world.spawnEntity(stand.location.clone().add(0.0, 2.1, 0.0), EntityType.TEXT_DISPLAY) as TextDisplay).also {
                            it.setGravity(false)
                            it.billboard = Display.Billboard.CENTER
                            it.persistentDataContainer.set(leaderboardKey, PersistentDataType.STRING, leaderboardId)
                            it.persistentDataContainer.set(rankKey, PersistentDataType.INTEGER, rank)
                            it.persistentDataContainer.set(displayTypeKey, PersistentDataType.STRING, "entry")
                        }
                    }
                    
                    val text = if (entry != null) {
                        val format = config.formats[rank.toString()] ?: config.formats["default"] ?: "<yellow>#<pos></yellow> <gray><player></gray> » <green>$<balance></green>"
                        format.replace("<pos>", rank.toString())
                            .replace("<player>", entry.key)
                            .replace("<balance>", String.format("%.2f", entry.value))
                    } else {
                        (messagesConfig.leaderboards["vacant"] ?: "<gray>#<pos> - Vacante</gray>")
                            .replace("<pos>", rank.toString())
                    }
                    entryDisplay.text(miniMessage.deserialize(text))

                    // 2. Actualizar holograma del Encabezado (solo si rank <= headerAboveRank)
                    val showHeader = rank <= headerAboveRank
                    if (showHeader) {
                        val headerDisplay = if (headerDisplays.isNotEmpty()) {
                            val kept = headerDisplays.first()
                            headerDisplays.drop(1).forEach { it.remove() }
                            kept
                        } else {
                            (stand.world.spawnEntity(stand.location.clone().add(0.0, 2.45, 0.0), EntityType.TEXT_DISPLAY) as TextDisplay).also {
                                it.setGravity(false)
                                it.billboard = Display.Billboard.CENTER
                                it.persistentDataContainer.set(leaderboardKey, PersistentDataType.STRING, leaderboardId)
                                it.persistentDataContainer.set(rankKey, PersistentDataType.INTEGER, rank)
                                it.persistentDataContainer.set(displayTypeKey, PersistentDataType.STRING, "header")
                            }
                        }
                        val headerText = config.header.replace("%top_id%", config.displayName)
                        headerDisplay.text(miniMessage.deserialize(headerText))
                    } else {
                        // Remover el holograma de cabecera si existe pero no corresponde
                        headerDisplays.forEach { it.remove() }
                    }
                }
            }, { command -> Bukkit.getAsyncScheduler().runNow(plugin) { _ -> command.run() } })
            
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

    fun reloadLeaderboards(): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            loadAllLeaderboards()
            
            val configuredKeys = mutableSetOf<String>()
            leaderboards.forEach { (id, config) ->
                config.podiums.forEach { (rankStr, _) ->
                    val rank = rankStr.toIntOrNull() ?: 1
                    configuredKeys.add("${id}_$rank")
                }
            }

            val futures = mutableListOf<CompletableFuture<Void>>()

            // 1. Eliminar podios activos en memoria que ya no estén presentes en las configuraciones
            activePodiums.forEach { (key, active) ->
                if (!configuredKeys.contains(key)) {
                    val id = key.substringBefore("_")
                    val rank = key.substringAfter("_").toIntOrNull() ?: 1
                    activePodiums.remove(key)
                    
                    val future = CompletableFuture.runAsync({
                        val p = CompletableFuture<Void>()
                        Bukkit.getRegionScheduler().execute(plugin, active.location) {
                            val stand = Bukkit.getEntity(active.uuid) as? ArmorStand
                            if (stand != null) {
                                stand.passengers.forEach { it.remove() }
                                stand.remove()
                            }
                            
                            // Eliminar displays flotantes en su área
                            active.location.world.getNearbyEntities(active.location, 0.8, 3.5, 0.8) { entity ->
                                entity is TextDisplay &&
                                entity.persistentDataContainer.get(leaderboardKey, PersistentDataType.STRING) == id &&
                                (entity.persistentDataContainer.get(rankKey, PersistentDataType.INTEGER) ?: 1) == rank
                            }.forEach { it.remove() }
                            p.complete(null)
                        }
                        p.join()
                    }, { command -> Bukkit.getAsyncScheduler().runNow(plugin) { _ -> command.run() } })
                    futures.add(future)
                }
            }

            // 2. Procesar podios configurados (agregar nuevos, mover existentes, refrescar inalterados)
            leaderboards.forEach { (id, config) ->
                config.podiums.forEach { (rankStr, persisted) ->
                    val rank = rankStr.toIntOrNull() ?: 1
                    val world = Bukkit.getWorld(persisted.world) ?: return@forEach
                    val newLoc = Location(world, persisted.x, persisted.y, persisted.z, persisted.yaw, persisted.pitch)
                    
                    val key = "${id}_$rank"
                    val active = activePodiums[key]
                    
                    val future = CompletableFuture.runAsync({
                        if (active != null) {
                            // Si ya existe activo, comprobamos si cambió de localización/mundo
                            val oldLoc = active.location
                            val locationChanged = oldLoc.world.name != newLoc.world.name || oldLoc.distanceSquared(newLoc) > 0.01
                            
                            if (locationChanged) {
                                val p1 = CompletableFuture<Void>()
                                // Regional en la vieja localización para teleportar stand y limpiar displays viejos
                                Bukkit.getRegionScheduler().execute(plugin, oldLoc) {
                                    val stand = Bukkit.getEntity(active.uuid) as? ArmorStand
                                    if (stand != null) {
                                        stand.teleport(newLoc)
                                    }
                                    
                                    // Limpiar displays en el viejo sitio
                                    oldLoc.world.getNearbyEntities(oldLoc, 0.8, 3.5, 0.8) { entity ->
                                        entity is TextDisplay &&
                                        entity.persistentDataContainer.get(leaderboardKey, PersistentDataType.STRING) == id &&
                                        (entity.persistentDataContainer.get(rankKey, PersistentDataType.INTEGER) ?: 1) == rank
                                    }.forEach { it.remove() }
                                    p1.complete(null)
                                }
                                
                                p1.thenRun {
                                    // Regional en la NUEVA localización para configurar displays y registrar en memoria
                                    Bukkit.getRegionScheduler().execute(plugin, newLoc) {
                                        spawnOrFindLeaderboard(newLoc, id, rank)
                                    }
                                }.join()
                            } else {
                                // No cambió localización, refrescar displays y configuración regionalmente
                                val p2 = CompletableFuture<Void>()
                                Bukkit.getRegionScheduler().execute(plugin, newLoc) {
                                    spawnOrFindLeaderboard(newLoc, id, rank)
                                    p2.complete(null)
                                }
                                p2.join()
                            }
                        } else {
                            // Nuevo podio añadido a mano, spawnear directamente regionalmente
                            val p3 = CompletableFuture<Void>()
                            Bukkit.getRegionScheduler().execute(plugin, newLoc) {
                                spawnOrFindLeaderboard(newLoc, id, rank)
                                p3.complete(null)
                            }
                            p3.join()
                        }
                    }, { command -> Bukkit.getAsyncScheduler().runNow(plugin) { _ -> command.run() } })
                    
                    futures.add(future)
                }
            }

            // 3. Esperar a que se procesen todas las tareas regionales y refrescar stats
            CompletableFuture.allOf(*futures.toTypedArray()).thenRun {
                refreshAllLeaderboards()
            }.join()
        }, { command -> Bukkit.getAsyncScheduler().runNow(plugin) { _ -> command.run() } })
    }

    fun shutdown() {
        activePodiums.clear()
    }
}
