package com.github.berserkr2k.coreplugin.infrastructure.hologram

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureDescriptor
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.placeholder.PlaceholderService
import com.github.berserkr2k.coreplugin.api.framework.hologram.HologramService as APIHologramService

class HologramFeature : Feature {
    override val descriptor = FeatureDescriptor(
        id = "holograms",
        provides = setOf(com.github.berserkr2k.coreplugin.api.framework.hologram.HologramService::class.java)
    )

    private var hologramService: HologramService? = null

    override fun registerServices(registry: ServiceRegistry) {
        val plugin = registry.get(org.bukkit.plugin.Plugin::class.java)
        val placeholderService = registry.get(PlaceholderService::class.java)

        val service = HologramService(
            plugin,
            placeholderService,
            registry
        )
        this.hologramService = service
        registry.register(APIHologramService::class.java, service)
    }

    override fun onEnable(context: FeatureContext) {
        context.messageService.registerFeature("holograms", HologramMessages.defaults)

        val service = hologramService ?: context.getService(APIHologramService::class.java) as HologramService

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
