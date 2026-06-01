package com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails

import com.github.berserkr2k.coreplugin.common.gui.MenuConfig
import com.github.berserkr2k.coreplugin.common.gui.FillerConfig
import com.github.berserkr2k.coreplugin.common.gui.ItemConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import com.github.berserkr2k.coreplugin.infrastructure.database.DatabaseService
import com.github.berserkr2k.coreplugin.infrastructure.database.*
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture

import com.github.berserkr2k.coreplugin.common.gui.MenuItemConfig

class ProjectileTrailManager(
    private val plugin: Plugin,
    private val databaseService: DatabaseService,
    private val configManager: ModularConfigManager
) {
    val activePlayerTrails = ConcurrentHashMap<UUID, String>()
    val trails = ConcurrentHashMap<String, TrailConfig>()
    
    private val trailsFolder = plugin.dataFolder.resolve("trails")

    val selectorConfig: MenuConfig by lazy {
        configManager.loadModuleConfig("menus/trails-selector.conf", MenuConfig::class.java, createDefaultSelectorConfig()).join()
    }

    init {
        setupTrailsFolder()
        loadAllTrails()
        
        databaseService.initFuture.thenAccept {
            setupDatabaseTable()
        }
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
                displayName = "<red><bold>Estela Infernal</bold></red>",
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
                guiIcon = "FLINT_AND_STEEL",
                guiLore = listOf(
                    "<gray>Una ardiente doble hélice</gray>",
                    "<gray>envuelve tus proyectiles.</gray>",
                    "<dark_gray>Fuego, humo y brasas.</dark_gray>"
                )
            ))
        }

        // 2. Arcane Ribbon (Arcane)
        val defaultArcaneFile = trailsFolder.resolve("arcane.conf")
        if (!defaultArcaneFile.exists()) {
            saveDefaultTrail(defaultArcaneFile, TrailConfig(
                id = "arcane",
                displayName = "<light_purple><bold>Arcane Ribbon</bold></light_purple>",
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
                guiIcon = "AMETHYST_SHARD",
                guiLore = listOf(
                    "<gray>Ondas arcanas brillantes</gray>",
                    "<gray>siguen cada disparo.</gray>",
                    "<dark_gray>Energía ancestral.</dark_gray>"
                )
            ))
        }

        // 3. Thunder Shot (Thunder)
        val defaultThunderFile = trailsFolder.resolve("thunder.conf")
        if (!defaultThunderFile.exists()) {
            saveDefaultTrail(defaultThunderFile, TrailConfig(
                id = "thunder",
                displayName = "<yellow><bold>Thunder Shot</bold></yellow>",
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
                guiIcon = "LIGHTNING_ROD",
                guiLore = listOf(
                    "<gray>Descargas eléctricas caóticas</gray>",
                    "<gray>acompañan tus proyectiles.</gray>",
                    "<dark_gray>Poder inestable.</dark_gray>"
                )
            ))
        }

        // 4. Frozen Soul (Frost)
        val defaultFrostFile = trailsFolder.resolve("frost.conf")
        if (!defaultFrostFile.exists()) {
            saveDefaultTrail(defaultFrostFile, TrailConfig(
                id = "frost",
                displayName = "<aqua><bold>Frozen Soul</bold></aqua>",
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
                guiIcon = "BLUE_ICE",
                guiLore = listOf(
                    "<gray>Un rastro helado y elegante</gray>",
                    "<gray>cubre cada proyectil.</gray>",
                    "<dark_gray>Frío absoluto.</dark_gray>"
                )
            ))
        }

        // 5. Void Collapse (Void)
        val defaultVoidFile = trailsFolder.resolve("void.conf")
        if (!defaultVoidFile.exists()) {
            saveDefaultTrail(defaultVoidFile, TrailConfig(
                id = "void",
                displayName = "<dark_purple><bold>Void Collapse</bold></dark_purple>",
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
                guiIcon = "ENDER_EYE",
                guiLore = listOf(
                    "<gray>La energía del vacío</gray>",
                    "<gray>distorsiona el entorno.</gray>",
                    "<dark_gray>Oscuridad inestable.</dark_gray>"
                )
            ))
        }
    }

    private fun saveDefaultTrail(file: File, config: TrailConfig) {
        try {
            configManager.saveModuleConfig("trails/${file.name}", TrailConfig::class.java, config).join()
        } catch (e: Exception) {
            plugin.logger.severe("Fallo al guardar estela por defecto: ${e.message}")
        }
    }

    fun loadAllTrails() {
        trails.clear()
        val files = trailsFolder.listFiles { _, name -> name.endsWith(".conf") } ?: emptyArray()

        for (file in files) {
            val id = file.nameWithoutExtension.lowercase()
            try {
                val config = configManager.loadModuleConfig("trails/${file.name}", TrailConfig::class.java, TrailConfig(id = id)).join()
                
                val finalId = if (config.id.isNotEmpty()) config.id.lowercase() else id
                trails[finalId] = config
                if (finalId != id) {
                    trails[id] = config
                }
            } catch (e: Exception) {
                plugin.logger.severe("Error al cargar la estela desde ${file.name}: ${e.message}")
            }
        }
    }

    private fun setupDatabaseTable() {
        try {
            databaseService.execute("CREATE TABLE IF NOT EXISTS player_projectile_trails (uuid VARCHAR(36) PRIMARY KEY, trail_id VARCHAR(32))")
        } catch (e: Exception) {
            plugin.logger.severe("Fallo al inicializar la tabla player_projectile_trails: ${e.message}")
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

    // Cargar estela de base de datos de manera asíncrona
    fun loadPlayerTrail(uuid: UUID): CompletableFuture<String?> {
        val sql = "SELECT trail_id FROM player_projectile_trails WHERE uuid = ?"
        return databaseService.querySingleAsync(
            sql,
            preparer = { stmt -> stmt.setString(1, uuid.toString()) },
            mapper = { rs -> rs.getString("trail_id") }
        ).thenApply { trailId ->
            if (trailId != null) {
                activePlayerTrails[uuid] = trailId
            }
            trailId
        }.exceptionally { e ->
            plugin.logger.severe("Error al cargar la estela del jugador $uuid de la DB: ${e.message}")
            null
        }
    }

    // Guardar estela de base de datos de manera asíncrona
    fun savePlayerTrail(uuid: UUID, trailId: String?): CompletableFuture<Void> {
        val future = if (trailId == null) {
            activePlayerTrails.remove(uuid)
            databaseService.executeAsync("DELETE FROM player_projectile_trails WHERE uuid = ?") { stmt ->
                stmt.setString(1, uuid.toString())
            }
        } else {
            activePlayerTrails[uuid] = trailId
            databaseService.executeAsync("REPLACE INTO player_projectile_trails (uuid, trail_id) VALUES (?, ?)") { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.setString(2, trailId)
            }
        }
        return future.thenAccept { }.exceptionally { e ->
            plugin.logger.severe("Error al guardar la estela del jugador $uuid en la DB: ${e.message}")
            null as Void?
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
