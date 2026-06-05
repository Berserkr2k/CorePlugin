package com.github.berserkr2k.coreplugin.infrastructure.mechanics

import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

class AnvilModule(private val plugin: Plugin, private val configManager: ModularConfigManager) {
    lateinit var config: AnvilConfig
        private set
    private var listener: AnvilListener? = null

    init {
        configManager.loadModuleConfig("anvil.conf", AnvilConfig::class.java, AnvilConfig())
            .thenAccept { loadedConfig ->
                this.config = loadedConfig
                
                // Registro del listener en el planificador regional maestro
                Bukkit.getGlobalRegionScheduler().execute(plugin) {
                    val anvilListener = AnvilListener(config)
                    plugin.server.pluginManager.registerEvents(anvilListener, plugin)
                    listener = anvilListener
                }
            }
    }
}
