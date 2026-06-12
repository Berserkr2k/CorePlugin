package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.bukkit.parser.PlayerParser.playerParser

class FeedCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val messageService: MessageService
) {

    init {
        manager.command(
            manager.commandBuilder("feed")
                .optional("target", playerParser())
                .permission("core.utility.feed")
                .handler { context ->
                    val sender = context.sender()
                    val targetOpt = context.optional<Player>("target")

                    if (targetOpt.isPresent) {
                        // Alimentar a otro jugador
                        if (!sender.hasPermission("core.utility.feed.others")) {
                            messageService.send(sender, UtilityMessages.NO_PERMISSION_OTHER)
                            return@handler
                        }
                        val target = targetOpt.get()
                        feedPlayer(sender, target, true)
                    } else {
                        // Alimentarse a sí mismo
                        if (sender !is Player) {
                            messageService.send(sender, UtilityMessages.ONLY_PLAYERS)
                            return@handler
                        }
                        feedPlayer(sender, sender, false)
                    }
                }
        )
    }

    private fun feedPlayer(sender: CommandSender, target: Player, isOther: Boolean) {
        target.foodLevel = 20
        target.saturation = 20.0f

        if (isOther) {
            messageService.send(sender, UtilityMessages.FEED_SUCCESS_OTHER, PlaceholderContext.of("player" to target.name))
            messageService.send(target, UtilityMessages.FEED_SUCCESS_BY_ADMIN)
        } else {
            messageService.send(target, UtilityMessages.FEED_SUCCESS)
        }
    }
}
