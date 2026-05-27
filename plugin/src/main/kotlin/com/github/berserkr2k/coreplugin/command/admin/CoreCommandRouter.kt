package com.github.berserkr2k.coreplugin.command.admin

import com.github.berserkr2k.coreplugin.command.SubCommand
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CoreCommandRouter : CommandExecutor {

    private val subCommands = mutableListOf<SubCommand>()

    // Función para registrar nuevos subcomandos fácilmente
    fun register(subCommand: SubCommand) {
        subCommands.add(subCommand)
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // Validación 1: Consola vs Jugador
        if (sender !is Player) {
            sender.sendMessage("Este comando es solo para jugadores.")
            return true
        }

        // Validación 2: Argumentos vacíos (ej. escribieron solo "/core")
        if (args.isEmpty()) {
            sender.sendMessage("§cUso correcto: /core <subcomando>")
            return true
        }

        // Buscamos el subcomando (ej. args[0] sería "testui")
        val targetCommand = subCommands.find { it.name.equals(args[0], ignoreCase = true) }

        if (targetCommand == null) {
            sender.sendMessage("§cSubcomando desconocido.")
            return true
        }

        // Validación 3: Permisos
        if (targetCommand.permission != null && !sender.hasPermission(targetCommand.permission!!)) {
            sender.sendMessage("§cNo tienes permisos para hacer esto.")
            return true
        }

        // ¡Todo en orden! Ejecutamos aislando el primer argumento
        targetCommand.execute(sender, args.drop(1).toTypedArray())

        return true
    }
}