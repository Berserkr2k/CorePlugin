package com.github.berserkr2k.coreplugin.infrastructure.commands

import com.github.berserkr2k.coreplugin.infrastructure.lifecycle.ReloadCoordinator
import com.github.berserkr2k.coreplugin.infrastructure.message.FeatureMessageRegistry
import com.github.berserkr2k.coreplugin.api.core.config.ConfigService
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.CoreMessages
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class DebugCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val reloadCoordinator: ReloadCoordinator,
    private val messageRegistry: FeatureMessageRegistry,
    private val configService: ConfigService,
    private val messageService: MessageService
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    init {
        registerDebugCommands()
    }

    private fun registerDebugCommands() {
        val debugBuilder = manager.commandBuilder("core")
            .literal("debug")
            .permission("core.admin.debug")

        // 1. /core debug messages
        manager.command(
            debugBuilder.literal("messages")
                .handler { context ->
                    val sender = context.sender()
                    val features = messageRegistry.getRegisteredFeatures()
                    val totalCached = messageRegistry.getCachedComponentsCount()
                    
                    val sb = StringBuilder()
                    sb.append("<dark_gray>======================================================</dark_gray>\n")
                    sb.append("<yellow><bold>          MESSAGE REGISTRY DIAGNOSTICS</bold></yellow>\n")
                    sb.append("<dark_gray>======================================================</dark_gray>\n")
                    sb.append(" <gray>⚡ <gold>Total Features</gold>: ${features.size}</gray>\n")
                    sb.append(" <gray>⚡ <gold>Cached Components</gold>: $totalCached</gray>\n")
                    sb.append("<dark_gray>------------------------------------------------------</dark_gray>\n")
                    for (feature in features) {
                        val count = messageRegistry.getMessageCount(feature)
                        val padded = feature.padEnd(20, '.')
                        sb.append(" <gray>⚡ <gold>$padded</gold>: $count keys loaded</gray>\n")
                    }
                    sb.append("<dark_gray>======================================================</dark_gray>")
                    messageService.sendRaw(sender, sb.toString())
                }
        )

        // 2. /core debug configs
        manager.command(
            debugBuilder.literal("configs")
                .handler { context ->
                    val sender = context.sender()
                    val sb = StringBuilder()
                    sb.append("<dark_gray>======================================================</dark_gray>\n")
                    sb.append("<yellow><bold>          CONFIGURATION REGISTRY DIAGNOSTICS</bold></yellow>\n")
                    sb.append("<dark_gray>======================================================</dark_gray>\n")
                    val loadedConfigs = (configService as? com.github.berserkr2k.coreplugin.infrastructure.config.ConfigServiceImpl)?.getLoadedConfigs() ?: emptySet()
                    sb.append(" <gray>⚡ <gold>Total Active Configurations</gold>: ${loadedConfigs.size}</gray>\n")
                    sb.append("<dark_gray>------------------------------------------------------</dark_gray>\n")
                    for (cfg in loadedConfigs) {
                        sb.append(" <gray>⚡ <gold>Config File</gold>: $cfg</gray>\n")
                    }
                    sb.append("<dark_gray>======================================================</dark_gray>")
                    messageService.sendRaw(sender, sb.toString())
                }
        )

        // 3. /core debug reload
        manager.command(
            debugBuilder.literal("reload")
                .handler { context ->
                    val sender = context.sender()
                    messageService.sendRaw(sender, "<gold>⚙️ Iniciando recarga asíncrona de todos los módulos...</gold>")
                    
                    coroutineScope.launch {
                        try {
                            val metrics = reloadCoordinator.reloadAll()
                            val sb = StringBuilder()
                            sb.append("\n<dark_gray>======================================================</dark_gray>\n")
                            sb.append("<green><bold>          RELOAD PIPELINE DIAGNOSTICS</bold></green>\n")
                            sb.append("<dark_gray>======================================================</dark_gray>\n")
                            var totalTime = 0L
                            for ((module, duration) in metrics) {
                                val padded = module.padEnd(25, '.')
                                sb.append(" <gray>⚡ <gold>$padded</gold>: <green>${duration}ms</green></gray>\n")
                                totalTime += duration
                            }
                            sb.append("<dark_gray>------------------------------------------------------</dark_gray>\n")
                            sb.append(" <gray>⚡ <gold>Tiempo Total Transcurrido</gold>: <green>${totalTime}ms</green></gray>\n")
                            sb.append("<dark_gray>======================================================</dark_gray>")
                            messageService.sendRaw(sender, sb.toString())
                        } catch (e: Exception) {
                            messageService.sendRaw(sender, "<red>❌ Falló la recarga asíncrona: ${e.message}</red>")
                            plugin.logger.severe("Error durante debug reload: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
        )
    }
}
