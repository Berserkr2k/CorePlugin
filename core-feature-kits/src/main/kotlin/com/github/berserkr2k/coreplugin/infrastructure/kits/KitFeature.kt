package com.github.berserkr2k.coreplugin.infrastructure.kits

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuService
import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilderFactory

class KitFeature : Feature {
    override val id = "kits"

    private var kitService: KitService? = null

    override fun onEnable(context: FeatureContext) {
        context.messageService.registerFeature("kits", KitMessages.defaults)

        val config = context.configService.getConfig("kits")
        val menuService = context.getService(MenuService::class.java)
        val itemFactory = context.getService(ItemBuilderFactory::class.java)

        // Inicialización del servicio con dependencias puras desde el contexto
        val service = KitService(
            context._plugin,
            config,
            context.messageService,
            context.taskScheduler,
            context.getService(PlayerStateService::class.java)
        )
        this.kitService = service

        // Autoregistro autónomo de listeners y comandos
        context.registerListener(service)
        
        val commandService = context.getService(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java)
        KitCommand(context._plugin, commandService.manager, service, context.messageService, menuService, itemFactory)

        // Registro opcional en el coordinador de recargas
        val reloadCoordinator = context.getOptionalService(com.github.berserkr2k.coreplugin.api.core.lifecycle.ReloadCoordinator::class.java)
        reloadCoordinator?.register("kits", service)
    }
}
