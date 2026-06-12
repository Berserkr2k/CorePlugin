package com.github.berserkr2k.coreplugin.infrastructure.hologram

import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.parser.standard.StringParser.greedyStringParser
import com.github.berserkr2k.coreplugin.api.feature.holograms.HologramService
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import com.github.berserkr2k.coreplugin.api.protection.permissions.Permissions

class HologramCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val hologramService: HologramService,
    private val messageService: MessageService
) {

    init {
        // 1. /core hologram create <id> <text>
        manager.command(
            manager.commandBuilder("core")
                .literal("hologram")
                .literal("create")
                .required("id", stringParser())
                .required("text", greedyStringParser())
                .permission(Permissions.HOLOGRAM_ADMIN)
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        messageService.send(sender, HologramMessages.ONLY_PLAYERS)
                        return@handler
                    }
                    val id = context.get<String>("id")
                    val rawText = context.get<String>("text")
                    val lines = rawText.split("|").map { it.trim() }

                    hologramService.createHologram(id, sender.location, lines)
                    messageService.send(sender, HologramMessages.CREATED, PlaceholderContext.of("id" to id))
                }
        )

        // 2. /core hologram delete <id>
        manager.command(
            manager.commandBuilder("core")
                .literal("hologram")
                .literal("delete")
                .required("id", stringParser())
                .permission(Permissions.HOLOGRAM_ADMIN)
                .handler { context ->
                    val sender = context.sender()
                    val id = context.get<String>("id")
                    if (hologramService.deleteHologram(id)) {
                        messageService.send(sender, HologramMessages.DELETED, PlaceholderContext.of("id" to id))
                    } else {
                        messageService.send(sender, HologramMessages.NOT_FOUND, PlaceholderContext.of("id" to id))
                    }
                }
        )

        // 3. /core hologram list
        manager.command(
            manager.commandBuilder("core")
                .literal("hologram")
                .literal("list")
                .permission(Permissions.HOLOGRAM_ADMIN)
                .handler { context ->
                    val sender = context.sender()
                    val holos = hologramService.getActiveHolograms()
                    if (holos.isEmpty()) {
                        messageService.send(sender, HologramMessages.LIST_EMPTY)
                        return@handler
                    }
                    messageService.send(sender, HologramMessages.LIST_HEADER, PlaceholderContext.of("size" to holos.size.toString()))
                    holos.forEach { (id, loc) ->
                        messageService.send(
                            sender,
                            HologramMessages.LIST_ITEM,
                            PlaceholderContext.of(
                                "id" to id,
                                "world" to loc.world.name,
                                "x" to String.format("%.2f", loc.x),
                                "y" to String.format("%.2f", loc.y),
                                "z" to String.format("%.2f", loc.z)
                            )
                        )
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
                .permission(Permissions.HOLOGRAM_ADMIN)
                .handler { context ->
                    val sender = context.sender()
                    val id = context.get<String>("id")
                    val rawText = context.get<String>("text")
                    val lines = rawText.split("|").map { it.trim() }

                    if (hologramService.editHologram(id, lines)) {
                        messageService.send(sender, HologramMessages.EDIT_SUCCESS, PlaceholderContext.of("id" to id))
                    } else {
                        messageService.send(sender, HologramMessages.NOT_FOUND, PlaceholderContext.of("id" to id))
                    }
                }
        )

        // 5. /core hologram move <id>
        manager.command(
            manager.commandBuilder("core")
                .literal("hologram")
                .literal("move")
                .required("id", stringParser())
                .permission(Permissions.HOLOGRAM_ADMIN)
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        messageService.send(sender, HologramMessages.ONLY_PLAYERS)
                        return@handler
                    }
                    val id = context.get<String>("id")
                    if (hologramService.moveHologram(id, sender.location)) {
                        messageService.send(sender, HologramMessages.MOVE_SUCCESS, PlaceholderContext.of("id" to id))
                    } else {
                        messageService.send(sender, HologramMessages.NOT_FOUND, PlaceholderContext.of("id" to id))
                    }
                }
        )

        // 6. /core hologram center <id>
        manager.command(
            manager.commandBuilder("core")
                .literal("hologram")
                .literal("center")
                .required("id", stringParser())
                .permission(Permissions.HOLOGRAM_ADMIN)
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        messageService.send(sender, HologramMessages.ONLY_PLAYERS)
                        return@handler
                    }
                    val id = context.get<String>("id")
                    
                    val holos = hologramService.getActiveHolograms()
                    val holoLoc = holos[id]
                    if (holoLoc == null) {
                        messageService.send(sender, HologramMessages.NOT_FOUND, PlaceholderContext.of("id" to id))
                        return@handler
                    }

                    // Centrar coordenadas X y Z al centro del bloque
                    val currentLoc = holoLoc.clone()
                    val centeredLoc = Location(
                        currentLoc.world,
                        currentLoc.blockX + 0.5,
                        currentLoc.y,
                        currentLoc.blockZ + 0.5,
                        currentLoc.yaw,
                        currentLoc.pitch
                    )

                    if (hologramService.moveHologram(id, centeredLoc)) {
                        messageService.send(
                            sender,
                            HologramMessages.CENTER_SUCCESS,
                            PlaceholderContext.of(
                                "id" to id,
                                "x" to String.format("%.2f", centeredLoc.x),
                                "z" to String.format("%.2f", centeredLoc.z)
                            )
                        )
                    } else {
                        messageService.send(sender, HologramMessages.CENTER_ERROR, PlaceholderContext.of("id" to id))
                    }
                }
        )

        // 7. /core hologram reload
        manager.command(
            manager.commandBuilder("core")
                .literal("hologram")
                .literal("reload")
                .permission(Permissions.HOLOGRAM_ADMIN)
                .handler { context ->
                    val sender = context.sender()
                    messageService.send(sender, HologramMessages.RELOAD_START)
                    hologramService.reloadHolograms().thenAccept {
                        messageService.send(sender, HologramMessages.RELOAD_SUCCESS)
                    }.exceptionally { ex ->
                        messageService.send(sender, HologramMessages.RELOAD_ERROR, PlaceholderContext.of("error" to (ex.message ?: ex.toString())))
                        null
                    }
                }
        )
    }
}
