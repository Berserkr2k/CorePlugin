package com.github.berserkr2k.coreplugin.infrastructure.scoreboard

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService

class ScoreboardFeature : Feature {
    override val id = "scoreboard"

    private var scoreboardService: ScoreboardService? = null

    override fun onEnable(context: FeatureContext) {
        val config = context.configService.getConfig("scoreboard")
        val playerStateService = context.getService(PlayerStateService::class.java)

        // 1. Initialize the service utilizing only abstract API tools from the context
        val service = ScoreboardService(
            context.plugin,
            config,
            context.taskScheduler,
            playerStateService,
            context.messageService
        )
        this.scoreboardService = service

        // 2. Register local scoreboard events (e.g., PlayerJoin/Quit scoreboard setup) autonomously
        context.plugin.server.pluginManager.registerEvents(service, context.plugin)

        // 3. Register commands using the framework's CommandService (Cloud V2)
        val commandService = context.getService(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java)
        ScoreboardCommand(commandService.manager, service, context.messageService)

        // 4. Register into the global reload coordinator if it is present
        val reloadCoordinator = context.getOptionalService(com.github.berserkr2k.coreplugin.infrastructure.lifecycle.ReloadCoordinator::class.java)
        reloadCoordinator?.register("scoreboard", service)
    }

    override fun onDisable(context: FeatureContext) {
        // Safe lifecycle teardown: Stop active updater tasks or timers to prevent orphan execution threads
        scoreboardService?.stopTasks()
    }
}
