package com.github.berserkr2k.coreplugin.command.chat

import com.github.berserkr2k.coreplugin.CorePlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class SocialSpyCommand(private val plugin: CorePlugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true

        if (!sender.hasPermission("coreplugin.admin.spy")) {
            sender.sendMessage("§cNo tienes permisos para usar esto.")
            return true
        }

        val isNowSpying = plugin.privateMessageManager.toggleSpy(sender)

        if (isNowSpying) {
            sender.sendMessage("§a¡SocialSpy ACTIVADO! Ahora puedes leer los mensajes privados.")
        } else {
            sender.sendMessage("§c¡SocialSpy DESACTIVADO! Ya no leerás mensajes privados.")
        }
        return true
    }
}