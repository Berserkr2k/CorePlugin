package com.github.berserkr2k.coreplugin.infrastructure.kits

import com.github.berserkr2k.coreplugin.api.framework.menu.*
import com.github.berserkr2k.coreplugin.api.framework.item.*
import com.github.berserkr2k.coreplugin.api.config.ItemConfig
import com.github.berserkr2k.coreplugin.api.feature.kits.ClaimResult
import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.event.inventory.ClickType
// Configurate imports decoupled
import java.io.File

class KitGuis(
    private val plugin: Plugin,
    private val kitService: KitService,
    private val messageService: MessageService
) {
    val selectorConfig: KitsSelectorMenuConfig
        get() = kitService.selectorConfig
    
    val showcaseConfig: KitsShowcaseMenuConfig
        get() = kitService.showcaseConfig

    init {
        reload()
    }

    fun reload() {
        // No-op, configuration reloads are now centrally managed by KitService
    }

    fun openKitSelector(player: Player, menuService: MenuService, itemBuilderFactory: ItemBuilderFactory) {
        val builder = menuService.createBuilder()
            .title(ColorUtility.parse(selectorConfig.menu.title))
            .slots(selectorConfig.menu.size)

        // Rellenar con paneles decorativos
        if (selectorConfig.menu.filler.enabled) {
            val fillerItem = itemBuilderFactory.builder(selectorConfig.menu.filler.item).build()
            val fillerButton = Button.builder().icon(fillerItem).build()
            builder.fill(fillerButton)
        }

        val sortedKits = kitService.kits.toList().sortedBy { it.first }

        val drawKit = { (kitId, config): Pair<String, KitConfig>, slot: Int ->
            val remaining = kitService.getRemainingCooldown(player.uniqueId, kitId)
            val hasPerm = player.hasPermission(config.permission)
            val hasBypass = player.hasPermission("core.kits.bypass.cooldown") || 
                            player.hasPermission("core.kits.bypass.cooldown.${kitId.lowercase()}")
            val isCooldownActive = remaining > 0 && !hasBypass
            
            val baseItem = config.item
            
            val loreLines = mutableListOf<String>()
            baseItem.lore.forEach { line ->
                loreLines.add(line)
            }
            loreLines.add(" ")

            when {
                !hasPerm -> {
                    loreLines.add(selectorConfig.statusLocked)
                }
                isCooldownActive -> {
                    val timeStr = kitService.formatTime(remaining)
                    loreLines.add(selectorConfig.statusCooldown.replace("<time>", timeStr))
                }
                else -> {
                    if (config.cost > 0.0) {
                        loreLines.add(selectorConfig.statusCost
                            .replace("<cost>", config.cost.toString())
                            .replace("<currency>", config.currency.uppercase()))
                    }
                    if (remaining > 0 && hasBypass) {
                        loreLines.add(selectorConfig.statusBypass)
                    } else {
                        loreLines.add(selectorConfig.statusAvailable)
                    }
                }
            }

            loreLines.add(" ")
            loreLines.add(selectorConfig.actionClaim)
            loreLines.add(selectorConfig.actionPreview)

            val kitIcon = itemBuilderFactory.builder(baseItem)
                .lore(loreLines)
                .build()

            val btn = Button.builder()
                .icon(kitIcon)
                .onClick { p, clickType ->
                    if (clickType == ClickType.RIGHT) {
                        openKitShowcase(p, kitId, menuService, itemBuilderFactory)
                    } else {
                        p.closeInventory()
                        kitService.claimKit(p, kitId, false).thenAccept { result ->
                            when (result) {
                                is ClaimResult.Success -> messageService.sendRaw(p, result.message)
                                is ClaimResult.Failure -> {
                                    messageService.sendRaw(p, result.reason)
                                    p.playSound(p.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f)
                                }
                            }
                        }
                    }
                }
                .build()
            builder.button(slot, btn)
        }

        if (selectorConfig.menu.paginated) {
            builder.placePaginatedItems(
                selectorConfig.menu,
                sortedKits,
                selectorConfig.menu.previousPageItem,
                selectorConfig.menu.nextPageItem
            ) { kitEntry, slot ->
                drawKit(kitEntry, slot)
            }
        } else {
            builder.placeDynamicItems(
                selectorConfig.menu,
                sortedKits,
                { it.second.guiSlot }
            ) { kitEntry, slot ->
                drawKit(kitEntry, slot)
            }
        }

        builder.build().open(player)
    }

    fun openKitShowcase(player: Player, kitId: String, menuService: MenuService, itemBuilderFactory: ItemBuilderFactory) {
        val config = kitService.kits[kitId.lowercase()] ?: return
        val items = config.items.mapNotNull { kitService.buildItemStack(it) }

        val slots = when {
            items.size <= 7 -> 27
            items.size <= 14 -> 36
            else -> 54
        }

        val titleText = showcaseConfig.menu.title.replace("%kit_name%", config.displayName)
        val builder = menuService.createBuilder()
            .title(ColorUtility.parse(titleText))
            .slots(slots)

        // Rellenar con paneles decorativos
        if (showcaseConfig.menu.filler.enabled) {
            val fillerItem = itemBuilderFactory.builder(showcaseConfig.menu.filler.item).build()
            val fillerButton = Button.builder().icon(fillerItem).build()
            builder.fill(fillerButton)
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
                val itemBtn = Button.builder().icon(itemStack).build()
                builder.button(activeSlots[index], itemBtn)
            }
        }

        val actionSlot = slots - 5 
        val remaining = kitService.getRemainingCooldown(player.uniqueId, kitId)
        val hasPerm = player.hasPermission(config.permission)
        val hasBypass = player.hasPermission("core.kits.bypass.cooldown") || 
                        player.hasPermission("core.kits.bypass.cooldown.${kitId.lowercase()}")
        val isCooldownActive = remaining > 0 && !hasBypass

        // Cargar botones desde la configuración HOCON
        val backBtnConfig = showcaseConfig.menu.items["back"] ?: MenuItemConfig(
            item = ItemConfig(material = "BARRIER", displayName = "", lore = emptyList())
        )
        val claimBtnConfig = showcaseConfig.menu.items["claim"] ?: MenuItemConfig(
            item = ItemConfig(material = "GREEN_CONCRETE", displayName = "", lore = emptyList())
        )

        // Botón de Volver
        val backDisplayName = showcaseConfig.backName
        val backLore = showcaseConfig.backLore

        val backItem = itemBuilderFactory.builder(backBtnConfig.item.copy(displayName = backDisplayName, lore = backLore)).build()
        val backBtn = Button.builder()
            .icon(backItem)
            .onClick { p ->
                backBtnConfig.sound?.let { sound ->
                    try {
                        p.playSound(p.location, Sound.valueOf(sound.uppercase()), 1.0f, 1.0f)
                    } catch (e: Exception) {}
                }
                openKitSelector(p, menuService, itemBuilderFactory)
            }
            .build()
        builder.button(slots - 9, backBtn)

        // Botón de Reclamar
        val claimItemConfig = claimBtnConfig.item
        val actionItem: ItemStack = when {
            !hasPerm -> {
                val name = showcaseConfig.lockedName
                val resolvedLore = showcaseConfig.lockedLore.map { line ->
                    line.replace("<permission>", config.permission)
                }
                itemBuilderFactory.builder(Material.RED_CONCRETE)
                    .displayName(name)
                    .lore(resolvedLore)
                    .build()
            }
            isCooldownActive -> {
                val timeStr = kitService.formatTime(remaining)
                val name = showcaseConfig.cooldownName
                val resolvedLore = showcaseConfig.cooldownLore.map { line ->
                    line.replace("<time>", timeStr)
                }
                itemBuilderFactory.builder(Material.YELLOW_CONCRETE)
                    .displayName(name)
                    .lore(resolvedLore)
                    .build()
            }
            else -> {
                val priceLore = if (config.cost > 0.0) "<gray>Costo: </gray><gold>${config.cost} ${config.currency.uppercase()}</gold>\n" else ""
                val bypassLore = if (remaining > 0 && hasBypass) "<yellow>¡Bypass de Cooldown Activo!</yellow>\n" else ""
                
                val claimDisplayName = showcaseConfig.claimName
                val resolvedLore = showcaseConfig.claimLore.flatMap { line ->
                    line.replace("%price_lore%", priceLore).replace("%bypass_lore%", bypassLore).split("\n")
                }
                
                itemBuilderFactory.builder(claimItemConfig.copy(displayName = claimDisplayName, lore = resolvedLore)).build()
            }
        }

        val claimBtn = Button.builder()
            .icon(actionItem)
            .onClick { p ->
                claimBtnConfig.sound?.let { sound ->
                    try {
                        p.playSound(p.location, Sound.valueOf(sound.uppercase()), 1.0f, 1.0f)
                    } catch (e: Exception) {}
                }
                p.closeInventory()
                kitService.claimKit(p, kitId, false).thenAccept { result ->
                    when (result) {
                        is ClaimResult.Success -> messageService.sendRaw(p, result.message)
                        is ClaimResult.Failure -> {
                            messageService.sendRaw(p, result.reason)
                            p.playSound(p.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f)
                        }
                    }
                }
            }
            .build()
        builder.button(actionSlot, claimBtn)

        builder.build().open(player)
    }
}
