package com.github.berserkr2k.coreplugin.infrastructure.chat

import com.github.berserkr2k.coreplugin.common.LegacyPlaceholderBridge
import com.github.berserkr2k.coreplugin.domain.chat.ChatConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler
import com.github.berserkr2k.coreplugin.api.core.message.MessageService

class ChatModule(
    private val plugin: Plugin,
    private val configManager: ModularConfigManager,
    private val papiBridge: LegacyPlaceholderBridge,
    private val profileRegistry: com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry,
    private val serviceRegistry: ServiceRegistry,
    private val messageService: MessageService
) : com.github.berserkr2k.coreplugin.api.core.lifecycle.Reloadable {
    lateinit var config: ChatConfig
        private set
    private var listener: ModernChatModuleListener? = null
    private val taskScheduler = serviceRegistry.get(TaskScheduler::class.java)

    init {
        configManager.loadModuleConfig("core/chat.conf", ChatConfig::class.java, ChatConfig())
            .thenAccept { loadedConfig ->
                this.config = loadedConfig
                
                // Registro del listener en el planificador regional maestro
                taskScheduler.runSync {
                    val chatListener = ModernChatModuleListener(config, papiBridge, profileRegistry, serviceRegistry, messageService)
                    plugin.server.pluginManager.registerEvents(chatListener, plugin)
                    listener = chatListener
                }
            }
    }

    override suspend fun reload() {
        try {
            val loadedConfig = configManager.loadModuleConfig("core/chat.conf", ChatConfig::class.java, ChatConfig()).join()
            this.config = loadedConfig
            listener?.chatConfig = loadedConfig
        } catch (e: Exception) {
            plugin.logger.severe("Error al recargar chat.conf: ${e.message}")
        }
    }
}
