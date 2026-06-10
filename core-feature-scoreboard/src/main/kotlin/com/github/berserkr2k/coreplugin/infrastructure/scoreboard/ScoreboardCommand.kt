package com.github.berserkr2k.coreplugin.infrastructure.scoreboard

import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.getScoreboard
import com.github.berserkr2k.coreplugin.common.ColorUtility
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager

class ScoreboardCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val scoreboardService: ScoreboardService,
    private val messagesConfig: MessagesConfig
) {

    init {
        registerSidebarCommands()
        registerAdminCommands()
    }

    private fun getMsg(key: String, vararg placeholders: Pair<String, Any>): String {
        return messagesConfig.getScoreboard(key, *placeholders)
    }

    private fun registerSidebarCommands() {
        val sidebarBuilder = manager.commandBuilder("sidebar")
            .permission("core.scoreboard.use")

        // 1. /sidebar -> toggles visibility
        manager.command(
            sidebarBuilder.handler { context ->
                val sender = context.sender()
                if (sender !is Player) {
                    sender.sendMessage(ColorUtility.parse(messagesConfig.utility["only-players"] ?: "<red>Solo jugadores pueden ejecutar este comando.</red>"))
                    return@handler
                }

                val state = scoreboardService.getScoreboardState(sender.uniqueId)
                state.visible = !state.visible
                
                if (state.visible) {
                    sender.sendMessage(ColorUtility.parse(getMsg("toggle-on")))
                } else {
                    sender.sendMessage(ColorUtility.parse(getMsg("toggle-off")))
                }
            }
        )

        // 2. /sidebar toggle -> toggles visibility (explicit sub-command)
        manager.command(
            sidebarBuilder.literal("toggle")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        sender.sendMessage(ColorUtility.parse(messagesConfig.utility["only-players"] ?: "<red>Solo jugadores pueden ejecutar este comando.</red>"))
                        return@handler
                    }

                    val state = scoreboardService.getScoreboardState(sender.uniqueId)
                    state.visible = !state.visible
                    
                    if (state.visible) {
                        sender.sendMessage(ColorUtility.parse(getMsg("toggle-on")))
                    } else {
                        sender.sendMessage(ColorUtility.parse(getMsg("toggle-off")))
                    }
                }
        )
    }

    private fun registerAdminCommands() {
        // /core scoreboard reload
        manager.command(
            manager.commandBuilder("core")
                .literal("scoreboard")
                .literal("reload")
                .permission("core.scoreboard.admin")
                .handler { context ->
                    val sender = context.sender()
                    scoreboardService.reloadConfig().thenRun {
                        sender.sendMessage(ColorUtility.parse(getMsg("reloaded")))
                    }
                }
        )
    }
}
