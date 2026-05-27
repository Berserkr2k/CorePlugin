package com.github.berserkr2k.coreplugin.command.chat

import com.github.berserkr2k.coreplugin.CorePlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ReplyCommand(private val plugin: CorePlugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true

        if (args.isEmpty()) {
            sender.sendMessage("§cUso: /reply <mensaje>")
            return true
        }

        // Le pedimos al cerebro con quién habló este jugador por última vez
        val target = plugin.privateMessageManager.getReplyTarget(sender)

        if (target == null || !target.isOnline) {
            sender.sendMessage("§cNo tienes a nadie a quien responder o se ha desconectado.")
            return true
        }

        val message = args.joinToString(" ")
        plugin.privateMessageManager.sendMessage(sender, target, message)
        return true
    }
}