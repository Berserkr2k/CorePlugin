package com.github.berserkr2k.coreplugin.infrastructure.kits

import com.github.berserkr2k.coreplugin.api.framework.menu.*
import com.github.berserkr2k.coreplugin.api.framework.item.*
import com.github.berserkr2k.coreplugin.api.config.ItemConfig
import com.github.berserkr2k.coreplugin.api.feature.kits.ClaimResult
import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.event.inventory.ClickType
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry

class KitGuis(
    private val plugin: Plugin,
    private val configManager: ModularConfigManager,
    private val kitService: KitService,
    private val serviceRegistry: ServiceRegistry
) {
    private val menuService = serviceRegistry.get(MenuService::class.java)!!
    private val itemBuilderFactory = serviceRegistry.get(ItemBuilderFactory::class.java)!!

    var selectorConfig: MenuConfig = createDefaultSelectorConfig()
        private set
    
    var showcaseConfig: MenuConfig = createDefaultShowcaseConfig()
        private set

    init {
        reload()
    }

    fun reload() {
        selectorConfig = configManager.loadModuleConfig("menus/kits-selector.conf", MenuConfig::class.java, createDefaultSelectorConfig()).join()
        showcaseConfig = configManager.loadModuleConfig("menus/kits-showcase.conf", MenuConfig::class.java, createDefaultShowcaseConfig()).join()
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
        val builder = menuService.createBuilder()
            .title(ColorUtility.parse(selectorConfig.title))
            .slots(selectorConfig.size)

        // Rellenar con paneles decorativos
        if (selectorConfig.filler.enabled) {
            val fillerItem = itemBuilderFactory.builder(selectorConfig.filler.item).build()
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

            val kitIcon = itemBuilderFactory.builder(baseItem)
                .lore(loreLines)
                .build()

            val btn = Button.builder()
                .icon(kitIcon)
                .onClick { p, clickType ->
                    if (clickType == ClickType.RIGHT) {
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
                .build()
            builder.button(slot, btn)
        }

        if (selectorConfig.paginated) {
            val prevArrow = itemBuilderFactory.builder(selectorConfig.previousPageItem).build()
            val nextArrow = itemBuilderFactory.builder(selectorConfig.nextPageItem).build()
            builder.placePaginatedItems(
                selectorConfig,
                sortedKits,
                prevArrow,
                nextArrow
            ) { kitEntry, slot ->
                drawKit(kitEntry, slot)
            }
        } else {
            builder.placeDynamicItems(
                selectorConfig,
                sortedKits,
                { it.second.guiSlot }
            ) { kitEntry, slot ->
                drawKit(kitEntry, slot)
            }
        }

        builder.build().open(player)
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
        val builder = menuService.createBuilder()
            .title(ColorUtility.parse(titleText))
            .slots(slots)

        // Rellenar con paneles decorativos
        if (showcaseConfig.filler.enabled) {
            val fillerItem = itemBuilderFactory.builder(showcaseConfig.filler.item).build()
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
        val backBtnConfig = showcaseConfig.items["back"] ?: MenuItemConfig(
            item = ItemConfig(material = "BARRIER", displayName = "<red>Volver al Selector</red>", lore = listOf("<gray>Regresa al selector de kits principal.</gray>"))
        )
        val claimBtnConfig = showcaseConfig.items["claim"] ?: MenuItemConfig(
            item = ItemConfig(material = "GREEN_CONCRETE", displayName = "<green><bold>✔ ¡Reclamar Kit!</bold></green>", lore = listOf("%price_lore%%bypass_lore%<gray>Haz click aquí para recibir los items.</gray>"))
        )

        // Botón de Volver
        val backItem = itemBuilderFactory.builder(backBtnConfig.item).build()
        val backBtn = Button.builder()
            .icon(backItem)
            .onClick { p ->
                backBtnConfig.sound?.let { sound ->
                    try {
                        p.playSound(p.location, Sound.valueOf(sound.uppercase()), 1.0f, 1.0f)
                    } catch (e: Exception) {}
                }
                openKitSelector(p)
            }
            .build()
        builder.button(slots - 9, backBtn)

        // Botón de Reclamar
        val claimItemConfig = claimBtnConfig.item
        val actionItem: ItemStack = when {
            !hasPerm -> {
                itemBuilderFactory.builder(Material.RED_CONCRETE)
                    .displayName("<red><bold>❌ Kit Bloqueado</bold></red>")
                    .lore(listOf("<gray>Requiere el rango de permiso:</gray>", "<red>${config.permission}</red>"))
                    .build()
            }
            isCooldownActive -> {
                val timeStr = kitService.formatTime(remaining)
                itemBuilderFactory.builder(Material.YELLOW_CONCRETE)
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
                
                itemBuilderFactory.builder(claimItemConfig.copy(lore = processedLore)).build()
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
                        is ClaimResult.Success -> p.sendMessage(ColorUtility.parse(result.message))
                        is ClaimResult.Failure -> {
                            p.sendMessage(ColorUtility.parse(result.reason))
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
