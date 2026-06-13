package com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.feature.trails.ProjectileTrailService
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuService
import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilderFactory

class ProjectileTrailFeature : Feature {
    override val id = "projectile-trails"

    private var trailManager: ProjectileTrailManager? = null

    override fun onEnable(context: FeatureContext) {
        context.messageService.registerFeature("trails", TrailMessages.defaults)

        val config = context.configService.getConfig("trails")
        val menuService = context.getService(MenuService::class.java)
        val itemFactory = context.getService(ItemBuilderFactory::class.java)

        // 1. Initialize the manager using abstract API components from the context
        val manager = ProjectileTrailManager(
            context._plugin,
            config,
            context.taskScheduler,
            context.messageService
        )
        this.trailManager = manager

        // 2. Register the public contract into the ServiceRegistry so other modules can consume it
        context.registry.register(ProjectileTrailService::class.java, manager)

        // 3. Register local events autonomously 
        context.registerListener(ProjectileTrailListener(context._plugin, manager, context.taskScheduler))

        // 4. Register commands using the CommandService abstraction (Cloud V2)
        val commandService = context.getService(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java)
        ProjectileTrailCommand(commandService.manager, manager, context.messageService, menuService, itemFactory)

        // 5. Register into the reload coordinator if it is present
        val reloadCoordinator = context.getOptionalService(com.github.berserkr2k.coreplugin.api.core.lifecycle.ReloadCoordinator::class.java)
        reloadCoordinator?.register("trails", manager)
    }

    override fun onDisable(context: FeatureContext) {
        // Clean up or disable active particle tasks if necessary to avoid memory leaks
    }
}
