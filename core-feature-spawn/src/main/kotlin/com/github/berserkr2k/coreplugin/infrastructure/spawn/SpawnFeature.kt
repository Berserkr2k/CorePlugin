package com.github.berserkr2k.coreplugin.infrastructure.spawn

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.infrastructure.spawn.service.SpawnService
import com.github.berserkr2k.coreplugin.infrastructure.spawn.listener.SpawnListener
import com.github.berserkr2k.coreplugin.infrastructure.spawn.command.SpawnCommand
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager

class SpawnFeature : Feature {
    override val id = "spawn"
    
    private var spawnService: SpawnService? = null

    override fun onEnable(context: FeatureContext) {
        val configManager = context.getService(ModularConfigManager::class.java)
        
        val service = SpawnService(
            context.plugin,
            configManager,
            context.taskScheduler,
            context.regionTaskScheduler,
            context.getService(com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService::class.java),
            context.messageService
        )
        this.spawnService = service

        // Autonomía: La feature registra sus propios listeners y comandos usando el contexto
        context.plugin.server.pluginManager.registerEvents(SpawnListener(service, context.regionTaskScheduler), context.plugin)
        SpawnCommand(context.commandManager, service, context.messageService)

        val reloadCoordinator = context.getOptionalService(com.github.berserkr2k.coreplugin.infrastructure.lifecycle.ReloadCoordinator::class.java)
        reloadCoordinator?.register("spawn", service)
    }
}
