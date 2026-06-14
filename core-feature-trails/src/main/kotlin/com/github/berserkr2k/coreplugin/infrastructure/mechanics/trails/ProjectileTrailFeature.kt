package com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureDescriptor
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.feature.trails.ProjectileTrailService
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuService
import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilderFactory

class ProjectileTrailFeature : Feature {
    override val descriptor = FeatureDescriptor(
        id = "projectile-trails",
        dependencies = setOf("database"),
        provides = setOf(ProjectileTrailService::class.java)
    )

    private var trailManager: ProjectileTrailManager? = null

    override fun registerServices(registry: ServiceRegistry) {
        val plugin = registry.get(org.bukkit.plugin.Plugin::class.java)
        val configService = registry.get(com.github.berserkr2k.coreplugin.api.core.config.ConfigService::class.java)
        val taskScheduler = registry.get(com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler::class.java)
        val messageService = registry.get(com.github.berserkr2k.coreplugin.api.core.message.MessageService::class.java)

        val config = configService.getConfig("trails")
        val manager = ProjectileTrailManager(
            plugin,
            config,
            taskScheduler,
            messageService
        )
        this.trailManager = manager
        registry.register(ProjectileTrailService::class.java, manager)
    }

    override fun onEnable(context: FeatureContext) {
        context.messageService.registerFeature("trails", TrailMessages.defaults)

        val manager = trailManager ?: context.getService(ProjectileTrailService::class.java) as ProjectileTrailManager

        val menuService = context.getService(MenuService::class.java)
        val itemFactory = context.getService(ItemBuilderFactory::class.java)

        context.registerListener(ProjectileTrailListener(context._plugin, manager, context.taskScheduler))

        val commandService = context.getService(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java)
        ProjectileTrailCommand(commandService.manager, manager, context.messageService, menuService, itemFactory)

        val reloadCoordinator = context.getOptionalService(com.github.berserkr2k.coreplugin.api.core.lifecycle.ReloadCoordinator::class.java)
        reloadCoordinator?.register("trails", manager)
    }

    override fun onDisable(context: FeatureContext) {
        // Safe lifecycle teardown
    }
}
