package com.github.berserkr2k.coreplugin.infrastructure.chat

import com.github.berserkr2k.coreplugin.common.LegacyPlaceholderBridge
import com.github.berserkr2k.coreplugin.domain.chat.ChatConfig
import com.github.berserkr2k.coreplugin.api.core.config.FeatureConfig
import com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService
import org.bukkit.plugin.Plugin

class ChatModule(
    private val plugin: Plugin,
    private val featureConfig: FeatureConfig,
    private val papiBridge: LegacyPlaceholderBridge,
    private val profileRegistry: com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry,
    private val messageService: MessageService,
    private val taskScheduler: TaskScheduler,
    private val stateService: PlayerStateService
) : com.github.berserkr2k.coreplugin.api.core.lifecycle.Reloadable {
    lateinit var config: ChatConfig
        private set
    private var listener: ModernChatModuleListener? = null

    private val mapperFactory = org.spongepowered.configurate.objectmapping.ObjectMapper.factoryBuilder()
        .defaultNamingScheme(org.spongepowered.configurate.util.NamingSchemes.PASSTHROUGH)
        .build()
    private val mapper = mapperFactory.get(ChatConfig::class.java)

    private fun getRootNode(): org.spongepowered.configurate.CommentedConfigurationNode {
        val field = featureConfig.javaClass.getDeclaredField("rootNode")
        field.isAccessible = true
        return field.get(featureConfig) as org.spongepowered.configurate.CommentedConfigurationNode
    }

    private fun loadConfig() {
        val rootNode = getRootNode()
        this.config = mapper.load(rootNode) ?: ChatConfig()
    }

    init {
        loadConfig()
        
        // Registro del listener en el planificador regional maestro
        taskScheduler.runSync {
            val chatListener = ModernChatModuleListener(config, papiBridge, profileRegistry, stateService, messageService)
            plugin.server.pluginManager.registerEvents(chatListener, plugin)
            listener = chatListener
        }
    }

    override suspend fun reload() {
        try {
            featureConfig.reload()
            loadConfig()
            listener?.chatConfig = this.config
        } catch (e: Exception) {
            plugin.logger.severe("Error al recargar chat: ${e.message}")
        }
    }
}
