package com.github.berserkr2k.coreplugin.infrastructure.hologram

import org.bukkit.Location
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
        // 1. /core hologram create <id> <text>
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
                    val lines = rawText.split("|").map { it.trim() }

                    hologramService.createHologram(id, sender.location, lines)
                    sender.sendMessage(miniMessage.deserialize("<green>¡Holograma '$id' creado con éxito en tu posición!</green>"))
                }
        )

        // 2. /core hologram delete <id>
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

        // 3. /core hologram list
        manager.command(
            manager.commandBuilder("core")
                .literal("hologram")
                .literal("list")
                .permission("core.hologram.admin")
                .handler { context ->
                    val sender = context.sender()
                    val holos = hologramService.getActiveHolograms()
                    if (holos.isEmpty()) {
                        sender.sendMessage(miniMessage.deserialize("<yellow>No hay hologramas activos en el servidor.</yellow>"))
                        return@handler
                    }
                    sender.sendMessage(miniMessage.deserialize("<gold><bold>Hologramas Activos (${holos.size}):</bold></gold>"))
                    holos.forEach { (id, holo) ->
                        val loc = holo.location
                        sender.sendMessage(miniMessage.deserialize(" <gray>-</gray> <yellow>$id</yellow> <gray>(Mundo: ${loc.world.name}, X: ${String.format("%.2f", loc.x)}, Y: ${String.format("%.2f", loc.y)}, Z: ${String.format("%.2f", loc.z)})</gray>"))
                    }
                }
        )

        // 4. /core hologram edit <id> <text>
        manager.command(
            manager.commandBuilder("core")
                .literal("hologram")
                .literal("edit")
                .required("id", stringParser())
                .required("text", greedyStringParser())
                .permission("core.hologram.admin")
                .handler { context ->
                    val sender = context.sender()
                    val id = context.get<String>("id")
                    val rawText = context.get<String>("text")
                    val lines = rawText.split("|").map { it.trim() }

                    if (hologramService.editHologram(id, lines)) {
                        sender.sendMessage(miniMessage.deserialize("<green>¡Holograma '$id' editado con éxito!</green>"))
                    } else {
                        sender.sendMessage(miniMessage.deserialize("<red>No se encontró ningún holograma activo con el ID '$id'.</red>"))
                    }
                }
        )

        // 5. /core hologram move <id>
        manager.command(
            manager.commandBuilder("core")
                .literal("hologram")
                .literal("move")
                .required("id", stringParser())
                .permission("core.hologram.admin")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        sender.sendMessage(miniMessage.deserialize("<red>Solo jugadores pueden ejecutar este comando.</red>"))
                        return@handler
                    }
                    val id = context.get<String>("id")
                    if (hologramService.moveHologram(id, sender.location)) {
                        sender.sendMessage(miniMessage.deserialize("<green>¡Holograma '$id' trasladado a tu posición actual!</green>"))
                    } else {
                        sender.sendMessage(miniMessage.deserialize("<red>No se encontró ningún holograma activo con el ID '$id'.</red>"))
                    }
                }
        )

        // 6. /core hologram center <id>
        manager.command(
            manager.commandBuilder("core")
                .literal("hologram")
                .literal("center")
                .required("id", stringParser())
                .permission("core.hologram.admin")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        sender.sendMessage(miniMessage.deserialize("<red>Solo jugadores pueden ejecutar este comando.</red>"))
                        return@handler
                    }
                    val id = context.get<String>("id")
                    
                    val holos = hologramService.getActiveHolograms()
                    val holo = holos[id]
                    if (holo == null) {
                        sender.sendMessage(miniMessage.deserialize("<red>No se encontró ningún holograma activo con el ID '$id'.</red>"))
                        return@handler
                    }

                    // Centrar coordenadas X y Z al centro del bloque
                    val currentLoc = holo.location.clone()
                    val centeredLoc = Location(
                        currentLoc.world,
                        currentLoc.blockX + 0.5,
                        currentLoc.y,
                        currentLoc.blockZ + 0.5,
                        currentLoc.yaw,
                        currentLoc.pitch
                    )

                    if (hologramService.moveHologram(id, centeredLoc)) {
                        sender.sendMessage(miniMessage.deserialize("<green>¡Holograma '$id' centrado en el bloque (X: ${centeredLoc.x}, Z: ${centeredLoc.z})!</green>"))
                    } else {
                        sender.sendMessage(miniMessage.deserialize("<red>Error al intentar trasladar el holograma '$id'.</red>"))
                    }
                }
        )
    }
}
