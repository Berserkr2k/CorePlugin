package com.github.berserkr2k.coreplugin.infrastructure.hologram

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import com.github.berserkr2k.coreplugin.common.LegacyPlaceholderBridge
import com.github.berserkr2k.coreplugin.api.feature.holograms.HologramService as APIHologramService

class HologramFeature : Feature {
    override val id = "holograms"

    private var hologramService: HologramService? = null

    override fun onEnable(context: FeatureContext) {
        val configManager = context.getService(ModularConfigManager::class.java)
        val placeholderBridge = context.getService(LegacyPlaceholderBridge::class.java)

        val service = HologramService(
            context.plugin,
            configManager,
            placeholderBridge,
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

        val reloadCoordinator = context.getOptionalService(com.github.berserkr2k.coreplugin.infrastructure.lifecycle.ReloadCoordinator::class.java)
        reloadCoordinator?.register("hologram", service)
    }

    override fun onDisable(context: FeatureContext) {
        hologramService?.shutdown()
    }
}
