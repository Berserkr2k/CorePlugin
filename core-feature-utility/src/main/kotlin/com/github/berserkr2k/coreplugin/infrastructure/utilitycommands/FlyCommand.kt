package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.bukkit.parser.PlayerParser.playerParser

class FlyCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val utilityService: UtilityService,
    private val messageService: MessageService
) {

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
                            messageService.send(sender, UtilityMessages.NO_PERMISSION_OTHER)
                            return@handler
                        }
                        val target = targetOpt.get()
                        toggleFlight(sender, target, true)
                    } else {
                        // Cambiar propio vuelo
                        if (sender !is Player) {
                            messageService.send(sender, UtilityMessages.ONLY_PLAYERS)
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
                messageService.send(sender, UtilityMessages.FLY_WORLD_NOT_ALLOWED)
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
            val key = if (newState) UtilityMessages.FLY_ENABLED_OTHER else UtilityMessages.FLY_DISABLED_OTHER
            messageService.send(sender, key, PlaceholderContext.of("player" to target.name))
        } else {
            val key = if (newState) UtilityMessages.FLY_ENABLED else UtilityMessages.FLY_DISABLED
            messageService.send(target, key)
        }
    }
}
