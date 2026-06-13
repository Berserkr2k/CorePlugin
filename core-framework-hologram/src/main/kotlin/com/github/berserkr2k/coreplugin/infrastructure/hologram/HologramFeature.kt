package com.github.berserkr2k.coreplugin.infrastructure.hologram

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.core.placeholder.PlaceholderService
import com.github.berserkr2k.coreplugin.api.framework.hologram.HologramService as APIHologramService

class HologramFeature : Feature {
    override val id = "holograms"

    private var hologramService: HologramService? = null

    override fun onEnable(context: FeatureContext) {
        context.messageService.registerFeature("holograms", HologramMessages.defaults)

        val placeholderService = context.getService(PlaceholderService::class.java)

        val service = HologramService(
            context._plugin,
            placeholderService,
            context.registry
        )
        this.hologramService = service

        context.registry.register(APIHologramService::class.java, service)

        HologramCommand(
            context._plugin,
            context.getService(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java).manager,
            service,
            context.messageService
        )

        val reloadCoordinator = context.getOptionalService(com.github.berserkr2k.coreplugin.api.core.lifecycle.ReloadCoordinator::class.java)
        reloadCoordinator?.register("holograms", service)
    }

    override fun onDisable(context: FeatureContext) {
        hologramService?.shutdown()
    }
}
