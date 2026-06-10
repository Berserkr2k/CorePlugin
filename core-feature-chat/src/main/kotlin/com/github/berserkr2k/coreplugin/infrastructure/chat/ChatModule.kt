package com.github.berserkr2k.coreplugin.infrastructure.chat

import com.github.berserkr2k.coreplugin.common.LegacyPlaceholderBridge
import com.github.berserkr2k.coreplugin.domain.chat.ChatConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.scheduler.TaskScheduler

class ChatModule(
    private val plugin: Plugin,
    private val configManager: ModularConfigManager,
    private val papiBridge: LegacyPlaceholderBridge,
    private val profileRegistry: com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry,
    private val serviceRegistry: ServiceRegistry
) {
    lateinit var config: ChatConfig
        private set
    private var listener: ModernChatModuleListener? = null
    private val taskScheduler = serviceRegistry.get(TaskScheduler::class.java)

    init {
        configManager.loadModuleConfig("chat.conf", ChatConfig::class.java, ChatConfig())
            .thenAccept { loadedConfig ->
                this.config = loadedConfig
                
                // Registro del listener en el planificador regional maestro
                taskScheduler.runSync {
                    val chatListener = ModernChatModuleListener(config, papiBridge, profileRegistry)
                    plugin.server.pluginManager.registerEvents(chatListener, plugin)
                    listener = chatListener
                }
            }
    }
}
