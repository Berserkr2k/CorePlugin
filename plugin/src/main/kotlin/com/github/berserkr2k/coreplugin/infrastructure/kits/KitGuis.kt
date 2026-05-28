package com.github.berserkr2k.coreplugin.infrastructure.kits

import com.github.berserkr2k.coreplugin.common.gui.CustomMenu
import com.github.berserkr2k.coreplugin.common.ColorUtility
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.event.inventory.ClickType

class KitGuis(
    private val plugin: Plugin,
    private val kitService: KitService
) {

    fun openKitSelector(player: Player) {
        val menu = CustomMenu(
            ColorUtility.parse("<gold><bold>Kits Disponibles</bold></gold>"),
            27,
            plugin
        )

        val panel = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val pMeta = panel.itemMeta
        pMeta.displayName(ColorUtility.parse(" "))
        panel.itemMeta = pMeta
        for (i in listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25)) {
            menu.setItem(i, panel)
        }

        var slot = 10
        kitService.kits.forEach { (kitId, config) ->
            if (slot > 16) return@forEach 

            val remaining = kitService.getRemainingCooldown(player.uniqueId, kitId)
            val hasPerm = player.hasPermission(config.permission)
            val hasBypass = player.hasPermission("core.kits.bypass.cooldown") || 
                            player.hasPermission("core.kits.bypass.cooldown.${kitId.lowercase()}")
            val isCooldownActive = remaining > 0 && !hasBypass
            
            val displayMat = Material.matchMaterial(config.guiIcon) ?: Material.CHEST
            val kitItem = ItemStack(displayMat)
            val meta = kitItem.itemMeta ?: return@forEach

            meta.displayName(ColorUtility.parse(config.displayName))

            val loreLines = mutableListOf<net.kyori.adventure.text.Component>()
            config.guiLore.forEach { line ->
                loreLines.add(ColorUtility.parse(line))
            }
            
            loreLines.add(ColorUtility.parse(" "))

            when {
                !hasPerm -> {
                    loreLines.add(ColorUtility.parse("<red>❌ Bloqueado (Requiere Rango)</red>"))
                }
                isCooldownActive -> {
                    val timeStr = kitService.formatTime(remaining)
                    loreLines.add(ColorUtility.parse("<red>⏳ En Cooldown (Espera: $timeStr)</red>"))
                }
                else -> {
                    if (config.cost > 0.0) {
                        loreLines.add(ColorUtility.parse("<gold>⚖ Costo: ${config.cost} ${config.currency.uppercase()}</gold>"))
                    }
                    if (remaining > 0 && hasBypass) {
                        loreLines.add(ColorUtility.parse("<green>✔ ¡Disponible! (<yellow>Bypass de Cooldown Activo</yellow>)</green>"))
                    } else {
                        loreLines.add(ColorUtility.parse("<green>✔ ¡Disponible para Reclamar!</green>"))
                    }
                }
            }

            loreLines.add(ColorUtility.parse(" "))
            loreLines.add(ColorUtility.parse("<yellow>⚡ Click Izquierdo para Reclamar</yellow>"))
            loreLines.add(ColorUtility.parse("<aqua>⚡ Click Derecho para Previsualizar</aqua>"))

            meta.lore(loreLines)
            kitItem.itemMeta = meta

            menu.setItem(slot, kitItem) { p, event ->
                if (event.click == ClickType.RIGHT) {
                    openKitShowcase(p, kitId)
                } else {
                    p.closeInventory()
                    kitService.claimKit(p, kitId, false).thenAccept { result ->
                        when (result) {
                            is ClaimResult.Success -> p.sendMessage(ColorUtility.parse(result.message))
                            is ClaimResult.Failure -> {
                                p.sendMessage(ColorUtility.parse(result.reason))
                                p.playSound(p.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f)
                            }
                        }
                    }
                }
            }
            slot++
        }

        menu.open(player)
    }

    fun openKitShowcase(player: Player, kitId: String) {
        val config = kitService.kits[kitId.lowercase()] ?: return
        val items = config.items.mapNotNull { kitService.buildItemStack(it) }

        val slots = when {
            items.size <= 7 -> 27
            items.size <= 14 -> 36
            else -> 54
        }

        val menu = CustomMenu(
            ColorUtility.parse("<gold>Previsualizar: ${config.displayName}</gold>"),
            slots,
            plugin
        )

        val panel = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val pMeta = panel.itemMeta
        pMeta.displayName(ColorUtility.parse(" "))
        panel.itemMeta = pMeta

        for (i in 0 until slots) {
            menu.setItem(i, panel)
        }

        val activeSlots = mutableListOf<Int>()
        if (slots == 27) {
            activeSlots.addAll(10..16)
        } else if (slots == 36) {
            activeSlots.addAll(10..16)
            activeSlots.addAll(19..25)
        } else {
            activeSlots.addAll(10..16)
            activeSlots.addAll(19..25)
            activeSlots.addAll(28..34)
            activeSlots.addAll(37..43)
        }

        items.forEachIndexed { index, itemStack ->
            if (index < activeSlots.size) {
                menu.setItem(activeSlots[index], itemStack)
            }
        }

        val actionSlot = slots - 5 
        val remaining = kitService.getRemainingCooldown(player.uniqueId, kitId)
        val hasPerm = player.hasPermission(config.permission)
        val hasBypass = player.hasPermission("core.kits.bypass.cooldown") || 
                        player.hasPermission("core.kits.bypass.cooldown.${kitId.lowercase()}")
        val isCooldownActive = remaining > 0 && !hasBypass

        val actionItem = when {
            !hasPerm -> {
                createGuiItem(Material.RED_CONCRETE, "<red><bold>❌ Kit Bloqueado</bold></red>", "<gray>Requiere el rango de permiso:</gray>\n<red>${config.permission}</red>")
            }
            isCooldownActive -> {
                val timeStr = kitService.formatTime(remaining)
                createGuiItem(Material.YELLOW_CONCRETE, "<yellow><bold>⏳ En Cooldown</bold></yellow>", "<gray>Debes esperar:</gray>\n<yellow>$timeStr</yellow>\n<gray>para reclamar nuevamente.</gray>")
            }
            else -> {
                val priceLore = if (config.cost > 0.0) "<gray>Costo: </gray><gold>${config.cost} ${config.currency.uppercase()}</gold>\n" else ""
                val bypassLore = if (remaining > 0 && hasBypass) "<yellow>¡Bypass de Cooldown Activo!</yellow>\n" else ""
                createGuiItem(Material.GREEN_CONCRETE, "<green><bold>✔ ¡Reclamar Kit!</bold></green>", "$priceLore$bypassLore<gray>Haz click aquí para recibir los items.</gray>")
            }
        }

        menu.setItem(actionSlot, actionItem) { p, _ ->
            p.closeInventory()
            kitService.claimKit(p, kitId, false).thenAccept { result ->
                when (result) {
                    is ClaimResult.Success -> p.sendMessage(ColorUtility.parse(result.message))
                    is ClaimResult.Failure -> {
                        p.sendMessage(ColorUtility.parse(result.reason))
                        p.playSound(p.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f)
                    }
                }
            }
        }

        val returnSlot = slots - 9
        menu.setItem(returnSlot, createGuiItem(Material.BARRIER, "<red>Volver al Selector</red>", "<gray>Regresa al selector de kits principal.</gray>")) { p, _ ->
            openKitSelector(p)
        }

        menu.open(player)
    }

    private fun createGuiItem(material: Material, title: String, loreText: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item
        meta.displayName(ColorUtility.parse(title))
        meta.lore(loreText.split("\n").map { ColorUtility.parse(it) })
        item.itemMeta = meta
        return item
    }
}
