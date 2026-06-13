package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import com.github.berserkr2k.coreplugin.api.core.config.ConfigService
import com.github.berserkr2k.coreplugin.api.core.database.DatabaseService
import com.github.berserkr2k.coreplugin.api.core.placeholder.PlaceholderService
import com.github.berserkr2k.coreplugin.api.core.user.ProfileRegistry
import com.github.berserkr2k.coreplugin.api.core.user.UserProfile
import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilder
import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilderFactory
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
// Imported from api.core.user package
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.filesystem.FeatureFolderProvider
import com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler
import com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler
import com.github.berserkr2k.coreplugin.api.core.scheduler.Task
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.infrastructure.leaderboard.LeaderboardMessages

data class ActivePodium(val uuid: UUID, val location: Location)

class LeaderboardService(
    val plugin: Plugin,
    private val featureConfig: com.github.berserkr2k.coreplugin.api.core.config.FeatureConfig,
    private val taskScheduler: TaskScheduler,
    private val databaseService: DatabaseService
) : Listener, com.github.berserkr2k.coreplugin.api.feature.leaderboard.LeaderboardService, com.github.berserkr2k.coreplugin.api.core.lifecycle.Reloadable {
    private val leaderboardKey = NamespacedKey(plugin, "leaderboard_id")
    private val rankKey = NamespacedKey(plugin, "leaderboard_rank")
    private val miniMessage = MiniMessage.miniMessage()
    
    private val registry = org.bukkit.Bukkit.getServicesManager().load(com.github.berserkr2k.coreplugin.api.di.ServiceRegistry::class.java)
        ?: throw IllegalStateException("ServiceRegistry not found in ServicesManager")

    private val regionTaskScheduler = registry.get(RegionTaskScheduler::class.java)
    private val itemBuilderFactory = registry.get(ItemBuilderFactory::class.java)!!
    private val folderProvider = registry.get(FeatureFolderProvider::class.java)!!
    private val messageService = registry.get(MessageService::class.java)!!
    private val profileRegistry = registry.get(ProfileRegistry::class.java)!!
    private val placeholderBridge = registry.get(PlaceholderService::class.java)!!
    private val configService = registry.get(ConfigService::class.java)!!

    val leaderboards = ConcurrentHashMap<String, CustomLeaderboardConfig>()
    val activePodiums = ConcurrentHashMap<String, ActivePodium>()
    
    private val leaderboardsFolder = folderProvider.getFeatureFolder("economy").resolve("leaderboards").toFile()

    init {
        // Inicializar directorio e cargar clasificacións
        setupLeaderboardsFolder()
        loadAllLeaderboards()
        
        // Spawn de podios persistidos
        taskScheduler.runAsync {
            spawnAllPersistedLeaderboards()
        }

        // Tarea de actualización y refresco de podios (cada 60s)
        taskScheduler.runAsyncTimer({
            for (player in Bukkit.getOnlinePlayers()) {
                updatePlayerStats(player)
            }
            refreshAllLeaderboards()
        }, 100L, 1200L)

        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    override suspend fun reload() {
        featureConfig.reload()
        loadAllLeaderboards()
        refreshAllLeaderboards()
    }

    private fun setupLeaderboardsFolder() {
        if (!leaderboardsFolder.exists()) {
            leaderboardsFolder.mkdirs()
        }

        // Crear clasificaciones por defecto si está vacía
        val defaultCreditsFile = leaderboardsFolder.resolve("credits.conf")
        if (!defaultCreditsFile.exists()) {
            configService.loadConfig(
                defaultCreditsFile,
                CustomLeaderboardConfig::class.java,
                CustomLeaderboardConfig(
                    id = "credits",
                    placeholder = "%coreplugin_balance_credits%",
                    displayName = "<gold><bold>TOP CRÉDITOS</bold></gold>"
                )
            )
        }

        val defaultKillsFile = leaderboardsFolder.resolve("kills.conf")
        if (!defaultKillsFile.exists()) {
            configService.loadConfig(
                defaultKillsFile,
                CustomLeaderboardConfig::class.java,
                CustomLeaderboardConfig(
                    id = "kills",
                    placeholder = "%statistic_player_kills%",
                    displayName = "<red><bold>TOP KILLS</bold></gold>"
                )
            )
        }
    }

    fun loadAllLeaderboards() {
        leaderboards.clear()
        val files = leaderboardsFolder.listFiles { _, name -> name.endsWith(".conf") } ?: emptyArray()

        for (file in files) {
            val id = file.nameWithoutExtension.lowercase()
            try {
                val config = configService.loadConfig(file, CustomLeaderboardConfig::class.java, CustomLeaderboardConfig(id = id))
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

    override fun spawnOrFindLeaderboard(location: Location, leaderboardId: String, rank: Int) {
        val key = "${leaderboardId}_$rank"
        
        regionTaskScheduler.runAtLocation(location) {
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
            entryDisplay.text(miniMessage.deserialize(messageService.getRawTemplate(LeaderboardMessages.LEADERBOARD_LOADING).ifEmpty { "<gold>Cargando...</gold>" }))

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
                headerDisplay.text(miniMessage.deserialize(messageService.getRawTemplate(LeaderboardMessages.LEADERBOARD_LOADING).ifEmpty { "<gold>Cargando...</gold>" }))
            } else {
                headerDisplays.forEach { it.remove() }
            }

            activePodiums[key] = ActivePodium(stand.uniqueId, location.clone())
        }
    }

    override fun registerLeaderboard(id: String, rank: Int, loc: Location): CompletableFuture<Void> {
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
                val file = leaderboardsFolder.resolve("$leaderboardId.conf")
                configService.saveConfig(file, CustomLeaderboardConfig::class.java, updatedConfig)
            } catch (e: Exception) {
                plugin.logger.severe("Fallo al guardar clasificación $leaderboardId: ${e.message}")
            }

            spawnOrFindLeaderboard(loc, leaderboardId, rank)
        }, { command -> taskScheduler.runAsync(command) })
    }

    override fun unregisterLeaderboard(id: String, rank: Int): CompletableFuture<Boolean> {
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
                val file = leaderboardsFolder.resolve("$leaderboardId.conf")
                configService.saveConfig(file, CustomLeaderboardConfig::class.java, updatedConfig)
            } catch (e: Exception) {
                plugin.logger.severe("Fallo al guardar clasificación $leaderboardId tras remover podio: ${e.message}")
            }

            if (activePodium != null) {
                regionTaskScheduler.runAtLocation(activePodium.location) {
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
        }, { command -> taskScheduler.runAsync(command) })
    }

    override fun updatePlayerStats(player: Player) {
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
        val db = databaseService.getDatabase("leaderboards")
        val userIdFuture = if (profile != null) {
            CompletableFuture.completedFuture(profile.internalId)
        } else {
            db.querySingle(
                "SELECT id FROM core_users WHERE uuid = ?",
                { rs -> rs.getInt("id") },
                uuid.toString()
            ).thenCompose { id ->
                if (id != null) {
                    CompletableFuture.completedFuture(id)
                } else {
                    val insertFuture = CompletableFuture<Int>()
                    db.executeTransaction { conn ->
                        val insertSql = "INSERT INTO core_users (uuid, username) VALUES (?, ?)"
                        conn.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS).use { stmt ->
                            stmt.setString(1, uuid.toString())
                            stmt.setString(2, username)
                            stmt.executeUpdate()
                            stmt.generatedKeys.use { gk ->
                                if (gk.next()) {
                                    insertFuture.complete(gk.getInt(1))
                                } else {
                                    insertFuture.completeExceptionally(java.sql.SQLException("No key"))
                                }
                            }
                        }
                    }.exceptionally { ex ->
                        insertFuture.completeExceptionally(ex)
                        null
                    }
                    insertFuture
                }
            }
        }

        userIdFuture.thenAccept { userId ->
            val upsertSql = "INSERT INTO player_scores (user_id, leaderboard_id, score) VALUES (?, ?, ?) ON CONFLICT(user_id, leaderboard_id) DO UPDATE SET score = excluded.score"
            db.executeUpdate(upsertSql, userId, leaderboardId, score)
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
        val db = databaseService.getDatabase("leaderboards")
        return db.query(
            sql,
            { rs ->
                val username = rs.getString("username")
                val score = rs.getDouble("score")
                java.util.AbstractMap.SimpleEntry(username, score) as Map.Entry<String, Double>
            },
            leaderboardId
        ).exceptionally { e ->
            plugin.logger.severe("Error al consultar el top 10 en la base de datos: ${e.message}")
            emptyList()
        }
    }

    override fun refreshAllLeaderboards() {
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
                regionTaskScheduler.runAtLocation(activePodium.location) {
                    val stand = Bukkit.getEntity(activePodium.uuid) as? ArmorStand ?: return@runAtLocation
                    
                    // Actualizar cabeza usando ItemBuilder
                    if (profile != null) {
                        val skull = itemBuilderFactory.createSkull()
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
                        messageService.getRawTemplate(LeaderboardMessages.LEADERBOARD_VACANT)
                            .ifEmpty { "<gray>#<pos> - Vacante</gray>" }
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
            }, { command -> taskScheduler.runAsync(command) })
            
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

    override fun reloadLeaderboards(): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            featureConfig.reload()
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
                        regionTaskScheduler.runAtLocation(active.location) {
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
                    }, { command -> taskScheduler.runAsync(command) })
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
                                regionTaskScheduler.runAtLocation(oldLoc) {
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
                                    regionTaskScheduler.runAtLocation(newLoc) {
                                        spawnOrFindLeaderboard(newLoc, id, rank)
                                    }
                                }.join()
                            } else {
                                // No cambió localización, refrescar displays y configuración regionalmente
                                val p2 = CompletableFuture<Void>()
                                regionTaskScheduler.runAtLocation(newLoc) {
                                    spawnOrFindLeaderboard(newLoc, id, rank)
                                    p2.complete(null)
                                }
                                p2.join()
                            }
                        } else {
                            // Nuevo podio añadido a mano, spawnear directamente regionalmente
                             val p3 = CompletableFuture<Void>()
                             regionTaskScheduler.runAtLocation(newLoc) {
                                 spawnOrFindLeaderboard(newLoc, id, rank)
                                 p3.complete(null)
                             }
                             p3.join()
                        }
                     }, { command -> taskScheduler.runAsync(command) })
                    
                    futures.add(future)
                }
            }
 
            // 3. Esperar a que se procesen todas las tareas regionales y refrescar stats
            CompletableFuture.allOf(*futures.toTypedArray()).thenRun {
                refreshAllLeaderboards()
            }.join()
        }, { command -> taskScheduler.runAsync(command) })
    }

    fun shutdown() {
        activePodiums.clear()
    }
}
