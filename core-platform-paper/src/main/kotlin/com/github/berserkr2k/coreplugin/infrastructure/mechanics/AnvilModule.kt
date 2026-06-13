package com.github.berserkr2k.coreplugin.infrastructure.mechanics

import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.util.NamingSchemes
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler

class AnvilModule(
    private val plugin: Plugin,
    private val serviceRegistry: ServiceRegistry
) : com.github.berserkr2k.coreplugin.api.core.lifecycle.Reloadable {
    lateinit var config: AnvilConfig
        private set
    private var listener: AnvilListener? = null
    private val taskScheduler = serviceRegistry.get(TaskScheduler::class.java)

    init {
        this.config = loadConfig()
        taskScheduler.runSync {
            val anvilListener = AnvilListener(config)
            plugin.server.pluginManager.registerEvents(anvilListener, plugin)
            listener = anvilListener
        }
    }

    private fun loadConfig(): AnvilConfig {
        val file = plugin.dataFolder.toPath().resolve("config/anvil.conf").toFile()
        val configService = serviceRegistry.get(com.github.berserkr2k.coreplugin.api.core.config.ConfigService::class.java)!!
        return configService.loadConfig(file, AnvilConfig::class.java, AnvilConfig())
    }

    override suspend fun reload() {
        try {
            val loadedConfig = loadConfig()
            this.config = loadedConfig
            listener?.config = loadedConfig
        } catch (e: Exception) {
            plugin.logger.severe("Error al recargar anvil.conf: ${e.message}")
        }
    }
}
