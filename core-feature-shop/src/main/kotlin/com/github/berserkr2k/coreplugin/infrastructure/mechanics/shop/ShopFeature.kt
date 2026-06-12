package com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.feature.economy.EconomyService
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuService
import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilderFactory
import com.github.berserkr2k.coreplugin.api.framework.command.CommandService

class ShopFeature : Feature {
    override val id = "shop"
    override val requiresDatabase = true
    override val requiresEconomy = true // Enforces that Economy system must be loaded first

    private var shopManager: ShopManager? = null

    override fun onEnable(context: FeatureContext) {
        val config = context.configService.getConfig("shop")
        val economyService = context.getService(EconomyService::class.java)
        val menuService = context.getService(MenuService::class.java)
        val itemFactory = context.getService(ItemBuilderFactory::class.java)
        val commandService = context.getService(CommandService::class.java)

        // 1. Initialize ShopManager using only public API contracts
        val manager = ShopManager(
            context.plugin,
            config,
            economyService,
            context.messageService
        )
        this.shopManager = manager

        // 2. Register local events autonomously if any exist
        context.plugin.server.pluginManager.registerEvents(manager, context.plugin)

        // 3. Register shop commands cleanly using the Cloud V2 engine from CommandService
        ShopCommand(commandService.manager, manager, context.messageService, menuService, itemFactory)

        // 4. Register into the global reload coordinator if present
        val reloadCoordinator = context.getOptionalService(com.github.berserkr2k.coreplugin.api.core.lifecycle.ReloadCoordinator::class.java)
        reloadCoordinator?.register("shop", manager)
    }

    override fun onDisable(context: FeatureContext) {
        // Safe lifecycle teardown: Clear temporary maps or cache structures if necessary
    }
}
