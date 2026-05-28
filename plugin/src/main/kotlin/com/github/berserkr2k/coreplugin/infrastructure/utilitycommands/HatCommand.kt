package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import net.kyori.adventure.text.minimessage.MiniMessage

class HatCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val messagesConfig: MessagesConfig
) {
    private val miniMessage = object {
        fun deserialize(text: String) = com.github.berserkr2k.coreplugin.common.ColorUtility.parse(text)
    }

    init {
        manager.command(
            manager.commandBuilder("hat")
                .permission("core.utility.hat")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        val msg = messagesConfig.utility["only-players"] ?: "<red>Solo jugadores pueden ejecutar este comando.</red>"
                        sender.sendMessage(miniMessage.deserialize(msg))
                        return@handler
                    }

                    val inventory = sender.inventory
                    val handItem = inventory.itemInMainHand

                    // Comprobar que el item en la mano no sea nulo o aire
                    if (handItem.type == Material.AIR) {
                        val msg = messagesConfig.utility["hat-empty"] ?: "<red>Debes tener un ítem en la mano para usarlo de sombrero.</red>"
                        sender.sendMessage(miniMessage.deserialize(msg))
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

                    val msg = messagesConfig.utility["hat-equipped"] ?: "<green>¡Te has puesto el ítem en la cabeza!</green>"
                    sender.sendMessage(miniMessage.deserialize(msg))
                }
        )
    }
}
