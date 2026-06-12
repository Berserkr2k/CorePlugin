package com.github.berserkr2k.coreplugin.api.core.lifecycle

class FeatureContext(
    val plugin: org.bukkit.plugin.java.JavaPlugin,
    val registry: com.github.berserkr2k.coreplugin.api.di.ServiceRegistry,
    val commandManager: org.incendo.cloud.paper.LegacyPaperCommandManager<org.bukkit.command.CommandSender>,
    val taskScheduler: com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler,
    val regionTaskScheduler: com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler,
    val messageService: com.github.berserkr2k.coreplugin.api.core.message.MessageService,
    val configService: com.github.berserkr2k.coreplugin.api.core.config.ConfigService,
    val databaseService: com.github.berserkr2k.coreplugin.api.core.database.DatabaseService?
) {
    fun <T : Any> getService(clazz: Class<T>): T = registry.get(clazz)
    fun <T : Any> getOptionalService(clazz: Class<T>): T? = registry.getOptional(clazz)
}
