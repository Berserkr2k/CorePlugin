package com.github.berserkr2k.coreplugin.infrastructure.kits

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureDescriptor
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuService
import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilderFactory

class KitFeature : Feature {
    override val descriptor = FeatureDescriptor(
        id = "kits",
        dependencies = setOf("economy"),
        provides = setOf(com.github.berserkr2k.coreplugin.api.feature.kits.KitService::class.java)
    )

    private var kitService: KitService? = null

    override fun registerServices(registry: ServiceRegistry) {
        val plugin = registry.get(org.bukkit.plugin.Plugin::class.java)
        val configService = registry.get(com.github.berserkr2k.coreplugin.api.core.config.ConfigService::class.java)
        val messageService = registry.get(com.github.berserkr2k.coreplugin.api.core.message.MessageService::class.java)
        val taskScheduler = registry.get(com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler::class.java)
        val playerStateService = registry.get(PlayerStateService::class.java)

        val config = configService.getConfig("kits")
        val service = KitService(
            plugin,
            config,
            messageService,
            taskScheduler,
            playerStateService
        )
        this.kitService = service
        registry.register(com.github.berserkr2k.coreplugin.api.feature.kits.KitService::class.java, service)
    }

    override fun onEnable(context: FeatureContext) {
        context.messageService.registerFeature("kits", KitMessages.defaults)

        val service = kitService ?: context.getService(com.github.berserkr2k.coreplugin.api.feature.kits.KitService::class.java) as KitService

        context.registerListener(service)
        
        val menuService = context.getService(MenuService::class.java)
        val itemFactory = context.getService(ItemBuilderFactory::class.java)
        val commandService = context.getService(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java)
        KitCommand(context._plugin, commandService.manager, service, context.messageService, menuService, itemFactory)

        val reloadCoordinator = context.getOptionalService(com.github.berserkr2k.coreplugin.api.core.lifecycle.ReloadCoordinator::class.java)
        reloadCoordinator?.register("kits", service)
    }
}
