package com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureDescriptor
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.framework.economy.EconomyService
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuService
import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilderFactory
import com.github.berserkr2k.coreplugin.api.framework.command.CommandService

class ShopFeature : Feature {
    override val descriptor = FeatureDescriptor(
        id = "shop",
        dependencies = setOf("database", "economy"),
        provides = emptySet()
    )

    private var shopManager: ShopManager? = null

    override fun registerServices(registry: ServiceRegistry) {
        val plugin = registry.get(org.bukkit.plugin.Plugin::class.java)
        val configService = registry.get(com.github.berserkr2k.coreplugin.api.core.config.ConfigService::class.java)
        val economyService = registry.get(EconomyService::class.java)
        val messageService = registry.get(com.github.berserkr2k.coreplugin.api.core.message.MessageService::class.java)

        val config = configService.getConfig("shop")
        val manager = ShopManager(
            plugin,
            config,
            economyService,
            messageService
        )
        this.shopManager = manager
    }

    override fun onEnable(context: FeatureContext) {
        context.messageService.registerFeature("shop", ShopMessages.defaults)

        val manager = shopManager ?: throw IllegalStateException("ShopManager not initialized during registerServices")

        val menuService = context.getService(MenuService::class.java)
        val itemFactory = context.getService(ItemBuilderFactory::class.java)
        val commandService = context.getService(CommandService::class.java)

        context.registerListener(manager)
        ShopCommand(commandService.manager, manager, context.messageService, menuService, itemFactory)

        val reloadCoordinator = context.getOptionalService(com.github.berserkr2k.coreplugin.api.core.lifecycle.ReloadCoordinator::class.java)
        reloadCoordinator?.register("shop", manager)
    }

    override fun registerValidators(registry: com.github.berserkr2k.coreplugin.api.core.validation.ValidationRegistry) {
        registry.register(ShopConfigValidator())
        registry.register(MarketConfigValidator())
    }

    override fun onDisable(context: FeatureContext) {
        // Safe lifecycle teardown
    }
}

class ShopConfigValidator : com.github.berserkr2k.coreplugin.api.core.validation.ConfigValidator<ShopConfig> {
    override fun validate(config: ShopConfig): List<String> {
        val errors = mutableListOf<String>()
        if (config.shopId.isBlank()) {
            errors.add("shopId cannot be empty or blank")
        }
        if (config.guiSize % 9 != 0 || config.guiSize !in 9..54) {
            errors.add("guiSize must be a multiple of 9 between 9 and 54 (got ${config.guiSize})")
        }
        for (item in config.items) {
            val mat = org.bukkit.Material.matchMaterial(item.material)
            if (mat == null) {
                errors.add("Invalid material '${item.material}' for shop item in category '${config.shopId}'")
            }
            val price = try {
                java.math.BigDecimal(item.basePrice)
            } catch (e: Exception) {
                null
            }
            if (price == null || price < java.math.BigDecimal.ZERO) {
                errors.add("Invalid basePrice '${item.basePrice}' for shop item '${item.material}'")
            }
        }
        return errors
    }
}

class MarketConfigValidator : com.github.berserkr2k.coreplugin.api.core.validation.ConfigValidator<MarketConfig> {
    override fun validate(config: MarketConfig): List<String> {
        val errors = mutableListOf<String>()
        if (config.taxRate < 0.0 || config.taxRate > 1.0) {
            errors.add("taxRate must be between 0.0 and 1.0 (got ${config.taxRate})")
        }
        if (config.purgeIntervalMinutes <= 0) {
            errors.add("purgeIntervalMinutes must be positive (got ${config.purgeIntervalMinutes})")
        }
        if (config.historyWindowHours <= 0) {
            errors.add("historyWindowHours must be positive (got ${config.historyWindowHours})")
        }
        return errors
    }
}
