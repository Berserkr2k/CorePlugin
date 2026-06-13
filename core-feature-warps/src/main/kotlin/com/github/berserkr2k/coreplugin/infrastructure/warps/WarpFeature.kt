package com.github.berserkr2k.coreplugin.infrastructure.warps

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService

class WarpFeature : Feature {
    override val id = "warps"

    private var warpService: WarpService? = null

    override fun onEnable(context: FeatureContext) {
        context.messageService.registerFeature("warps", WarpMessages.defaults)

        val config = context.configService.getConfig("warps")
        val folderProvider = context.getService(com.github.berserkr2k.coreplugin.api.core.filesystem.FeatureFolderProvider::class.java)

        // Inicializamos el servicio pasando dependencias limpias extraídas del contexto
        val service = WarpService(
            context._plugin,
            config, // Pasamos FeatureConfig en lugar del Manager completo
            context.messageService,
            context.taskScheduler,
            context.regionTaskScheduler,
            context.getService(PlayerStateService::class.java),
            folderProvider
        )
        this.warpService = service

        context.registry.register(com.github.berserkr2k.coreplugin.api.framework.warp.WarpService::class.java, service)

        // Autoregister de Comandos y Eventos locales
        context.registerListener(service)
        
        val commandService = context.getService(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java)
        WarpCommand(context._plugin, commandService.manager, service, context.messageService, context.registry)

        // Registrar en el coordinador de retransmisión/recarga en caliente
        val reloadCoordinator = context.getOptionalService(com.github.berserkr2k.coreplugin.api.core.lifecycle.ReloadCoordinator::class.java)
        reloadCoordinator?.register("warps", service)
    }
}
