package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.bukkit.parser.PlayerParser.playerParser
import net.kyori.adventure.text.minimessage.MiniMessage

class FlyCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val utilityService: UtilityService,
    private val messagesConfig: MessagesConfig
) {
    private val miniMessage = MiniMessage.miniMessage()

    init {
        manager.command(
            manager.commandBuilder("fly")
                .optional("target", playerParser())
                .permission("core.utility.fly")
                .handler { context ->
                    val sender = context.sender()
                    val targetOpt = context.optional<Player>("target")

                    if (targetOpt.isPresent) {
                        // Cambiar vuelo de otro jugador
                        if (!sender.hasPermission("core.utility.fly.others")) {
                            val msg = messagesConfig.utility["no-permission-other"] ?: "<red>No tienes permiso para aplicar esto a otros jugadores.</red>"
                            sender.sendMessage(miniMessage.deserialize(msg))
                            return@handler
                        }
                        val target = targetOpt.get()
                        toggleFlight(sender, target, true)
                    } else {
                        // Cambiar propio vuelo
                        if (sender !is Player) {
                            val msg = messagesConfig.utility["only-players"] ?: "<red>Solo jugadores pueden ejecutar este comando.</red>"
                            sender.sendMessage(miniMessage.deserialize(msg))
                            return@handler
                        }
                        toggleFlight(sender, sender, false)
                    }
                }
        )
    }

    private fun toggleFlight(sender: CommandSender, target: Player, isOther: Boolean) {
        val newState = !target.allowFlight
        if (newState) {
            // Verificar si el mundo actual permite vuelo
            if (!utilityService.isFlyAllowed(target)) {
                sender.sendMessage(miniMessage.deserialize(
                    messagesConfig.utility["fly-world-not-allowed"] ?: "<red>El vuelo no está permitido en este mundo.</red>"
                ))
                return
            }
            target.allowFlight = true
            target.isFlying = true
        } else {
            target.allowFlight = false
            target.isFlying = false
        }

        // Mensaje al emisor
        if (isOther) {
            val key = if (newState) "fly-enabled-other" else "fly-disabled-other"
            val defaultMsg = if (newState) "<green>Modo de vuelo habilitado para <player>.</green>" else "<red>Modo de vuelo deshabilitado para <player>.</red>"
            val msg = (messagesConfig.utility[key] ?: defaultMsg).replace("<player>", target.name)
            sender.sendMessage(miniMessage.deserialize(msg))
        } else {
            val key = if (newState) "fly-enabled" else "fly-disabled"
            val defaultMsg = if (newState) "<green>Modo de vuelo habilitado.</green>" else "<red>Modo de vuelo deshabilitado.</red>"
            val msg = messagesConfig.utility[key] ?: defaultMsg
            target.sendMessage(miniMessage.deserialize(msg))
        }
    }
}
