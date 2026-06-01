package com.github.berserkr2k.coreplugin.infrastructure.kits

import com.github.berserkr2k.coreplugin.common.gui.CustomMenu
import com.github.berserkr2k.coreplugin.common.gui.MenuConfig
import com.github.berserkr2k.coreplugin.common.gui.MenuItemConfig
import com.github.berserkr2k.coreplugin.common.gui.ItemConfig
import com.github.berserkr2k.coreplugin.common.gui.FillerConfig
import com.github.berserkr2k.coreplugin.common.gui.ItemBuilder
import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.event.inventory.ClickType

class KitGuis(
    private val plugin: Plugin,
    private val configManager: ModularConfigManager,
    private val kitService: KitService
) {
    private val selectorConfig: MenuConfig by lazy {
        configManager.loadModuleConfig("menus/kits-selector.conf", MenuConfig::class.java, createDefaultSelectorConfig()).join()
    }
    
    private val showcaseConfig: MenuConfig by lazy {
        configManager.loadModuleConfig("menus/kits-showcase.conf", MenuConfig::class.java, createDefaultShowcaseConfig()).join()
    }

    private fun createDefaultSelectorConfig(): MenuConfig {
        return MenuConfig(
            title = "<gold><bold>Kits Disponibles</bold></gold>",
            size = 27,
            filler = FillerConfig(
                enabled = true,
                item = ItemConfig(material = "GRAY_STAINED_GLASS_PANE", displayName = " ")
            )
        )
    }

    private fun createDefaultShowcaseConfig(): MenuConfig {
        return MenuConfig(
            title = "<gold>Previsualizar: %kit_name%</gold>",
            size = 27,
            filler = FillerConfig(
                enabled = true,
                item = ItemConfig(material = "GRAY_STAINED_GLASS_PANE", displayName = " ")
            ),
            items = mapOf(
                "back" to MenuItemConfig(
                    slots = listOf(18),
                    item = ItemConfig(material = "BARRIER", displayName = "<red>Volver al Selector</red>", lore = listOf("<gray>Regresa al selector de kits principal.</gray>")),
                    action = "back"
                ),
                "claim" to MenuItemConfig(
                    slots = listOf(22),
                    item = ItemConfig(material = "GREEN_CONCRETE", displayName = "<green><bold>✔ ¡Reclamar Kit!</bold></green>", lore = listOf("%price_lore%%bypass_lore%<gray>Haz click aquí para recibir los items.</gray>")),
                    action = "claim"
                )
            )
        )
    }

    fun openKitSelector(player: Player) {
        val menu = CustomMenu(
            ColorUtility.parse(selectorConfig.title),
            selectorConfig.size,
            plugin
        )

        // Rellenar con paneles decorativos
        if (selectorConfig.filler.enabled) {
            val fillerItem = selectorConfig.filler.item.toItemStack()
            for (i in 0 until selectorConfig.size) {
                menu.setItem(i, fillerItem.clone())
            }
        }

        val sortedKits = kitService.kits.toList().sortedBy { it.first }

        menu.placeDynamicItems(
            selectorConfig,
            sortedKits,
            { it.second.guiSlot }
        ) { (kitId, config), slot ->
            val remaining = kitService.getRemainingCooldown(player.uniqueId, kitId)
            val hasPerm = player.hasPermission(config.permission)
            val hasBypass = player.hasPermission("core.kits.bypass.cooldown") || 
                            player.hasPermission("core.kits.bypass.cooldown.${kitId.lowercase()}")
            val isCooldownActive = remaining > 0 && !hasBypass
            
            val displayMat = Material.matchMaterial(config.guiIcon) ?: Material.CHEST
            
            val loreLines = mutableListOf<String>()
            config.guiLore.forEach { line ->
                loreLines.add(line)
            }
            loreLines.add(" ")

            when {
                !hasPerm -> {
                    loreLines.add("<red>❌ Bloqueado (Requiere Rango)</red>")
                }
                isCooldownActive -> {
                    val timeStr = kitService.formatTime(remaining)
                    loreLines.add("<red>⏳ En Cooldown (Espera: $timeStr)</red>")
                }
                else -> {
                    if (config.cost > 0.0) {
                        loreLines.add("<gold>⚖ Costo: ${config.cost} ${config.currency.uppercase()}</gold>")
                    }
                    if (remaining > 0 && hasBypass) {
                        loreLines.add("<green>✔ ¡Disponible! (<yellow>Bypass de Cooldown Activo</yellow>)</green>")
                    } else {
                        loreLines.add("<green>✔ ¡Disponible para Reclamar!</green>")
                    }
                }
            }

            loreLines.add(" ")
            loreLines.add("<yellow>⚡ Click Izquierdo para Reclamar</yellow>")
            loreLines.add("<aqua>⚡ Click Derecho para Previsualizar</aqua>")

            val kitIcon = ItemBuilder(displayMat)
                .displayName(config.displayName)
                .lore(loreLines)
                .build()

            menu.setItem(slot, kitIcon) { p, event ->
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

        val titleText = showcaseConfig.title.replace("%kit_name%", config.displayName)
        val menu = CustomMenu(
            ColorUtility.parse(titleText),
            slots,
            plugin
        )

        // Rellenar con paneles decorativos
        if (showcaseConfig.filler.enabled) {
            val fillerItem = showcaseConfig.filler.item.toItemStack()
            for (i in 0 until slots) {
                menu.setItem(i, fillerItem.clone())
            }
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

        // Cargar botones desde la configuración HOCON
        val backBtnConfig = showcaseConfig.items["back"] ?: MenuItemConfig(
            item = ItemConfig(material = "BARRIER", displayName = "<red>Volver al Selector</red>", lore = listOf("<gray>Regresa al selector de kits principal.</gray>"))
        )
        val claimBtnConfig = showcaseConfig.items["claim"] ?: MenuItemConfig(
            item = ItemConfig(material = "GREEN_CONCRETE", displayName = "<green><bold>✔ ¡Reclamar Kit!</bold></green>", lore = listOf("%price_lore%%bypass_lore%<gray>Haz click aquí para recibir los items.</gray>"))
        )

        // Botón de Volver
        val backItem = ItemBuilder.fromConfig(backBtnConfig.item).build()
        menu.setItem(slots - 9, backItem) { p, _ ->
            if (backBtnConfig.sound != null) {
                try {
                    p.playSound(p.location, Sound.valueOf(backBtnConfig.sound.uppercase()), 1.0f, 1.0f)
                } catch (e: Exception) {}
            }
            openKitSelector(p)
        }

        // Botón de Reclamar
        val claimItemConfig = claimBtnConfig.item
        val actionItem: ItemStack = when {
            !hasPerm -> {
                ItemBuilder(Material.RED_CONCRETE)
                    .displayName("<red><bold>❌ Kit Bloqueado</bold></red>")
                    .lore(listOf("<gray>Requiere el rango de permiso:</gray>", "<red>${config.permission}</red>"))
                    .build()
            }
            isCooldownActive -> {
                val timeStr = kitService.formatTime(remaining)
                ItemBuilder(Material.YELLOW_CONCRETE)
                    .displayName("<yellow><bold>⏳ En Cooldown</bold></yellow>")
                    .lore(listOf("<gray>Debes esperar:</gray>", "<yellow>$timeStr</yellow>", "<gray>para reclamar nuevamente.</gray>"))
                    .build()
            }
            else -> {
                val priceLore = if (config.cost > 0.0) "<gray>Costo: </gray><gold>${config.cost} ${config.currency.uppercase()}</gold>\n" else ""
                val bypassLore = if (remaining > 0 && hasBypass) "<yellow>¡Bypass de Cooldown Activo!</yellow>\n" else ""
                
                val processedLore = claimItemConfig.lore.map { line ->
                    line.replace("%price_lore%", priceLore).replace("%bypass_lore%", bypassLore)
                }
                
                ItemBuilder.fromConfig(claimItemConfig.copy(lore = processedLore)).build()
            }
        }

        menu.setItem(actionSlot, actionItem) { p, _ ->
            if (claimBtnConfig.sound != null) {
                try {
                    p.playSound(p.location, Sound.valueOf(claimBtnConfig.sound.uppercase()), 1.0f, 1.0f)
                } catch (e: Exception) {}
            }
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

        menu.open(player)
    }
}
