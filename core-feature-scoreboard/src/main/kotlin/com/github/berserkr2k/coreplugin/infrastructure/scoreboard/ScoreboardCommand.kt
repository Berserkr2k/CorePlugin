package com.github.berserkr2k.coreplugin.infrastructure.scoreboard

import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.CoreMessages
import com.github.berserkr2k.coreplugin.common.ColorUtility
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.CommandManager

class ScoreboardCommand(
    private val manager: CommandManager<CommandSender>,
    private val scoreboardService: ScoreboardService,
    private val messageService: MessageService
) {

    init {
        registerSidebarCommands()
        registerAdminCommands()
    }

    private fun registerSidebarCommands() {
        val sidebarBuilder = manager.commandBuilder("sidebar")
            .permission("core.scoreboard.use")

        // 1. /sidebar -> toggles visibility
        manager.command(
            sidebarBuilder.handler { context ->
                val sender = context.sender()
                if (sender !is Player) {
                    messageService.send(sender, CoreMessages.ONLY_PLAYERS)
                    return@handler
                }

                val state = scoreboardService.getScoreboardState(sender.uniqueId)
                state.visible = !state.visible
                
                if (state.visible) {
                    messageService.send(sender, CoreMessages.SCOREBOARD_TOGGLE_ON)
                } else {
                    messageService.send(sender, CoreMessages.SCOREBOARD_TOGGLE_OFF)
                }
            }
        )

        // 2. /sidebar toggle -> toggles visibility (explicit sub-command)
        manager.command(
            sidebarBuilder.literal("toggle")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        messageService.send(sender, CoreMessages.ONLY_PLAYERS)
                        return@handler
                    }

                    val state = scoreboardService.getScoreboardState(sender.uniqueId)
                    state.visible = !state.visible
                    
                    if (state.visible) {
                        messageService.send(sender, CoreMessages.SCOREBOARD_TOGGLE_ON)
                    } else {
                        messageService.send(sender, CoreMessages.SCOREBOARD_TOGGLE_OFF)
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
                        messageService.send(sender, CoreMessages.SCOREBOARD_RELOADED)
                    }
                }
        )
    }
}
