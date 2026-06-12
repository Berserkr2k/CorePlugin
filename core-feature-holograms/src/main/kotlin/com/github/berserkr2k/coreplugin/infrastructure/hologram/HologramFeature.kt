package com.github.berserkr2k.coreplugin.infrastructure.hologram

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.core.placeholder.PlaceholderService
import com.github.berserkr2k.coreplugin.api.feature.holograms.HologramService as APIHologramService

class HologramFeature : Feature {
    override val id = "holograms"

    private var hologramService: HologramService? = null

    override fun onEnable(context: FeatureContext) {
        val placeholderService = context.getService(PlaceholderService::class.java)

        val service = HologramService(
            context.plugin,
            placeholderService,
            context.registry
        )
        this.hologramService = service

        context.registry.register(APIHologramService::class.java, service)

        HologramCommand(
            context.plugin,
            context.commandManager,
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
