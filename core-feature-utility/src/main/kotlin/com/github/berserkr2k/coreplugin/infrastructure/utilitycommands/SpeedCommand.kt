package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.IntegerParser.integerParser
import org.incendo.cloud.bukkit.parser.PlayerParser.playerParser

class SpeedCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val messageService: MessageService
) {

    init {
        // 1. /speed <1-10> [player]
        manager.command(
            manager.commandBuilder("speed")
                .required("speed", integerParser())
                .optional("target", playerParser())
                .permission("core.utility.speed")
                .handler { context ->
                    val sender = context.sender()
                    val speed = context.get<Int>("speed")
                    val targetOpt = context.optional<Player>("target")

                    handleSpeedChange(sender, speed, targetOpt.orElse(null), null)
                }
        )

        // 2. /speed fly <1-10> [player]
        manager.command(
            manager.commandBuilder("speed")
                .literal("fly")
                .required("speed", integerParser())
                .optional("target", playerParser())
                .permission("core.utility.speed")
                .handler { context ->
                    val sender = context.sender()
                    val speed = context.get<Int>("speed")
                    val targetOpt = context.optional<Player>("target")

                    handleSpeedChange(sender, speed, targetOpt.orElse(null), true)
                }
        )

        // 3. /speed walk <1-10> [player]
        manager.command(
            manager.commandBuilder("speed")
                .literal("walk")
                .required("speed", integerParser())
                .optional("target", playerParser())
                .permission("core.utility.speed")
                .handler { context ->
                    val sender = context.sender()
                    val speed = context.get<Int>("speed")
                    val targetOpt = context.optional<Player>("target")

                    handleSpeedChange(sender, speed, targetOpt.orElse(null), false)
                }
        )

        // 4. /speed reset [player]
        manager.command(
            manager.commandBuilder("speed")
                .literal("reset")
                .optional("target", playerParser())
                .permission("core.utility.speed")
                .handler { context ->
                    val sender = context.sender()
                    val targetOpt = context.optional<Player>("target")

                    handleSpeedReset(sender, targetOpt.orElse(null))
                }
        )
    }

    private fun handleSpeedReset(sender: CommandSender, target: Player?) {
        val targetPlayer = if (target != null) {
            // Verificar permiso para otros
            if (!sender.hasPermission("core.utility.speed.others")) {
                messageService.send(sender, UtilityMessages.NO_PERMISSION_OTHER)
                return
            }
            target
        } else {
            if (sender !is Player) {
                messageService.send(sender, UtilityMessages.ONLY_PLAYERS)
                return
            }
            sender
        }

        // Restablecer a valores por defecto
        targetPlayer.flySpeed = 0.1f
        targetPlayer.walkSpeed = 0.2f

        val isSelf = sender == targetPlayer
        if (isSelf) {
            messageService.send(targetPlayer, UtilityMessages.SPEED_RESET)
        } else {
            messageService.send(sender, UtilityMessages.SPEED_RESET_OTHER, PlaceholderContext.of("player" to targetPlayer.name))
        }
    }

    private fun handleSpeedChange(sender: CommandSender, speed: Int, target: Player?, forceFly: Boolean?) {
        // Validar rango
        if (speed < 1 || speed > 10) {
            messageService.send(sender, UtilityMessages.SPEED_INVALID)
            return
        }

        val targetPlayer = if (target != null) {
            // Verificar permiso para otros
            if (!sender.hasPermission("core.utility.speed.others")) {
                messageService.send(sender, UtilityMessages.NO_PERMISSION_OTHER)
                return
            }
            target
        } else {
            if (sender !is Player) {
                messageService.send(sender, UtilityMessages.ONLY_PLAYERS)
                return
            }
            sender
        }

        // Determinar si es fly o walk
        val isFly = forceFly ?: targetPlayer.isFlying

        // Convertir de 1-10 a float (-1.0f a 1.0f)
        val floatSpeed = speed / 10.0f

        if (isFly) {
            targetPlayer.flySpeed = floatSpeed
            sendFeedback(sender, targetPlayer, speed, true)
        } else {
            targetPlayer.walkSpeed = floatSpeed
            sendFeedback(sender, targetPlayer, speed, false)
        }
    }

    private fun sendFeedback(sender: CommandSender, target: Player, speed: Int, isFly: Boolean) {
        val isSelf = sender == target
        if (isSelf) {
            val key = if (isFly) UtilityMessages.SPEED_FLY_SET else UtilityMessages.SPEED_WALK_SET
            messageService.send(target, key, PlaceholderContext.of("speed" to speed.toString()))
        } else {
            val key = if (isFly) UtilityMessages.SPEED_FLY_SET_OTHER else UtilityMessages.SPEED_WALK_SET_OTHER
            messageService.send(sender, key, PlaceholderContext.of("speed" to speed.toString(), "player" to target.name))
        }
    }
}
