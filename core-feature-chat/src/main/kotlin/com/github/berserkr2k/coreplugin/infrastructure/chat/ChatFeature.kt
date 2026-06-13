package com.github.berserkr2k.coreplugin.infrastructure.chat

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.core.user.ProfileRegistry
import com.github.berserkr2k.coreplugin.api.core.placeholder.PlaceholderService
import com.github.berserkr2k.coreplugin.api.core.lifecycle.ReloadCoordinator

class ChatFeature : Feature {
    override val id = "chat"
    override val requiresDatabase = true // ⚡ El Kernel validará esto automáticamente

    private var chatModule: ChatModule? = null

    override fun onEnable(context: FeatureContext) {
        context.messageService.registerFeature("chat", ChatMessages.defaults)

        val config = context.configService.getConfig("chat")
        val profileRegistry = context.getService(ProfileRegistry::class.java)
        val placeholderService = context.getService(PlaceholderService::class.java)

        // Inicialización purificada del módulo sin pasarle el ServiceRegistry crudo
        val service = ChatModule(
            context._plugin,
            config,
            placeholderService,
            profileRegistry,
            context.messageService,
            context.taskScheduler,
            context.getService(com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService::class.java)
        )
        this.chatModule = service

        // Autoregistro autónomo de sub-comandos del chat usando CommandService
        val commandService = context.getService(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java)
        PrivateMessageCommand(context._plugin, commandService.manager, profileRegistry, context.messageService)
        ColorCommand(context._plugin, commandService.manager, profileRegistry, context.configService, context.messageService, context.registry)

        // Registrar en el coordinador de recargas si implementa Reloadable
        val reloadCoordinator = context.getOptionalService(ReloadCoordinator::class.java)
        reloadCoordinator?.register("chat", service)
    }
}
