package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureDescriptor
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry

class UtilityFeature : Feature {
    override val descriptor = FeatureDescriptor(
        id = "utility-commands",
        provides = emptySet()
    )

    private var utilityService: UtilityService? = null

    override fun registerServices(registry: ServiceRegistry) {
        val plugin = registry.get(org.bukkit.plugin.Plugin::class.java)
        val configService = registry.get(com.github.berserkr2k.coreplugin.api.core.config.ConfigService::class.java)

        val config = configService.getConfig("utility")
        val service = UtilityService(plugin, config)
        this.utilityService = service
    }

    override fun onEnable(context: FeatureContext) {
        context.messageService.registerFeature("utility", UtilityMessages.defaults)

        val service = utilityService ?: throw IllegalStateException("UtilityService not initialized during registerServices")

        val commandService = context.getService(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java)
        val manager = commandService.manager

        AnvilCommand(context._plugin, manager, service, context.messageService)
        BroadcastCommand(context._plugin, manager, service, context.messageService)
        EnderChestCommand(context._plugin, manager, context.messageService)
        ExpCommand(context._plugin, manager, context.messageService)
        FeedCommand(context._plugin, manager, context.messageService)
        FlyCommand(context._plugin, manager, service, context.messageService)
        HatCommand(context._plugin, manager, context.messageService)
        HealCommand(context._plugin, manager, context.messageService)
        SendTitleCommand(context._plugin, manager, service, context.messageService)
        SpeedCommand(context._plugin, manager, context.messageService)

        val reloadCoordinator = context.getOptionalService(com.github.berserkr2k.coreplugin.api.core.lifecycle.ReloadCoordinator::class.java)
        reloadCoordinator?.register("utility", service)
    }
}
