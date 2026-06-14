package com.github.berserkr2k.coreplugin.infrastructure.warps

import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.api.framework.menu.*
import com.github.berserkr2k.coreplugin.api.framework.item.*
import com.github.berserkr2k.coreplugin.api.config.ItemConfig
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.CoreMessages
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.StringParser.stringParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry

class WarpCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val warpService: WarpService,
    private val messageService: MessageService,
    private val serviceRegistry: ServiceRegistry
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val menuService = serviceRegistry.get(MenuService::class.java)!!
    private val itemBuilderFactory = serviceRegistry.get(ItemBuilderFactory::class.java)!!

    init {
        // 1. /setwarp <name>
        manager.command(
            manager.commandBuilder("setwarp")
                .permission("core.warps.set")
                .required("name", stringParser())
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        messageService.send(sender, CoreMessages.ONLY_PLAYERS)
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
                    messageService.send(sender, WarpMessages.SET, PlaceholderContext.of("name" to name))
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
                        messageService.send(sender, WarpMessages.DELETED, PlaceholderContext.of("name" to name))
                    } else {
                        messageService.send(sender, WarpMessages.NOT_FOUND, PlaceholderContext.of("name" to name))
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
                        messageService.send(sender, CoreMessages.ONLY_PLAYERS)
                        return@handler
                    }

                    val nameOpt = context.optional<String>("name")
                    if (nameOpt.isPresent) {
                        val name = nameOpt.get()
                        val warp = warpService.getWarpConfig(name)
                        if (warp == null) {
                            messageService.send(sender, WarpMessages.NOT_FOUND, PlaceholderContext.of("name" to name))
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
                    coroutineScope.launch {
                        warpService.reload()
                        messageService.send(sender, WarpMessages.RELOADED)
                    }
                }
        )
    }

    private fun openWarpsMenu(player: Player) {
        val selectorConfig = warpService.menuConfig
        val menuTitle = ColorUtility.parse(selectorConfig.menu.title)
        
        val builder = menuService.createBuilder()
            .title(menuTitle)
            .slots(selectorConfig.menu.size)

        // 1. Relleno de fondo si está activo
        if (selectorConfig.menu.filler.enabled) {
            val fillerItem = itemBuilderFactory.builder(selectorConfig.menu.filler.item).build()
            val fillerButton = Button.builder().icon(fillerItem).build()
            builder.fill(fillerButton)
        }

        // 2. Cargar ítems estáticos
        builder.loadFromConfig(selectorConfig.menu)

        // 3. Cargar warps dinámicos
        val warpsList = warpService.getAllWarpConfigs().sortedBy { it.name.lowercase() }

        val drawWarpItem = { warp: WarpConfig, slot: Int ->
            val hasPerm = warp.permission.isEmpty() || player.hasPermission(warp.permission)
            val baseItem = warp.item

            val loreLines = mutableListOf<String>()
            baseItem.lore.forEach { loreLines.add(it) }

            if (!hasPerm) {
                loreLines.add(" ")
                loreLines.add(selectorConfig.statusLocked)
                loreLines.add(selectorConfig.statusRequiresPermission.replace("<permission>", warp.permission))
            }

            val defaultDisplayName = selectorConfig.defaultDisplayName
            val processedItemConfig = baseItem.copy(
                displayName = (baseItem.displayName ?: defaultDisplayName).replace("<name>", warp.name),
                lore = loreLines.map { it.replace("<name>", warp.name) }
            )

            val item = itemBuilderFactory.builder(processedItemConfig).build()

            val btn = Button.builder()
                .icon(item)
                .onClick { p ->
                    if (!hasPerm) {
                        p.playSound(p.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f)
                        messageService.send(p, WarpMessages.NO_PERMISSION, PlaceholderContext.of("name" to warp.name))
                        return@onClick
                    }

                    p.closeInventory()
                    warpService.handleTeleportRequest(p, warp)
                }
                .build()
            builder.button(slot, btn)
        }

        if (selectorConfig.menu.paginated) {
            builder.placePaginatedItems(
                selectorConfig.menu,
                warpsList,
                selectorConfig.menu.previousPageItem,
                selectorConfig.menu.nextPageItem
            ) { warp, slot ->
                drawWarpItem(warp, slot)
            }
        } else {
            builder.placeDynamicItems(
                selectorConfig.menu,
                warpsList,
                { it.guiSlot },
                startSlot = 0
            ) { warp, slot ->
                drawWarpItem(warp, slot)
            }
        }

        builder.build().open(player)
    }
}
