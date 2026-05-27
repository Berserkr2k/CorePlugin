package com.github.berserkr2k.coreplugin.command.chat

import com.github.berserkr2k.coreplugin.CorePlugin
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class MsgCommand(private val plugin: CorePlugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true

        if (args.size < 2) {
            sender.sendMessage("§cUso: /msg <jugador> <mensaje>")
            return true
        }

        val target = Bukkit.getPlayer(args[0])
        if (target == null || !target.isOnline) {
            sender.sendMessage("§cEse jugador no está en línea.")
            return true
        }

        if (target == sender) {
            sender.sendMessage("§cNo puedes enviarte mensajes a ti mismo.")
            return true
        }

        // Unimos todas las palabras del mensaje después del nombre del jugador
        val message = args.drop(1).joinToString(" ")
        plugin.privateMessageManager.sendMessage(sender, target, message)
        return true
    }
}