package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.bukkit.parser.PlayerParser.playerParser
import net.kyori.adventure.text.minimessage.MiniMessage

class FeedCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val messagesConfig: MessagesConfig
) {
    private val miniMessage = object {
        fun deserialize(text: String) = com.github.berserkr2k.coreplugin.common.ColorUtility.parse(text)
    }

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
                            val msg = messagesConfig.utility["no-permission-other"] ?: "<red>No tienes permiso para aplicar esto a otros jugadores.</red>"
                            sender.sendMessage(miniMessage.deserialize(msg))
                            return@handler
                        }
                        val target = targetOpt.get()
                        feedPlayer(sender, target, true)
                    } else {
                        // Alimentarse a sí mismo
                        if (sender !is Player) {
                            val msg = messagesConfig.utility["only-players"] ?: "<red>Solo jugadores pueden ejecutar este comando.</red>"
                            sender.sendMessage(miniMessage.deserialize(msg))
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
            val senderKey = "feed-success-other"
            val senderDefault = "<green>¡Has saciado el apetito de <player>!</green>"
            val senderMsg = (messagesConfig.utility[senderKey] ?: senderDefault).replace("<player>", target.name)
            sender.sendMessage(miniMessage.deserialize(senderMsg))

            val targetKey = "feed-success-by-admin"
            val targetDefault = "<green>¡Un administrador ha saciado tu apetito!</green>"
            val targetMsg = messagesConfig.utility[targetKey] ?: targetDefault
            target.sendMessage(miniMessage.deserialize(targetMsg))
        } else {
            val key = "feed-success"
            val defaultMsg = "<green>¡Tu apetito ha sido saciado!</green>"
            val msg = messagesConfig.utility[key] ?: defaultMsg
            target.sendMessage(miniMessage.deserialize(msg))
        }
    }
}
