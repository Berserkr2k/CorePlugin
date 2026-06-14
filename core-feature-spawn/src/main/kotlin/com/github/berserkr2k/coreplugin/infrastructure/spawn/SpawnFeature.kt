package com.github.berserkr2k.coreplugin.infrastructure.spawn

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureDescriptor
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.infrastructure.spawn.service.SpawnService
import com.github.berserkr2k.coreplugin.infrastructure.spawn.listener.SpawnListener
import com.github.berserkr2k.coreplugin.infrastructure.spawn.command.SpawnCommand

class SpawnFeature : Feature {
    override val descriptor = FeatureDescriptor(
        id = "spawn",
        provides = emptySet()
    )

    private var spawnService: SpawnService? = null

    override fun registerServices(registry: ServiceRegistry) {
        val plugin = registry.get(org.bukkit.plugin.Plugin::class.java)
        val configService = registry.get(com.github.berserkr2k.coreplugin.api.core.config.ConfigService::class.java)
        val taskScheduler = registry.get(com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler::class.java)
        val regionTaskScheduler = registry.get(com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler::class.java)
        val playerStateService = registry.get(com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService::class.java)
        val messageService = registry.get(com.github.berserkr2k.coreplugin.api.core.message.MessageService::class.java)

        val config = configService.getConfig("spawn")
        val service = SpawnService(
            plugin,
            config,
            taskScheduler,
            regionTaskScheduler,
            playerStateService,
            messageService
        )
        this.spawnService = service
    }

    override fun onEnable(context: FeatureContext) {
        context.messageService.registerFeature("spawn", SpawnMessages.defaults)

        val service = spawnService ?: throw IllegalStateException("SpawnService not initialized during registerServices")

        context.registerListener(SpawnListener(service, context.regionTaskScheduler))

        val commandService = context.getService(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java)
        SpawnCommand(commandService.manager, service, context.messageService)

        val reloadCoordinator = context.getOptionalService(com.github.berserkr2k.coreplugin.api.core.lifecycle.ReloadCoordinator::class.java)
        reloadCoordinator?.register("spawn", service)
    }
}
