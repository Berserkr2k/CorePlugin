package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager

class HatCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val messageService: MessageService
) {

    init {
        manager.command(
            manager.commandBuilder("hat")
                .permission("core.utility.hat")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        messageService.send(sender, UtilityMessages.ONLY_PLAYERS)
                        return@handler
                    }

                    val inventory = sender.inventory
                    val handItem = inventory.itemInMainHand

                    // Comprobar que el item en la mano no sea nulo o aire
                    if (handItem.type == Material.AIR) {
                        messageService.send(sender, UtilityMessages.HAT_EMPTY)
                        return@handler
                    }

                    // Intercambio atómico
                    val currentHelmet = inventory.helmet
                    inventory.helmet = handItem.clone()
                    
                    if (currentHelmet == null || currentHelmet.type == Material.AIR) {
                        inventory.setItemInMainHand(null)
                    } else {
                        inventory.setItemInMainHand(currentHelmet.clone())
                    }

                    messageService.send(sender, UtilityMessages.HAT_EQUIPPED)
                }
        )
    }
}
