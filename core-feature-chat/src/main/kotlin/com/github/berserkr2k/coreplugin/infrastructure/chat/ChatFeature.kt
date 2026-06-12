package com.github.berserkr2k.coreplugin.infrastructure.chat

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry
import com.github.berserkr2k.coreplugin.common.LegacyPlaceholderBridge

class ChatFeature : Feature {
    override val id = "chat"
    override val requiresDatabase = true // ⚡ El Kernel validará esto automáticamente

    private var chatModule: ChatModule? = null

    override fun onEnable(context: FeatureContext) {
        val config = context.configService.getConfig("chat")
        val profileRegistry = context.getService(ProfileRegistry::class.java)
        
        // Extraemos el bridge de placeholders antiguo (temporal hasta su refactor)
        val placeholderBridge = context.getService(LegacyPlaceholderBridge::class.java)

        // Inicialización purificada del módulo sin pasarle el ServiceRegistry crudo
        val service = ChatModule(
            context.plugin,
            config,
            placeholderBridge,
            profileRegistry,
            context.messageService,
            context.taskScheduler,
            context.getService(com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService::class.java)
        )
        this.chatModule = service

        // Autoregistro autónomo de sub-comandos del chat usando CommandService
        val commandService = context.getService(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java)
        PrivateMessageCommand(context.plugin, commandService.manager, profileRegistry, context.messageService)
        ColorCommand(context.plugin, commandService.manager, profileRegistry, context.configService, context.messageService, context.registry)

        // Registrar en el coordinador de recargas si implementa Reloadable
        val reloadCoordinator = context.getOptionalService(com.github.berserkr2k.coreplugin.infrastructure.lifecycle.ReloadCoordinator::class.java)
        reloadCoordinator?.register("chat", service)
    }
}
