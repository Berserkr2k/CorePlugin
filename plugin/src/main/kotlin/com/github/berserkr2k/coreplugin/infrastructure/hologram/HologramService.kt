package com.github.berserkr2k.coreplugin.infrastructure.hologram

import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap

class HologramService(
    private val plugin: Plugin,
    private val configManager: ModularConfigManager
) {
    private val activeHolograms = ConcurrentHashMap<String, ModernHologram>()
    lateinit var config: HologramConfig
        private set

    init {
        configManager.loadModuleConfig("holograms.conf", HologramConfig::class.java, HologramConfig())
            .thenAccept { loadedConfig ->
                this.config = loadedConfig
                
                // Carga y generación física de hologramas persistentes
                loadedConfig.holograms.forEach { (id, ph) ->
                    val world = Bukkit.getWorld(ph.world)
                    if (world != null) {
                        val loc = Location(world, ph.x, ph.y, ph.z)
                        val holo = ModernHologram(id, loc, plugin)
                        holo.clickCommand = ph.clickCommand
                        holo.spawn(ph.lines)
                        activeHolograms[id] = holo
                    }
                }
                
                // Registro del listener de colisiones en el planificador maestro
                Bukkit.getGlobalRegionScheduler().execute(plugin) {
                    plugin.server.pluginManager.registerEvents(HologramListener(plugin, this), plugin)
                }
            }
    }

    /**
     * Crea dinámicamente un holograma interactivo en el mundo físico y lo registra en memoria.
     */
    fun createHologram(id: String, location: Location, lines: List<String>, clickCommand: String? = null): ModernHologram {
        val holo = ModernHologram(id, location, plugin)
        holo.clickCommand = clickCommand
        holo.spawn(lines)
        activeHolograms[id] = holo
        return holo
    }

    /**
     * Elimina físicamente un holograma por su identificador.
     */
    fun deleteHologram(id: String): Boolean {
        val holo = activeHolograms.remove(id) ?: return false
        holo.delete()
        return true
    }

    /**
     * Busca un holograma mediante el UUID de su entidad de interacción (hitbox).
     */
    fun getHologramByInteraction(uuid: java.util.UUID): ModernHologram? {
        return activeHolograms.values.find { it.isInteractionEntity(uuid) }
    }

    fun getActiveHolograms(): Map<String, ModernHologram> = activeHolograms

    /**
     * Desintegra físicamente todos los hologramas activos.
     */
    fun shutdown() {
        activeHolograms.values.forEach { it.delete() }
        activeHolograms.clear()
    }
}
