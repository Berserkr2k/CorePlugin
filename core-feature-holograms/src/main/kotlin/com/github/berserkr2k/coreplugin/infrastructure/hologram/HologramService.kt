package com.github.berserkr2k.coreplugin.infrastructure.hologram

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.entity.Interaction
import java.util.concurrent.ConcurrentHashMap
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.events.ListenerPriority
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.filesystem.FeatureFolderProvider
import com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler
import com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler
import com.github.berserkr2k.coreplugin.api.core.scheduler.Task
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.util.NamingSchemes

class HologramService(
    private val plugin: Plugin,
    private val placeholderService: com.github.berserkr2k.coreplugin.api.core.placeholder.PlaceholderService,
    private val registry: ServiceRegistry
) : com.github.berserkr2k.coreplugin.api.feature.holograms.HologramService, com.github.berserkr2k.coreplugin.api.core.lifecycle.Reloadable {
    private val activeHolograms = ConcurrentHashMap<String, ModernHologram>()
    private val refreshTasks = ConcurrentHashMap<String, Task>()
    private val configs = ConcurrentHashMap<String, HologramConfig>()
    
    private val folderProvider = registry.get(FeatureFolderProvider::class.java)!!
    private val hologramsFolder = folderProvider.getFeatureFolder("holograms").resolve("holograms").toFile()
    
    private val taskScheduler = registry.get(TaskScheduler::class.java)
    private val regionTaskScheduler = registry.get(RegionTaskScheduler::class.java)

    private val mapperFactory = ObjectMapper.factoryBuilder()
        .defaultNamingScheme(NamingSchemes.PASSTHROUGH)
        .build()

    init {
        // 1. Crear carpeta de hologramas si no existe, y poblar con default si está vacía
        setupDefaultHolograms()

        // 3. Cargar todos los hologramas de forma síncrona/unificada en bootstrap
        loadAllHolograms()

        // 4. Tarea repetitiva asíncrona de seguimiento de rango de visión (cada 1 segundo)
        taskScheduler.runAsyncTimer({
            updateTracking()
        }, 20L, 20L)

        // 5. Registro del receptor de clics en la hitbox del cliente usando ProtocolLib
        registerPacketListener()

        // 6. Registro del listener de desconexión del jugador en el programador global
        taskScheduler.runSync {
            plugin.server.pluginManager.registerEvents(HologramListener(plugin, this), plugin)
        }
    }

    override suspend fun reload() {
        reloadHolograms().join()
    }

    private fun setupDefaultHolograms() {
        if (!hologramsFolder.exists()) {
            hologramsFolder.mkdirs()
        }
    }

    private fun loadConfig(file: java.io.File, id: String): HologramConfig {
        val loader = HoconConfigurationLoader.builder()
            .path(file.toPath())
            .defaultOptions { options ->
                options.serializers { builder ->
                    builder.registerAnnotatedObjects(mapperFactory)
                }
            }
            .build()
        val root = loader.load()
        val mapper = mapperFactory.get(HologramConfig::class.java)
        return mapper.load(root) ?: HologramConfig(id = id)
    }

    private fun saveConfig(file: java.io.File, config: HologramConfig) {
        val loader = HoconConfigurationLoader.builder()
            .path(file.toPath())
            .defaultOptions { options ->
                options.serializers { builder ->
                    builder.registerAnnotatedObjects(mapperFactory)
                }
            }
            .build()
        val root = loader.load()
        val mapper = mapperFactory.get(HologramConfig::class.java)
        mapper.save(config, root)
        loader.save(root)
    }

    fun loadAllHolograms() {
        shutdown()
        configs.clear()

        if (!hologramsFolder.exists()) {
            hologramsFolder.mkdirs()
        }

        val files = hologramsFolder.listFiles { _, name -> name.endsWith(".conf") } ?: emptyArray()

        val futures = files.map { file ->
            val id = file.nameWithoutExtension.lowercase()
            java.util.concurrent.CompletableFuture.supplyAsync({
                loadConfig(file, id)
            }, { command -> taskScheduler.runAsync(command) }).thenAccept { loadedConfig ->
                configs[id] = loadedConfig

                val world = Bukkit.getWorld(loadedConfig.world)
                if (world != null) {
                    val loc = Location(world, loadedConfig.x, loadedConfig.y, loadedConfig.z)

                    // Limpieza de entidades físicas remanentes de versiones anteriores
                    regionTaskScheduler.runAtLocation(loc) {
                        world.getNearbyEntities(loc, 3.0, 5.0, 3.0) { entity ->
                            entity is TextDisplay || entity is Interaction
                        }.forEach { it.remove() }
                    }

                    // Inicialización de los hologramas virtuales basados en paquetes
                    val holo = ModernHologram(id, loc, plugin, placeholderService)
                    holo.clickCommand = loadedConfig.clickCommand
                    holo.lineSpacing = loadedConfig.lineSpacing
                    holo.backgroundColor = loadedConfig.backgroundColor
                    holo.renderDistance = loadedConfig.renderDistance
                    holo.setup(loadedConfig.lines)
                    activeHolograms[id] = holo

                    // Si es actualizable, programar su propia tarea de actualización
                    if (loadedConfig.updatable) {
                        scheduleHologramUpdate(holo, loadedConfig.updateInterval)
                    }
                }
            }
        }

        java.util.concurrent.CompletableFuture.allOf(*futures.toTypedArray()).join()
    }

    private fun updateTracking() {
        val players = Bukkit.getOnlinePlayers()
        for (player in players) {
            activeHolograms.values.forEach { holo ->
                val dist = holo.renderDistance.toDouble()
                val inRange = player.world == holo.location.world && player.location.distanceSquared(holo.location) <= dist * dist
                val isViewing = holo.viewers.contains(player.uniqueId)
                if (inRange && !isViewing) {
                    holo.showTo(player)
                } else if (!inRange && isViewing) {
                    holo.hideFrom(player)
                }
            }
        }
    }

    private fun registerPacketListener() {
        val protocolManager = ProtocolLibrary.getProtocolManager()
        protocolManager.addPacketListener(object : PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.USE_ENTITY) {
            override fun onPacketReceiving(event: PacketEvent) {
                val player = event.player
                val packet = event.packet
                val targetId = packet.getIntegers().read(0)

                val holo = getHologramByInteractionId(targetId) ?: return

                // Ejecución segura en el programador de la región del jugador
                regionTaskScheduler.runAtLocation(player.location) {
                    holo.clickCommand?.let { cmd ->
                        val parsedCmd = cmd.replace("%player%", player.name)
                        player.performCommand(parsedCmd)
                    }
                }
            }
        })
    }

    fun getHologramByInteractionId(id: Int): ModernHologram? {
        return activeHolograms.values.find { it.isInteractionEntity(id) }
    }

    override fun createHologram(id: String, location: Location, lines: List<String>) {
        createHologram(id, location, lines, null, null, null, false, 1, null)
    }

    /**
     * Crea dinámicamente un holograma packet-based e impone unicidad eliminando cualquier duplicado previo.
     */
    fun createHologram(
        id: String,
        location: Location,
        lines: List<String>,
        clickCommand: String? = null,
        lineSpacing: Double? = null,
        backgroundColor: Int? = null,
        updatable: Boolean = false,
        updateInterval: Int = 1,
        renderDistance: Int? = null
    ): ModernHologram {
        // Garantizar unicidad del ID
        deleteHologram(id)

        val holo = ModernHologram(id, location, plugin, placeholderService)
        holo.clickCommand = clickCommand
        holo.lineSpacing = lineSpacing ?: 0.28
        holo.backgroundColor = backgroundColor ?: 1073741824
        holo.renderDistance = renderDistance ?: 48
        holo.setup(lines)
        activeHolograms[id] = holo

        // Mostrar instantáneamente a los jugadores en rango
        Bukkit.getOnlinePlayers().forEach { player ->
            val dist = holo.renderDistance.toDouble()
            if (player.world == location.world && player.location.distanceSquared(location) <= dist * dist) {
                holo.showTo(player)
            }
        }

        // Si es actualizable, programar su propia tarea de actualización
        if (updatable) {
            scheduleHologramUpdate(holo, updateInterval)
        }

        // Guardar la configuración modular en HOCON por holograma
        val newConfig = HologramConfig(
            id = id,
            world = location.world.name,
            x = location.x,
            y = location.y,
            z = location.z,
            lines = lines,
            clickCommand = clickCommand,
            lineSpacing = lineSpacing ?: 0.28,
            backgroundColor = backgroundColor ?: 1073741824,
            updatable = updatable,
            updateInterval = updateInterval,
            renderDistance = renderDistance ?: 48
        )
        configs[id] = newConfig
        saveConfig(hologramsFolder.resolve("$id.conf"), newConfig)

        return holo
    }

    /**
     * Edita dinámicamente las líneas de texto de un holograma y lo guarda en disco.
     */
    override fun editHologram(id: String, lines: List<String>): Boolean {
        val holo = activeHolograms[id] ?: return false
        holo.updateText(lines)

        val oldConfig = configs[id] ?: return false
        val newConfig = oldConfig.copy(lines = lines)
        configs[id] = newConfig
        saveConfig(hologramsFolder.resolve("$id.conf"), newConfig)
        return true
    }

    /**
     * Traslada la posición de un holograma y refresca su renderizado y configuración.
     */
    override fun moveHologram(id: String, newLocation: Location): Boolean {
        val holo = activeHolograms[id] ?: return false

        // Ocultar a los espectadores actuales
        val activeViewers = holo.viewers.mapNotNull { Bukkit.getPlayer(it) }
        activeViewers.forEach { holo.hideFrom(it) }

        // Actualizar ubicación física y reconfigurar IDs
        holo.location = newLocation
        holo.setup(holo.textLines)

        // Volver a mostrar a los espectadores correspondientes
        activeViewers.forEach { holo.showTo(it) }

        // Persistir cambio en disco
        val oldConfig = configs[id] ?: return false
        val newConfig = oldConfig.copy(
            x = newLocation.x,
            y = newLocation.y,
            z = newLocation.z,
            world = newLocation.world.name
        )
        configs[id] = newConfig
        saveConfig(hologramsFolder.resolve("$id.conf"), newConfig)
        return true
    }

    /**
     * Elimina el holograma físicamente para todos los clientes y lo remueve del disco.
     */
    override fun deleteHologram(id: String): Boolean {
        refreshTasks.remove(id)?.cancel()
        val holo = activeHolograms.remove(id) ?: return false
        holo.delete()

        configs.remove(id)
        val file = hologramsFolder.resolve("$id.conf")
        if (file.exists()) {
            file.delete()
        }
        return true
    }

    fun handleQuit(player: Player) {
        activeHolograms.values.forEach { holo ->
            holo.viewers.remove(player.uniqueId)
        }
    }

    override fun getActiveHolograms(): Map<String, Location> = activeHolograms.mapValues { it.value.location }
    fun getActiveHologramObjects(): Map<String, ModernHologram> = activeHolograms

    /**
     * Recarga todos los hologramas desde la carpeta de configuraciones.
     */
    override fun reloadHolograms(): java.util.concurrent.CompletableFuture<Void> {
        return java.util.concurrent.CompletableFuture.runAsync({
            loadAllHolograms()
            plugin.logger.info("¡Se han recargado con éxito ${activeHolograms.size} hologramas desde la carpeta holograms/!")
        }, { command -> taskScheduler.runAsync(command) })
    }

    private fun scheduleHologramUpdate(holo: ModernHologram, intervalMinutes: Int) {
        refreshTasks.remove(holo.id)?.cancel()
        val intervalTicks = (intervalMinutes * 60 * 20).toLong()
        val task = taskScheduler.runAsyncTimer({
            if (activeHolograms.containsKey(holo.id)) {
                holo.refreshForViewers()
            }
        }, intervalTicks, intervalTicks)
        refreshTasks[holo.id] = task
    }

    fun shutdown() {
        refreshTasks.values.forEach { it.cancel() }
        refreshTasks.clear()
        activeHolograms.values.forEach { it.delete(true) }
        activeHolograms.clear()
    }
}
