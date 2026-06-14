package com.github.berserkr2k.coreplugin.infrastructure.scoreboard

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureDescriptor
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService

class ScoreboardFeature : Feature {
    override val descriptor = FeatureDescriptor(
        id = "scoreboard",
        provides = emptySet()
    )

    private var scoreboardService: ScoreboardService? = null

    override fun registerServices(registry: ServiceRegistry) {
        val plugin = registry.get(org.bukkit.plugin.Plugin::class.java)
        val configService = registry.get(com.github.berserkr2k.coreplugin.api.core.config.ConfigService::class.java)
        val taskScheduler = registry.get(com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler::class.java)
        val playerStateService = registry.get(PlayerStateService::class.java)
        val messageService = registry.get(com.github.berserkr2k.coreplugin.api.core.message.MessageService::class.java)

        val config = configService.getConfig("scoreboard")
        val service = ScoreboardService(
            plugin,
            config,
            taskScheduler,
            playerStateService,
            messageService
        )
        this.scoreboardService = service
    }

    override fun onEnable(context: FeatureContext) {
        context.messageService.registerFeature("scoreboard", ScoreboardMessages.defaults)

        val service = scoreboardService ?: throw IllegalStateException("ScoreboardService not initialized during registerServices")

        context.registerListener(service)

        val commandService = context.getService(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java)
        ScoreboardCommand(commandService.manager, service, context.messageService)

        val reloadCoordinator = context.getOptionalService(com.github.berserkr2k.coreplugin.api.core.lifecycle.ReloadCoordinator::class.java)
        reloadCoordinator?.register("scoreboard", service)
    }

    override fun onDisable(context: FeatureContext) {
        scoreboardService?.stopTasks()
    }
}
