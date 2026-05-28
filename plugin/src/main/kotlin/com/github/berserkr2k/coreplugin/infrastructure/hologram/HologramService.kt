package com.github.berserkr2k.coreplugin.infrastructure.hologram

import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import com.github.berserkr2k.coreplugin.common.LegacyPlaceholderBridge
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

class HologramService(
    private val plugin: Plugin,
    private val configManager: ModularConfigManager,
    private val placeholderBridge: LegacyPlaceholderBridge
) {
    private val activeHolograms = ConcurrentHashMap<String, ModernHologram>()
    private val refreshTasks = ConcurrentHashMap<String, io.papermc.paper.threadedregions.scheduler.ScheduledTask>()
    lateinit var config: HologramConfig
        private set

    init {
        configManager.loadModuleConfig("holograms.conf", HologramConfig::class.java, HologramConfig())
            .thenAccept { loadedConfig ->
                this.config = loadedConfig
                
                // 1. Limpieza de entidades físicas remanentes de versiones anteriores
                loadedConfig.holograms.forEach { (id, ph) ->
                    val world = Bukkit.getWorld(ph.world)
                    if (world != null) {
                        val loc = Location(world, ph.x, ph.y, ph.z)
                        Bukkit.getRegionScheduler().execute(plugin, loc) {
                            world.getNearbyEntities(loc, 3.0, 5.0, 3.0) { entity ->
                                entity is TextDisplay || entity is Interaction
                            }.forEach { it.remove() }
                        }
                    }
                }

                // 2. Inicialización de los hologramas virtuales basados en paquetes
                loadedConfig.holograms.forEach { (id, ph) ->
                    val world = Bukkit.getWorld(ph.world)
                    if (world != null) {
                        val loc = Location(world, ph.x, ph.y, ph.z)
                        val holo = ModernHologram(id, loc, plugin, placeholderBridge)
                        holo.clickCommand = ph.clickCommand
                        holo.lineSpacing = ph.lineSpacing
                        holo.backgroundColor = ph.backgroundColor
                        holo.renderDistance = ph.renderDistance
                        holo.setup(ph.lines)
                        activeHolograms[id] = holo

                        // Si es actualizable, programar su propia tarea de actualización
                        if (ph.updatable) {
                            scheduleHologramUpdate(holo, ph.updateInterval)
                        }
                    }
                }
                
                // 3. Tarea repetitiva asíncrona de seguimiento de rango de visión (cada 1 segundo)
                Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _ ->
                    updateTracking()
                }, 1, 1, java.util.concurrent.TimeUnit.SECONDS)

                // 4. Registro del receptor de clics en la hitbox del cliente usando ProtocolLib
                registerPacketListener()

                // 5. Registro del listener de desconexión del jugador en el programador global
                Bukkit.getGlobalRegionScheduler().execute(plugin) {
                    plugin.server.pluginManager.registerEvents(HologramListener(plugin, this), plugin)
                }
            }
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
                Bukkit.getRegionScheduler().execute(plugin, player.location) {
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

        val holo = ModernHologram(id, location, plugin, placeholderBridge)
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

        // Actualizar y persistir la configuración modular en HOCON
        val newHolograms = config.holograms.toMutableMap()
        newHolograms[id] = HologramConfig.PersistedHologram(
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
        config = HologramConfig(newHolograms)
        configManager.saveModuleConfig("holograms.conf", HologramConfig::class.java, config)

        return holo
    }

    /**
     * Edita dinámicamente las líneas de texto de un holograma y lo guarda en disco.
     */
    fun editHologram(id: String, lines: List<String>): Boolean {
        val holo = activeHolograms[id] ?: return false
        holo.updateText(lines)

        val newHolograms = config.holograms.toMutableMap()
        val ph = newHolograms[id]
        if (ph != null) {
            newHolograms[id] = ph.copy(lines = lines)
            config = HologramConfig(newHolograms)
            configManager.saveModuleConfig("holograms.conf", HologramConfig::class.java, config)
        }
        return true
    }

    /**
     * Traslada la posición de un holograma y refresca su renderizado y configuración.
     */
    fun moveHologram(id: String, newLocation: Location): Boolean {
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
        val newHolograms = config.holograms.toMutableMap()
        val ph = newHolograms[id]
        if (ph != null) {
            newHolograms[id] = ph.copy(
                x = newLocation.x,
                y = newLocation.y,
                z = newLocation.z,
                world = newLocation.world.name
            )
            config = HologramConfig(newHolograms)
            configManager.saveModuleConfig("holograms.conf", HologramConfig::class.java, config)
        }
        return true
    }

    /**
     * Elimina el holograma físicamente para todos los clientes y lo remueve del disco.
     */
    fun deleteHologram(id: String): Boolean {
        refreshTasks.remove(id)?.cancel()
        val holo = activeHolograms.remove(id) ?: return false
        holo.delete()

        val newHolograms = config.holograms.toMutableMap()
        newHolograms.remove(id)
        config = HologramConfig(newHolograms)
        configManager.saveModuleConfig("holograms.conf", HologramConfig::class.java, config)

        return true
    }

    fun handleQuit(player: Player) {
        activeHolograms.values.forEach { holo ->
            holo.viewers.remove(player.uniqueId)
        }
    }

    fun getActiveHolograms(): Map<String, ModernHologram> = activeHolograms

    /**
     * Recarga todos los hologramas desde el archivo de configuración HOCON.
     */
    fun reloadHolograms(): java.util.concurrent.CompletableFuture<Void> {
        shutdown()
        return configManager.loadModuleConfig("holograms.conf", HologramConfig::class.java, HologramConfig())
            .thenAccept { loadedConfig ->
                this.config = loadedConfig
                loadedConfig.holograms.forEach { (id, ph) ->
                    val world = Bukkit.getWorld(ph.world)
                    if (world != null) {
                        val loc = Location(world, ph.x, ph.y, ph.z)
                        val holo = ModernHologram(id, loc, plugin, placeholderBridge)
                        holo.clickCommand = ph.clickCommand
                        holo.lineSpacing = ph.lineSpacing
                        holo.backgroundColor = ph.backgroundColor
                        holo.renderDistance = ph.renderDistance
                        holo.setup(ph.lines)
                        activeHolograms[id] = holo

                        if (ph.updatable) {
                            scheduleHologramUpdate(holo, ph.updateInterval)
                        }
                    }
                }
                plugin.logger.info("¡Se han recargado con éxito ${activeHolograms.size} hologramas desde holograms.conf!")
            }
    }

    private fun scheduleHologramUpdate(holo: ModernHologram, intervalMinutes: Int) {
        refreshTasks.remove(holo.id)?.cancel()
        val intervalSeconds = (intervalMinutes * 60).toLong()
        val task = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _ ->
            if (activeHolograms.containsKey(holo.id)) {
                holo.refreshForViewers()
            }
        }, intervalSeconds, intervalSeconds, java.util.concurrent.TimeUnit.SECONDS)
        refreshTasks[holo.id] = task
    }

    fun shutdown() {
        refreshTasks.values.forEach { it.cancel() }
        refreshTasks.clear()
        activeHolograms.values.forEach { it.delete(true) }
        activeHolograms.clear()
    }
}
