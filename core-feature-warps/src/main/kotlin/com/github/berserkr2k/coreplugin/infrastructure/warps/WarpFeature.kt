package com.github.berserkr2k.coreplugin.infrastructure.warps

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureDescriptor
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService

class WarpFeature : Feature {
    override val descriptor = FeatureDescriptor(
        id = "warps",
        provides = setOf(com.github.berserkr2k.coreplugin.api.framework.warp.WarpService::class.java)
    )

    private var warpService: WarpService? = null

    override fun registerServices(registry: ServiceRegistry) {
        val plugin = registry.get(org.bukkit.plugin.Plugin::class.java)
        val configService = registry.get(com.github.berserkr2k.coreplugin.api.core.config.ConfigService::class.java)
        val folderProvider = registry.get(com.github.berserkr2k.coreplugin.api.core.filesystem.FeatureFolderProvider::class.java)
        val messageService = registry.get(com.github.berserkr2k.coreplugin.api.core.message.MessageService::class.java)
        val taskScheduler = registry.get(com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler::class.java)
        val regionTaskScheduler = registry.get(com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler::class.java)
        val playerStateService = registry.get(PlayerStateService::class.java)

        val config = configService.getConfig("warps")
        val service = WarpService(
            plugin,
            config,
            messageService,
            taskScheduler,
            regionTaskScheduler,
            playerStateService,
            folderProvider
        )
        this.warpService = service
        registry.register(com.github.berserkr2k.coreplugin.api.framework.warp.WarpService::class.java, service)
    }

    override fun onEnable(context: FeatureContext) {
        context.messageService.registerFeature("warps", WarpMessages.defaults)

        val service = warpService ?: context.getService(com.github.berserkr2k.coreplugin.api.framework.warp.WarpService::class.java) as WarpService

        context.registerListener(service)
        
        val commandService = context.getService(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java)
        WarpCommand(context._plugin, commandService.manager, service, context.messageService, context.registry)

        val reloadCoordinator = context.getOptionalService(com.github.berserkr2k.coreplugin.api.core.lifecycle.ReloadCoordinator::class.java)
        reloadCoordinator?.register("warps", service)
    }
}
