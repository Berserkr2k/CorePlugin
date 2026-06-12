package com.github.berserkr2k.coreplugin.infrastructure.spawn

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.infrastructure.spawn.service.SpawnService
import com.github.berserkr2k.coreplugin.infrastructure.spawn.listener.SpawnListener
import com.github.berserkr2k.coreplugin.infrastructure.spawn.command.SpawnCommand

class SpawnFeature : Feature {
    override val id = "spawn"
    
    private var spawnService: SpawnService? = null

    override fun onEnable(context: FeatureContext) {
        val config = context.configService.getConfig("spawn")
        
        val service = SpawnService(
            context.plugin,
            config,
            context.taskScheduler,
            context.regionTaskScheduler,
            context.getService(com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService::class.java),
            context.messageService
        )
        this.spawnService = service

        // Autonomía: La feature registra sus propios listeners y comandos usando el contexto
        context.plugin.server.pluginManager.registerEvents(SpawnListener(service, context.regionTaskScheduler), context.plugin)
        
        val commandService = context.getService(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java)
        SpawnCommand(commandService.manager, service, context.messageService)

        val reloadCoordinator = context.getOptionalService(com.github.berserkr2k.coreplugin.infrastructure.lifecycle.ReloadCoordinator::class.java)
        reloadCoordinator?.register("spawn", service)
    }
}
