package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import org.bukkit.attribute.Attribute
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.bukkit.parser.PlayerParser.playerParser

class HealCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val messageService: MessageService
) {

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
                            messageService.send(sender, UtilityMessages.NO_PERMISSION_OTHER)
                            return@handler
                        }
                        val target = targetOpt.get()
                        healPlayer(sender, target, true)
                    } else {
                        // Curarse a sí mismo
                        if (sender !is Player) {
                            messageService.send(sender, UtilityMessages.ONLY_PLAYERS)
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
            messageService.send(sender, UtilityMessages.HEAL_SUCCESS_OTHER, PlaceholderContext.of("player" to target.name))
            messageService.send(target, UtilityMessages.HEAL_SUCCESS_BY_ADMIN)
        } else {
            messageService.send(target, UtilityMessages.HEAL_SUCCESS)
        }
    }
}
