package com.github.berserkr2k.coreplugin.infrastructure.mechanics

import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler

class AnvilModule(
    private val plugin: Plugin,
    private val configManager: ModularConfigManager,
    private val serviceRegistry: ServiceRegistry
) : com.github.berserkr2k.coreplugin.api.core.lifecycle.Reloadable {
    lateinit var config: AnvilConfig
        private set
    private var listener: AnvilListener? = null
    private val taskScheduler = serviceRegistry.get(TaskScheduler::class.java)

    init {
        configManager.loadModuleConfig("core/anvil.conf", AnvilConfig::class.java, AnvilConfig())
            .thenAccept { loadedConfig ->
                this.config = loadedConfig
                
                // Registro del listener en el planificador regional maestro
                taskScheduler.runSync {
                    val anvilListener = AnvilListener(config)
                    plugin.server.pluginManager.registerEvents(anvilListener, plugin)
                    listener = anvilListener
                }
            }
    }

    override suspend fun reload() {
        try {
            val loadedConfig = configManager.loadModuleConfig("core/anvil.conf", AnvilConfig::class.java, AnvilConfig()).join()
            this.config = loadedConfig
            listener?.config = loadedConfig
        } catch (e: Exception) {
            plugin.logger.severe("Error al recargar anvil.conf: ${e.message}")
        }
    }
}
