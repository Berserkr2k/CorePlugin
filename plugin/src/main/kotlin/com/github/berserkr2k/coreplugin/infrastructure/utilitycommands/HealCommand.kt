package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import org.bukkit.attribute.Attribute
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.bukkit.parser.PlayerParser.playerParser
import net.kyori.adventure.text.minimessage.MiniMessage

class HealCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val messagesConfig: MessagesConfig
) {
    private val miniMessage = MiniMessage.miniMessage()

    init {
        manager.command(
            manager.commandBuilder("heal")
                .optional("target", playerParser())
                .permission("core.utility.heal")
                .handler { context ->
                    val sender = context.sender()
                    val targetOpt = context.optional<Player>("target")

                    if (targetOpt.isPresent) {
                        // Curar a otro jugador
                        if (!sender.hasPermission("core.utility.heal.others")) {
                            val msg = messagesConfig.utility["no-permission-other"] ?: "<red>No tienes permiso para aplicar esto a otros jugadores.</red>"
                            sender.sendMessage(miniMessage.deserialize(msg))
                            return@handler
                        }
                        val target = targetOpt.get()
                        healPlayer(sender, target, true)
                    } else {
                        // Curarse a sí mismo
                        if (sender !is Player) {
                            val msg = messagesConfig.utility["only-players"] ?: "<red>Solo jugadores pueden ejecutar este comando.</red>"
                            sender.sendMessage(miniMessage.deserialize(msg))
                            return@handler
                        }
                        healPlayer(sender, sender, false)
                    }
                }
        )
    }

    private fun healPlayer(sender: CommandSender, target: Player, isOther: Boolean) {
        // Restaurar salud al máximo disponible
        val maxHealthAttr = target.getAttribute(Attribute.MAX_HEALTH)
        val maxHealth = maxHealthAttr?.value ?: maxHealthAttr?.defaultValue ?: 20.0
        target.health = maxHealth

        // Limpiar fuego
        target.fireTicks = 0

        // Limpiar efectos de pociones activos (purificación)
        target.activePotionEffects.forEach { effect ->
            target.removePotionEffect(effect.type)
        }

        // También saciar el apetito como comodidad en el comando de curación completo
        target.foodLevel = 20
        target.saturation = 20.0f

        if (isOther) {
            val senderKey = "heal-success-other"
            val senderDefault = "<green>¡Has curado y purificado a <player>!</green>"
            val senderMsg = (messagesConfig.utility[senderKey] ?: senderDefault).replace("<player>", target.name)
            sender.sendMessage(miniMessage.deserialize(senderMsg))

            val targetKey = "heal-success-by-admin"
            val targetDefault = "<green>¡Un administrador te ha curado y purificado!</green>"
            val targetMsg = messagesConfig.utility[targetKey] ?: targetDefault
            target.sendMessage(miniMessage.deserialize(targetMsg))
        } else {
            val key = "heal-success"
            val defaultMsg = "<green>¡Has sido curado y purificado!</green>"
            val msg = messagesConfig.utility[key] ?: defaultMsg
            target.sendMessage(miniMessage.deserialize(msg))
        }
    }
}
