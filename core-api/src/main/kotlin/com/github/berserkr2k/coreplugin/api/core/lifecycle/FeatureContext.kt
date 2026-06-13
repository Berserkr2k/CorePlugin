package com.github.berserkr2k.coreplugin.api.core.lifecycle

import com.github.berserkr2k.coreplugin.api.platform.PluginHandle
import org.bukkit.event.Listener

class FeatureContext(
    val platform: PluginHandle,
    val registry: com.github.berserkr2k.coreplugin.api.di.ServiceRegistry,
    val taskScheduler: com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler,
    val regionTaskScheduler: com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler,
    val messageService: com.github.berserkr2k.coreplugin.api.core.message.MessageService,
    val configService: com.github.berserkr2k.coreplugin.api.core.config.ConfigService,
    val databaseService: com.github.berserkr2k.coreplugin.api.core.database.DatabaseService?,
    // Internal Paper plugin reference — kept for listener registration only.
    // Features must NOT store or expose this reference; use platform for everything else.
    val _plugin: org.bukkit.plugin.Plugin
) {
    fun <T : Any> getService(clazz: Class<T>): T = registry.get(clazz)
    fun <T : Any> getOptionalService(clazz: Class<T>): T? = registry.getOptional(clazz)

    /**
     * Registers a Bukkit [Listener] with the server's plugin manager.
     * Features must call this instead of accessing [server.pluginManager] directly.
     */
    fun registerListener(listener: Listener) {
        _plugin.server.pluginManager.registerEvents(listener, _plugin)
    }
}

