package com.github.berserkr2k.coreplugin.infrastructure.hologram

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import net.kyori.adventure.text.minimessage.MiniMessage
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.parser.standard.StringParser.greedyStringParser

class HologramCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val hologramService: HologramService
) {
    private val miniMessage = MiniMessage.miniMessage()

    init {
        manager.command(
            manager.commandBuilder("core")
                .literal("hologram")
                .literal("create")
                .required("id", stringParser())
                .required("text", greedyStringParser())
                .permission("core.hologram.admin")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        sender.sendMessage(miniMessage.deserialize("<red>Solo jugadores pueden ejecutar este comando.</red>"))
                        return@handler
                    }
                    val id = context.get<String>("id")
                    val rawText = context.get<String>("text")
                    // Permite saltos de línea usando el carácter |
                    val lines = rawText.split("|").map { it.trim() }

                    hologramService.createHologram(id, sender.location, lines)
                    sender.sendMessage(miniMessage.deserialize("<green>¡Holograma '$id' creado con éxito en tu posición!</green>"))
                }
        )

        manager.command(
            manager.commandBuilder("core")
                .literal("hologram")
                .literal("delete")
                .required("id", stringParser())
                .permission("core.hologram.admin")
                .handler { context ->
                    val sender = context.sender()
                    val id = context.get<String>("id")
                    if (hologramService.deleteHologram(id)) {
                        sender.sendMessage(miniMessage.deserialize("<green>¡Holograma '$id' eliminado con éxito!</green>"))
                    } else {
                        sender.sendMessage(miniMessage.deserialize("<red>No se encontró ningún holograma activo con el ID '$id'.</red>"))
                    }
                }
        )
    }
}
