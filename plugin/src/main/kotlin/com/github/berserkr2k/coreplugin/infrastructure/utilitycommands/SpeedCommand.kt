package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.IntegerParser.integerParser
import org.incendo.cloud.bukkit.parser.PlayerParser.playerParser
import net.kyori.adventure.text.minimessage.MiniMessage

class SpeedCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val messagesConfig: MessagesConfig
) {
    private val miniMessage = MiniMessage.miniMessage()

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
                val msg = messagesConfig.utility["no-permission-other"] ?: "<red>No tienes permiso para aplicar esto a otros jugadores.</red>"
                sender.sendMessage(miniMessage.deserialize(msg))
                return
            }
            target
        } else {
            if (sender !is Player) {
                val msg = messagesConfig.utility["only-players"] ?: "<red>Solo jugadores pueden ejecutar este comando.</red>"
                sender.sendMessage(miniMessage.deserialize(msg))
                return
            }
            sender
        }

        // Restablecer a valores por defecto
        targetPlayer.flySpeed = 0.1f
        targetPlayer.walkSpeed = 0.2f

        val isSelf = sender == targetPlayer
        if (isSelf) {
            val key = "speed-reset"
            val defaultMsg = "<green>Tu velocidad de vuelo y caminata ha sido restablecida a los valores por defecto.</green>"
            val msg = messagesConfig.utility[key] ?: defaultMsg
            targetPlayer.sendMessage(miniMessage.deserialize(msg))
        } else {
            val key = "speed-reset-other"
            val defaultMsg = "<green>Velocidad de vuelo y caminata de <player> restablecida a los valores por defecto.</green>"
            val msg = (messagesConfig.utility[key] ?: defaultMsg).replace("<player>", targetPlayer.name)
            sender.sendMessage(miniMessage.deserialize(msg))
        }
    }

    private fun handleSpeedChange(sender: CommandSender, speed: Int, target: Player?, forceFly: Boolean?) {
        // Validar rango
        if (speed < 1 || speed > 10) {
            val msg = messagesConfig.utility["speed-invalid"] ?: "<red>Velocidad inválida. Debe ser un número entre 1 y 10.</red>"
            sender.sendMessage(miniMessage.deserialize(msg))
            return
        }

        val targetPlayer = if (target != null) {
            // Verificar permiso para otros
            if (!sender.hasPermission("core.utility.speed.others")) {
                val msg = messagesConfig.utility["no-permission-other"] ?: "<red>No tienes permiso para aplicar esto a otros jugadores.</red>"
                sender.sendMessage(miniMessage.deserialize(msg))
                return
            }
            target
        } else {
            if (sender !is Player) {
                val msg = messagesConfig.utility["only-players"] ?: "<red>Solo jugadores pueden ejecutar este comando.</red>"
                sender.sendMessage(miniMessage.deserialize(msg))
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
            val key = if (isFly) "speed-fly-set" else "speed-walk-set"
            val defaultMsg = if (isFly) "<green>Velocidad de vuelo establecida en <speed>.</green>" else "<green>Velocidad de caminata establecida en <speed>.</green>"
            val msg = (messagesConfig.utility[key] ?: defaultMsg).replace("<speed>", speed.toString())
            target.sendMessage(miniMessage.deserialize(msg))
        } else {
            val key = if (isFly) "speed-fly-set-other" else "speed-walk-set-other"
            val defaultMsg = if (isFly) "<green>Velocidad de vuelo de <player> establecida en <speed>.</green>" else "<green>Velocidad de caminata de <player> establecida en <speed>.</green>"
            val msg = (messagesConfig.utility[key] ?: defaultMsg)
                .replace("<speed>", speed.toString())
                .replace("<player>", target.name)
            sender.sendMessage(miniMessage.deserialize(msg))
        }
    }
}
