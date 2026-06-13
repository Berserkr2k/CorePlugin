package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.feature.leaderboard.LeaderboardService as APILeaderboardService
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuService
import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilderFactory

class LeaderboardFeature : Feature {
    override val id = "leaderboard"
    override val dependencies = setOf("database")

    private var leaderboardService: LeaderboardService? = null

    override fun onEnable(context: FeatureContext) {
        context.messageService.registerFeature("leaderboard", LeaderboardMessages.defaults)

        val config = context.configService.getConfig("leaderboard")
        val menuService = context.getService(MenuService::class.java)
        val itemFactory = context.getService(ItemBuilderFactory::class.java)

        // 1. Initialize the concrete leaderboard service
        val service = LeaderboardService(
            context._plugin,
            config,
            context.taskScheduler,
            context.getService(com.github.berserkr2k.coreplugin.api.core.database.DatabaseService::class.java)
        )
        this.leaderboardService = service

        // 2. Expose the contract into the global ServiceRegistry
        context.registry.register(APILeaderboardService::class.java, service)

        // 3. Initialize the static editor GUI configuration loading
        ArmorStandEditorGui.init(context._plugin, menuService)

        // 4. Register local editor listeners autonomously
        context.registerListener(ArmorStandEditorListener(service, menuService, itemFactory))

        // 5. Register commands using the framework's CommandService (Cloud V2)
        val commandService = context.getService(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java)
        LeaderboardCommand(commandService.manager, service, context.messageService)
        ArmorStandEditorCommand(commandService.manager, service, context.messageService, menuService, itemFactory)

        // 6. Register into the global reload coordinator if present
        val reloadCoordinator = context.getOptionalService(com.github.berserkr2k.coreplugin.api.core.lifecycle.ReloadCoordinator::class.java)
        reloadCoordinator?.register("leaderboard", service)
    }

    override fun onDisable(context: FeatureContext) {
        // Safe teardown: Save cache configurations or clean up volatile tracking models if necessary
        leaderboardService?.shutdown()
    }
}
