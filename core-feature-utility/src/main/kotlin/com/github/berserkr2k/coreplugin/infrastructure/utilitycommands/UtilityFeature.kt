package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext

class UtilityFeature : Feature {
    override val id = "utility-commands"

    private var utilityService: UtilityService? = null

    override fun onEnable(context: FeatureContext) {
        val config = context.configService.getConfig("utility")

        // 1. Initialize the Utility internal service
        val service = UtilityService(context.plugin, config)
        this.utilityService = service

        // 2. Fetch the abstract CommandService from the context (Cloud V2 Engine)
        val commandService = context.getService(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java)
        val manager = commandService.manager

        // 3. Register ALL utility sub-commands autonomously using the clean API manager
        AnvilCommand(context.plugin, manager, service, context.messageService)
        BroadcastCommand(context.plugin, manager, service, context.messageService)
        EnderChestCommand(context.plugin, manager, context.messageService)
        ExpCommand(context.plugin, manager, context.messageService)
        FeedCommand(context.plugin, manager, context.messageService)
        FlyCommand(context.plugin, manager, service, context.messageService)
        HatCommand(context.plugin, manager, context.messageService)
        HealCommand(context.plugin, manager, context.messageService)
        SendTitleCommand(context.plugin, manager, service, context.messageService)
        SpeedCommand(context.plugin, manager, context.messageService)

        // 4. Register into the reload coordinator if necessary
        val reloadCoordinator = context.getOptionalService(com.github.berserkr2k.coreplugin.infrastructure.lifecycle.ReloadCoordinator::class.java)
        reloadCoordinator?.register("utility", service)
    }
}
