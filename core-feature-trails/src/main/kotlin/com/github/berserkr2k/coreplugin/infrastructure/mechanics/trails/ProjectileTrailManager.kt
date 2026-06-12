package com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails

import com.github.berserkr2k.coreplugin.api.framework.menu.MenuConfig
import com.github.berserkr2k.coreplugin.api.framework.menu.FillerConfig
import com.github.berserkr2k.coreplugin.api.config.ItemConfig
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture

import com.github.berserkr2k.coreplugin.api.framework.menu.MenuItemConfig
import com.github.berserkr2k.coreplugin.api.core.user.ProfileRegistry
import com.github.berserkr2k.coreplugin.api.core.user.UserProfile

class ProjectileTrailManager(
    private val plugin: Plugin,
    private val config: com.github.berserkr2k.coreplugin.api.core.config.FeatureConfig,
    private val taskScheduler: com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler,
    private val messageService: com.github.berserkr2k.coreplugin.api.core.message.MessageService
) : com.github.berserkr2k.coreplugin.api.feature.trails.ProjectileTrailService, com.github.berserkr2k.coreplugin.api.core.lifecycle.Reloadable {

    private val registry = org.bukkit.Bukkit.getServicesManager().load(com.github.berserkr2k.coreplugin.api.di.ServiceRegistry::class.java)
        ?: throw IllegalStateException("ServiceRegistry not found in ServicesManager")
    private val databaseService = registry.get(com.github.berserkr2k.coreplugin.api.core.database.DatabaseService::class.java)
    private val profileRegistry = registry.get(ProfileRegistry::class.java)
    private val folderProvider = registry.get(com.github.berserkr2k.coreplugin.api.core.filesystem.FeatureFolderProvider::class.java)

    private val mapperFactory = org.spongepowered.configurate.objectmapping.ObjectMapper.factoryBuilder()
        .defaultNamingScheme(org.spongepowered.configurate.util.NamingSchemes.PASSTHROUGH)
        .build()

    private fun <T : Any> loadHoconFile(file: File, configClass: Class<T>, defaultInstance: T): T {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }
        val loader = org.spongepowered.configurate.hocon.HoconConfigurationLoader.builder()
            .path(file.toPath())
            .defaultOptions { options ->
                options.serializers { builder ->
                    builder.registerAnnotatedObjects(mapperFactory)
                }
            }
            .build()
        val root = loader.load()
        val mapper = mapperFactory.get(configClass)
        return if (root.empty()) {
            mapper.save(defaultInstance, root)
            loader.save(root)
            defaultInstance
        } else {
            mapper.load(root) ?: defaultInstance
        }
    }

    private fun <T : Any> saveHoconFile(file: File, configClass: Class<T>, instance: T) {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }
        val loader = org.spongepowered.configurate.hocon.HoconConfigurationLoader.builder()
            .path(file.toPath())
            .defaultOptions { options ->
                options.serializers { builder ->
                    builder.registerAnnotatedObjects(mapperFactory)
                }
            }
            .build()
        val root = loader.load()
        val mapper = mapperFactory.get(configClass)
        mapper.save(instance, root)
        loader.save(root)
    }
    val trails = ConcurrentHashMap<String, TrailConfig>()
    
    private val trailsFolder = folderProvider.getFeatureFolder("trails").resolve("trails").toFile()

    var selectorConfig = MenuConfig(
        title = "<gold><bold>Estelas de Proyectil</bold></gold>",
        size = 27,
        filler = FillerConfig(
            enabled = true,
            item = ItemConfig(material = "GRAY_STAINED_GLASS_PANE", displayName = " ")
        )
    )
        private set

    private val selectorConfigFile = folderProvider.getFeatureFolder("trails").resolve("menus").resolve("trails-selector.conf").toFile()

    init {
        setupTrailsFolder()
        loadAllTrails()
        loadSelectorConfig().join()
    }

    private fun loadSelectorConfig(): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            this.selectorConfig = loadHoconFile(selectorConfigFile, MenuConfig::class.java, createDefaultSelectorConfig())
        }, { taskScheduler.runAsync(it) })
    }

    override suspend fun reload() {
        loadAllTrails()
        loadSelectorConfig().join()
    }

    private fun setupTrailsFolder() {
        if (!trailsFolder.exists()) {
            trailsFolder.mkdirs()
        }

        // 1. Estela Infernal (Fire)
        val defaultFireFile = trailsFolder.resolve("fire.conf")
        if (!defaultFireFile.exists()) {
            saveDefaultTrail(defaultFireFile, TrailConfig(
                id = "fire",
                permission = "core.trail.use.fire",
                particleType = "FLAME",
                particleCount = 2,
                baseInterval = 2,
                density = 5.5,
                speed = 0.015,
                offsetX = 0.03,
                offsetY = 0.03,
                offsetZ = 0.03,
                colorR = 255,
                colorG = 120,
                colorB = 40,
                dustSize = 1.2f,
                style = "DOUBLE_HELIX",
                spiralRadius = 0.28,
                spiralSpeed = 2.8,
                randomness = 0.08,
                pulse = true,
                speedScaling = true,
                lifetimeFade = true,
                layers = listOf(
                    TrailLayerConfig(particle = "FLAME", count = 2, speed = 0.015, radius = 0.28, offsetX = 0.03, offsetY = 0.03, offsetZ = 0.03),
                    TrailLayerConfig(particle = "SOUL_FIRE_FLAME", count = 1, speed = 0.008, radius = 0.18, offsetX = 0.01, offsetY = 0.01, offsetZ = 0.01),
                    TrailLayerConfig(particle = "SMOKE", count = 1, speed = 0.002, radius = 0.35, offsetX = 0.02, offsetY = 0.02, offsetZ = 0.02)
                ),
                gradient = listOf("#ff6600", "#ff2200", "#ffaa00"),
                item = ItemConfig(
                    material = "FLINT_AND_STEEL",
                    displayName = "<red><bold>Estela Infernal</bold></red>",
                    lore = listOf(
                        "<gray>Una ardiente doble hélice</gray>",
                        "<gray>envuelve tus proyectiles.</gray>",
                        "<dark_gray>Fuego, humo y brasas.</dark_gray>"
                    )
                )
            ))
        }

        // 2. Arcane Ribbon (Arcane)
        val defaultArcaneFile = trailsFolder.resolve("arcane.conf")
        if (!defaultArcaneFile.exists()) {
            saveDefaultTrail(defaultArcaneFile, TrailConfig(
                id = "arcane",
                permission = "core.trail.use.arcane",
                particleType = "END_ROD",
                particleCount = 3,
                baseInterval = 1,
                density = 8.0,
                speed = 0.01,
                offsetX = 0.04,
                offsetY = 0.04,
                offsetZ = 0.04,
                colorR = 168,
                colorG = 85,
                colorB = 247,
                dustSize = 1.4f,
                style = "RIBBON",
                spiralRadius = 0.22,
                spiralSpeed = 3.4,
                waveAmplitude = 0.35,
                waveSpeed = 3.0,
                randomness = 0.05,
                pulse = true,
                lifetimeFade = true,
                layers = listOf(
                    TrailLayerConfig(particle = "END_ROD", count = 2, speed = 0.01, radius = 0.22),
                    TrailLayerConfig(particle = "ENCHANT", count = 5, speed = 0.001, radius = 0.35),
                    TrailLayerConfig(particle = "GLOW", count = 1, speed = 0.003, radius = 0.15)
                ),
                gradient = listOf("#a855f7", "#3b82f6", "#ec4899"),
                item = ItemConfig(
                    material = "AMETHYST_SHARD",
                    displayName = "<light_purple><bold>Arcane Ribbon</bold></light_purple>",
                    lore = listOf(
                        "<gray>Ondas arcanas brillantes</gray>",
                        "<gray>siguen cada disparo.</gray>",
                        "<dark_gray>Energía ancestral.</dark_gray>"
                    )
                )
            ))
        }

        // 3. Thunder Shot (Thunder)
        val defaultThunderFile = trailsFolder.resolve("thunder.conf")
        if (!defaultThunderFile.exists()) {
            saveDefaultTrail(defaultThunderFile, TrailConfig(
                id = "thunder",
                permission = "core.trail.use.thunder",
                particleType = "ELECTRIC_SPARK",
                particleCount = 4,
                baseInterval = 1,
                density = 7.5,
                speed = 0.04,
                offsetX = 0.08,
                offsetY = 0.08,
                offsetZ = 0.08,
                colorR = 255,
                colorG = 255,
                colorB = 120,
                dustSize = 0.8f,
                style = "CHAOS",
                spiralRadius = 0.12,
                spiralSpeed = 6.0,
                randomness = 0.25,
                pulse = true,
                speedScaling = true,
                emissionMode = "BURST",
                burstEvery = 3,
                burstCount = 10,
                layers = listOf(
                    TrailLayerConfig(particle = "ELECTRIC_SPARK", count = 4, speed = 0.04, radius = 0.15),
                    TrailLayerConfig(particle = "CRIT", count = 2, speed = 0.03, radius = 0.10),
                    TrailLayerConfig(particle = "GLOW", count = 1, speed = 0.01, radius = 0.25)
                ),
                gradient = listOf("#fff700", "#ffffff", "#ffe066"),
                item = ItemConfig(
                    material = "LIGHTNING_ROD",
                    displayName = "<yellow><bold>Thunder Shot</bold></yellow>",
                    lore = listOf(
                        "<gray>Descargas eléctricas caóticas</gray>",
                        "<gray>acompañan tus proyectiles.</gray>",
                        "<dark_gray>Poder inestable.</dark_gray>"
                    )
                )
            ))
        }

        // 4. Frozen Soul (Frost)
        val defaultFrostFile = trailsFolder.resolve("frost.conf")
        if (!defaultFrostFile.exists()) {
            saveDefaultTrail(defaultFrostFile, TrailConfig(
                id = "frost",
                permission = "core.trail.use.frost",
                particleType = "SNOWFLAKE",
                particleCount = 3,
                baseInterval = 2,
                density = 6.5,
                speed = 0.01,
                offsetX = 0.03,
                offsetY = 0.03,
                offsetZ = 0.03,
                colorR = 170,
                colorG = 240,
                colorB = 255,
                dustSize = 1.1f,
                style = "HELIX",
                spiralRadius = 0.30,
                spiralSpeed = 2.2,
                randomness = 0.04,
                pulse = false,
                lifetimeFade = true,
                layers = listOf(
                    TrailLayerConfig(particle = "SNOWFLAKE", count = 3, speed = 0.01, radius = 0.30),
                    TrailLayerConfig(particle = "GLOW", count = 1, speed = 0.002, radius = 0.12),
                    TrailLayerConfig(particle = "FALLING_WATER", count = 1, speed = 0.001, radius = 0.22)
                ),
                gradient = listOf("#dff6ff", "#9bdfff", "#b8f3ff"),
                item = ItemConfig(
                    material = "BLUE_ICE",
                    displayName = "<aqua><bold>Frozen Soul</bold></aqua>",
                    lore = listOf(
                        "<gray>Un rastro helado y elegante</gray>",
                        "<gray>cubre cada proyectil.</gray>",
                        "<dark_gray>Frío absoluto.</dark_gray>"
                    )
                )
            ))
        }

        // 5. Void Collapse (Void)
        val defaultVoidFile = trailsFolder.resolve("void.conf")
        if (!defaultVoidFile.exists()) {
            saveDefaultTrail(defaultVoidFile, TrailConfig(
                id = "void",
                permission = "core.trail.use.void",
                particleType = "PORTAL",
                particleCount = 5,
                baseInterval = 1,
                density = 9.0,
                speed = 0.02,
                offsetX = 0.07,
                offsetY = 0.07,
                offsetZ = 0.07,
                colorR = 90,
                colorG = 0,
                colorB = 140,
                dustSize = 1.5f,
                style = "VORTEX",
                spiralRadius = 0.45,
                spiralSpeed = 4.5,
                randomness = 0.18,
                pulse = true,
                lifetimeFade = true,
                layers = listOf(
                    TrailLayerConfig(particle = "PORTAL", count = 5, speed = 0.02, radius = 0.45),
                    TrailLayerConfig(particle = "DRAGON_BREATH", count = 2, speed = 0.008, radius = 0.25),
                    TrailLayerConfig(particle = "REVERSE_PORTAL", count = 1, speed = 0.003, radius = 0.55)
                ),
                gradient = listOf("#240046", "#5a189a", "#9d4edd"),
                item = ItemConfig(
                    material = "ENDER_EYE",
                    displayName = "<dark_purple><bold>Void Collapse</bold></dark_purple>",
                    lore = listOf(
                        "<gray>La energía del vacío</gray>",
                        "<gray>distorsiona el entorno.</gray>",
                        "<dark_gray>Oscuridad inestable.</dark_gray>"
                    )
                )
            ))
        }
    }

    private fun saveDefaultTrail(file: File, trailConfig: TrailConfig) {
        try {
            saveHoconFile(file, TrailConfig::class.java, trailConfig)
        } catch (e: Exception) {
            plugin.logger.severe("Fallo al guardar estela por defecto: ${e.message}")
        }
    }

    override fun loadAllTrails() {
        trails.clear()
        val files = trailsFolder.listFiles { _, name -> name.endsWith(".conf") } ?: emptyArray()

        for (file in files) {
            val id = file.nameWithoutExtension.lowercase()
            try {
                val loadedConfig = loadHoconFile(file, TrailConfig::class.java, TrailConfig(id = id))
                
                val finalId = if (loadedConfig.id.isNotEmpty()) loadedConfig.id.lowercase() else id
                trails[finalId] = loadedConfig
                if (finalId != id) {
                    trails[id] = loadedConfig
                }
            } catch (e: Exception) {
                plugin.logger.severe("Error al cargar la estela desde ${file.name}: ${e.message}")
            }
        }
    }



    private fun createDefaultSelectorConfig(): MenuConfig {
        return MenuConfig(
            title = "<gold><bold>Estelas de Proyectil</bold></gold>",
            size = 27,
            filler = FillerConfig(
                enabled = true,
                item = ItemConfig(material = "GRAY_STAINED_GLASS_PANE", displayName = " ")
            ),
            items = mapOf(
                "clear" to MenuItemConfig(
                    slots = listOf(22),
                    item = ItemConfig(
                        material = "BARRIER",
                        displayName = "<red><bold>❌ Quitar Estela</bold></red>",
                        lore = listOf(
                            "<gray>Haz click aquí para remover tu</gray>",
                            "<gray>estela de partículas activa.</gray>",
                            " ",
                            "<yellow>⚡ Click para remover</yellow>"
                        )
                    ),
                    action = "clear",
                    sound = "BLOCK_LAVA_EXTINGUISH"
                )
            )
        )
    }

    override fun getActiveTrail(uuid: UUID): String? {
        return profileRegistry.getProfile(uuid)?.activeTrailId
    }

    // Cargar estela de base de datos de manera asíncrona
    override fun loadPlayerTrail(uuid: UUID): CompletableFuture<String?> {
        val sql = """
            SELECT t.trail_id FROM core_player_projectile_trails t
            JOIN core_users u ON t.user_id = u.id
            WHERE u.uuid = ?
        """.trimIndent()
        return databaseService.getDatabase("trails").querySingle(
            sql,
            { rs -> rs.getString("trail_id") },
            uuid.toString()
        ).thenApply { trailId ->
            if (trailId != null) {
                profileRegistry.getProfile(uuid)?.setTrail(trailId)
            }
            trailId
        }.exceptionally { e ->
            plugin.logger.severe("Error al cargar la estela del jugador $uuid de la DB: ${e.message}")
            null
        }
    }

    // Guardar estela de base de datos de manera asíncrona
    override fun savePlayerTrail(uuid: UUID, trailId: String?): CompletableFuture<Void> {
        val profile = profileRegistry.getProfile(uuid)
        if (profile != null) {
            profile.setTrail(trailId)
            return CompletableFuture.completedFuture(null)
        }

        val db = databaseService.getDatabase("trails")
        return db.executeTransaction { conn ->
            var userId = -1
            conn.prepareStatement("SELECT id FROM core_users WHERE uuid = ?").use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeQuery().use { rs ->
                    if (rs.next()) userId = rs.getInt("id")
                }
            }
            if (userId == -1) return@executeTransaction

            if (trailId == null) {
                conn.prepareStatement("DELETE FROM core_player_projectile_trails WHERE user_id = ?").use { stmt ->
                    stmt.setInt(1, userId)
                    stmt.executeUpdate()
                }
            } else {
                val upsertSql = "INSERT INTO core_player_projectile_trails (user_id, trail_id) VALUES (?, ?) ON CONFLICT(user_id) DO UPDATE SET trail_id = excluded.trail_id"
                conn.prepareStatement(upsertSql).use { stmt ->
                    stmt.setInt(1, userId)
                    stmt.setString(2, trailId)
                    stmt.executeUpdate()
                }
            }
        }.exceptionally { e ->
            plugin.logger.severe("Error al guardar la estela del jugador $uuid en la DB: ${e.message}")
            null
        }
    }

    // Calcula el intervalo óptimo de emisión dinámicamente según el lag del servidor
    fun getDynamicInterval(baseInterval: Int, velocityLength: Double): Int {
        if (velocityLength < 0.05) {
            return 8 // Si el proyectil está estático, reducir masivamente la frecuencia
        }

        // Obtener salud de TPS y MSPT
        val tps = try {
            Bukkit.getTPS()[0]
        } catch (e: Exception) {
            20.0
        }

        val mspt = try {
            Bukkit.getAverageTickTime()
        } catch (e: Exception) {
            25.0
        }

        return when {
            mspt > 50.0 || tps < 16.0 -> 8
            mspt > 45.0 || tps < 17.5 -> 6
            mspt > 40.0 || tps < 18.5 -> 4
            else -> baseInterval
        }
    }
}
