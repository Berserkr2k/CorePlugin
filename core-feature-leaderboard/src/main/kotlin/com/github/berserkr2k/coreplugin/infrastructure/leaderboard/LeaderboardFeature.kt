package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureDescriptor
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.feature.leaderboard.LeaderboardService as APILeaderboardService
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuService
import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilderFactory

class LeaderboardFeature : Feature {
    override val descriptor = FeatureDescriptor(
        id = "leaderboard",
        dependencies = setOf("database"),
        provides = setOf(APILeaderboardService::class.java)
    )

    private var leaderboardService: LeaderboardService? = null

    override fun registerServices(registry: ServiceRegistry) {
        val plugin = registry.get(org.bukkit.plugin.Plugin::class.java)
        val configService = registry.get(com.github.berserkr2k.coreplugin.api.core.config.ConfigService::class.java)
        val taskScheduler = registry.get(com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler::class.java)
        val databaseService = registry.get(com.github.berserkr2k.coreplugin.api.core.database.DatabaseService::class.java)

        val config = configService.getConfig("leaderboard")
        val service = LeaderboardService(
            plugin,
            config,
            taskScheduler,
            databaseService
        )
        this.leaderboardService = service
        registry.register(APILeaderboardService::class.java, service)
    }

    override fun onEnable(context: FeatureContext) {
        context.messageService.registerFeature("leaderboard", LeaderboardMessages.defaults)

        val service = leaderboardService ?: context.getService(APILeaderboardService::class.java) as LeaderboardService

        val menuService = context.getService(MenuService::class.java)
        val itemFactory = context.getService(ItemBuilderFactory::class.java)

        ArmorStandEditorGui.init(context._plugin, menuService)
        context.registerListener(ArmorStandEditorListener(service, menuService, itemFactory))

        val commandService = context.getService(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java)
        LeaderboardCommand(commandService.manager, service, context.messageService)
        ArmorStandEditorCommand(commandService.manager, service, context.messageService, menuService, itemFactory)

        val reloadCoordinator = context.getOptionalService(com.github.berserkr2k.coreplugin.api.core.lifecycle.ReloadCoordinator::class.java)
        reloadCoordinator?.register("leaderboard", service)
    }

    override fun onDisable(context: FeatureContext) {
        leaderboardService?.shutdown()
    }
}
