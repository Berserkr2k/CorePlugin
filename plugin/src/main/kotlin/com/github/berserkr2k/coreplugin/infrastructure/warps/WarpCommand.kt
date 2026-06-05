package com.github.berserkr2k.coreplugin.infrastructure.warps

import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.common.gui.CustomMenu
import com.github.berserkr2k.coreplugin.common.gui.ItemBuilder
import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.getWarps
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.StringParser.stringParser

class WarpCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val warpService: WarpService,
    private val messagesConfig: MessagesConfig
) {

    init {
        // 1. /setwarp <name>
        manager.command(
            manager.commandBuilder("setwarp")
                .permission("core.warps.set")
                .required("name", stringParser())
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        sender.sendMessage(ColorUtility.parse(messagesConfig.utility["only-players"] ?: "<red>Solo jugadores pueden ejecutar este comando.</red>"))
                        return@handler
                    }

                    val name = context.get<String>("name")
                    val loc = sender.location
                    warpService.setWarp(
                        name = name,
                        world = loc.world.name,
                        x = loc.x,
                        y = loc.y,
                        z = loc.z,
                        yaw = loc.yaw,
                        pitch = loc.pitch
                    )
                    sender.sendMessage(ColorUtility.parse(messagesConfig.getWarps("set", "name" to name)))
                }
        )

        // 2. /delwarp <name>
        manager.command(
            manager.commandBuilder("delwarp")
                .permission("core.warps.delete")
                .required("name", stringParser())
                .handler { context ->
                    val sender = context.sender()
                    val name = context.get<String>("name")
                    
                    if (warpService.deleteWarp(name)) {
                        sender.sendMessage(ColorUtility.parse(messagesConfig.getWarps("deleted", "name" to name)))
                    } else {
                        sender.sendMessage(ColorUtility.parse(messagesConfig.getWarps("not-found", "name" to name)))
                    }
                }
        )

        // 3. /warp [name]
        manager.command(
            manager.commandBuilder("warp")
                .permission("core.warps.use")
                .optional("name", stringParser())
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        sender.sendMessage(ColorUtility.parse(messagesConfig.utility["only-players"] ?: "<red>Solo jugadores pueden ejecutar este comando.</red>"))
                        return@handler
                    }

                    val nameOpt = context.optional<String>("name")
                    if (nameOpt.isPresent) {
                        val name = nameOpt.get()
                        val warp = warpService.getWarp(name)
                        if (warp == null) {
                            sender.sendMessage(ColorUtility.parse(messagesConfig.getWarps("not-found", "name" to name)))
                            return@handler
                        }
                        warpService.handleTeleportRequest(sender, warp)
                    } else {
                        openWarpsMenu(sender)
                    }
                }
        )

        // 4. /core warps reload
        manager.command(
            manager.commandBuilder("core")
                .literal("warps")
                .literal("reload")
                .permission("core.warps.admin")
                .handler { context ->
                    val sender = context.sender()
                    warpService.reload()
                    sender.sendMessage(ColorUtility.parse("<green>¡Configuraciones de warps recargadas con éxito!</green>"))
                }
        )
    }

    private fun openWarpsMenu(player: Player) {
        val selectorConfig = warpService.menuConfig
        val menuTitle = ColorUtility.parse(selectorConfig.title)
        val menu = CustomMenu(menuTitle, selectorConfig.size, plugin)

        // 1. Relleno de fondo si está activo
        if (selectorConfig.filler.enabled) {
            val fillerItem = selectorConfig.filler.item.toItemStack()
            for (i in 0 until selectorConfig.size) {
                menu.setItem(i, fillerItem.clone())
            }
        }

        // 2. Cargar ítems estáticos
        menu.loadFromConfig(selectorConfig)

        // 3. Cargar warps dinámicos
        val warpsList = warpService.getAllWarps().sortedBy { it.name.lowercase() }

        val drawWarpItem = { warp: WarpConfig, slot: Int ->
            val hasPerm = warp.permission.isEmpty() || player.hasPermission(warp.permission)
            val baseItem = warp.item

            val loreLines = mutableListOf<String>()
            baseItem.lore.forEach { loreLines.add(it) }

            if (!hasPerm) {
                loreLines.add(" ")
                loreLines.add("<red>❌ Bloqueado</red>")
                loreLines.add("<gray>Requiere permiso: <red>${warp.permission}</red></gray>")
            }

            val processedItemConfig = baseItem.copy(
                displayName = (baseItem.displayName ?: "<green><bold>Warp <name></bold></green>").replace("<name>", warp.name),
                lore = loreLines.map { it.replace("<name>", warp.name) }
            )

            val item = ItemBuilder.fromConfig(processedItemConfig).build()

            menu.setItem(slot, item) { p, _ ->
                if (!hasPerm) {
                    p.playSound(p.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f)
                    p.sendMessage(ColorUtility.parse(messagesConfig.getWarps("no-permission", "name" to warp.name)))
                    return@setItem
                }

                p.closeInventory()
                warpService.handleTeleportRequest(p, warp)
            }
        }

        if (selectorConfig.paginated) {
            menu.placePaginatedItems(
                selectorConfig,
                warpsList,
                selectorConfig.previousPageItem,
                selectorConfig.nextPageItem
            ) { warp, slot ->
                drawWarpItem(warp, slot)
            }
        } else {
            menu.placeDynamicItems(
                selectorConfig,
                warpsList,
                { it.guiSlot },
                startSlot = 0
            ) { warp, slot ->
                drawWarpItem(warp, slot)
            }
        }

        menu.open(player)
    }
}
