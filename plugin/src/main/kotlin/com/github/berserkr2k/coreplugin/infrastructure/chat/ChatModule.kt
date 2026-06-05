package com.github.berserkr2k.coreplugin.infrastructure.chat

import com.github.berserkr2k.coreplugin.common.LegacyPlaceholderBridge
import com.github.berserkr2k.coreplugin.domain.chat.ChatConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import com.github.berserkr2k.coreplugin.infrastructure.listeners.ModernChatModuleListener
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

class ChatModule(
    private val plugin: Plugin,
    private val configManager: ModularConfigManager,
    private val papiBridge: LegacyPlaceholderBridge,
    private val profileRegistry: com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry
) {
    lateinit var config: ChatConfig
        private set
    private var listener: ModernChatModuleListener? = null

    init {
        configManager.loadModuleConfig("chat.conf", ChatConfig::class.java, ChatConfig())
            .thenAccept { loadedConfig ->
                this.config = loadedConfig
                
                // Registro del listener en el planificador regional maestro
                Bukkit.getGlobalRegionScheduler().execute(plugin) {
                    val chatListener = ModernChatModuleListener(config, papiBridge, profileRegistry)
                    plugin.server.pluginManager.registerEvents(chatListener, plugin)
                    listener = chatListener
                }
            }
    }
}
