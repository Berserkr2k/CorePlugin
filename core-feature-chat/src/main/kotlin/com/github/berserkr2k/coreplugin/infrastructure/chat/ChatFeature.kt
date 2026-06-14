package com.github.berserkr2k.coreplugin.infrastructure.chat

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureDescriptor
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.user.ProfileRegistry
import com.github.berserkr2k.coreplugin.api.core.placeholder.PlaceholderService
import com.github.berserkr2k.coreplugin.api.core.lifecycle.ReloadCoordinator

class ChatFeature : Feature {
    override val descriptor = FeatureDescriptor(
        id = "chat",
        dependencies = setOf("database"),
        provides = emptySet()
    )

    private var chatModule: ChatModule? = null

    override fun registerServices(registry: ServiceRegistry) {
        val plugin = registry.get(org.bukkit.plugin.Plugin::class.java)
        val configService = registry.get(com.github.berserkr2k.coreplugin.api.core.config.ConfigService::class.java)
        val profileRegistry = registry.get(ProfileRegistry::class.java)
        val placeholderService = registry.get(PlaceholderService::class.java)
        val messageService = registry.get(com.github.berserkr2k.coreplugin.api.core.message.MessageService::class.java)
        val taskScheduler = registry.get(com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler::class.java)
        val playerStateService = registry.get(com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService::class.java)

        val config = configService.getConfig("chat")
        val service = ChatModule(
            plugin,
            config,
            placeholderService,
            profileRegistry,
            messageService,
            taskScheduler,
            playerStateService
        )
        this.chatModule = service
    }

    override fun onEnable(context: FeatureContext) {
        context.messageService.registerFeature("chat", ChatMessages.defaults)

        val service = chatModule ?: throw IllegalStateException("ChatModule not initialized during registerServices")

        val profileRegistry = context.getService(ProfileRegistry::class.java)
        val commandService = context.getService(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java)
        PrivateMessageCommand(context._plugin, commandService.manager, profileRegistry, context.messageService)
        ColorCommand(context._plugin, commandService.manager, profileRegistry, context.configService, context.messageService, context.registry)

        val reloadCoordinator = context.getOptionalService(ReloadCoordinator::class.java)
        reloadCoordinator?.register("chat", service)
    }
}
