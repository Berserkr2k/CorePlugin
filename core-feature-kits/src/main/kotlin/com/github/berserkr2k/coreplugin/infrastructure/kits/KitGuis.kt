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
    private val configService: com.github.berserkr2k.coreplugin.api.core.config.ConfigService,
    private val kitService: KitService,
    private val messageService: MessageService
) {
    var selectorConfig: MenuConfig = createDefaultSelectorConfig()
        private set
    
    var showcaseConfig: MenuConfig = createDefaultShowcaseConfig()
        private set



    private val folderProvider by lazy {
        org.bukkit.Bukkit.getServicesManager().load(com.github.berserkr2k.coreplugin.api.di.ServiceRegistry::class.java)!!
            .get(com.github.berserkr2k.coreplugin.api.core.filesystem.FeatureFolderProvider::class.java)!!
    }

    init {
        reload()
    }

    fun reload() {
        val configFolder = folderProvider.getFeatureConfigFolder("kits")
        val selectorFile = configFolder.resolve("kits-selector.conf").toFile()
        val showcaseFile = configFolder.resolve("kits-showcase.conf").toFile()
        selectorConfig = configService.loadConfig(selectorFile, MenuConfig::class.java, createDefaultSelectorConfig())
        showcaseConfig = configService.loadConfig(showcaseFile, MenuConfig::class.java, createDefaultShowcaseConfig())
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

    fun openKitSelector(player: Player, menuService: MenuService, itemBuilderFactory: ItemBuilderFactory) {
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
                    loreLines.add(messageService.getRawTemplate(KitMessages.GUI_STATUS_LOCKED).ifEmpty { "<red>❌ Bloqueado (Requiere Rango)</red>" })
                }
                isCooldownActive -> {
                    val timeStr = kitService.formatTime(remaining)
                    loreLines.add(messageService.getRawTemplate(KitMessages.GUI_STATUS_COOLDOWN).ifEmpty { "<red>⏳ En Cooldown (Espera: <time>)</red>" }.replace("<time>", timeStr))
                }
                else -> {
                    if (config.cost > 0.0) {
                        loreLines.add(messageService.getRawTemplate(KitMessages.GUI_STATUS_COST).ifEmpty { "<gold>⚖ Costo: <cost> <currency></gold>" }
                            .replace("<cost>", config.cost.toString())
                            .replace("<currency>", config.currency.uppercase()))
                    }
                    if (remaining > 0 && hasBypass) {
                        loreLines.add(messageService.getRawTemplate(KitMessages.GUI_STATUS_BYPASS).ifEmpty { "<green>✔ ¡Disponible! (<yellow>Bypass de Cooldown Activo</yellow>)</green>" })
                    } else {
                        loreLines.add(messageService.getRawTemplate(KitMessages.GUI_STATUS_AVAILABLE).ifEmpty { "<green>✔ ¡Disponible para Reclamar!</green>" })
                    }
                }
            }

            loreLines.add(" ")
            loreLines.add(messageService.getRawTemplate(KitMessages.GUI_ACTION_CLAIM).ifEmpty { "<yellow>⚡ Click Izquierdo para Reclamar</yellow>" })
            loreLines.add(messageService.getRawTemplate(KitMessages.GUI_ACTION_PREVIEW).ifEmpty { "<aqua>⚡ Click Derecho para Previsualizar</aqua>" })

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

        if (selectorConfig.paginated) {
            builder.placePaginatedItems(
                selectorConfig,
                sortedKits,
                selectorConfig.previousPageItem,
                selectorConfig.nextPageItem
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

    fun openKitShowcase(player: Player, kitId: String, menuService: MenuService, itemBuilderFactory: ItemBuilderFactory) {
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
            item = ItemConfig(material = "BARRIER", displayName = "", lore = emptyList())
        )
        val claimBtnConfig = showcaseConfig.items["claim"] ?: MenuItemConfig(
            item = ItemConfig(material = "GREEN_CONCRETE", displayName = "", lore = emptyList())
        )

        // Botón de Volver
        val backDisplayName = messageService.getRawTemplate(KitMessages.GUI_SHOWCASE_BACK_NAME).ifEmpty {
            backBtnConfig.item.displayName?.ifEmpty { "<red>Volver al Selector</red>" } ?: "<red>Volver al Selector</red>"
        }
        val backLoreRaw = messageService.getRawTemplate(KitMessages.GUI_SHOWCASE_BACK_LORE).ifEmpty {
            if (backBtnConfig.item.lore.isNotEmpty()) {
                backBtnConfig.item.lore.joinToString("\n")
            } else {
                "<gray>Regresa al selector de kits principal.</gray>"
            }
        }
        val backLore = backLoreRaw.split("\n")

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
                val name = messageService.getRawTemplate(KitMessages.GUI_SHOWCASE_LOCKED_NAME).ifEmpty { "<red><bold>❌ Kit Bloqueado</bold></red>" }
                val loreRaw = messageService.getRawTemplate(KitMessages.GUI_SHOWCASE_LOCKED_LORE).ifEmpty { "<gray>Requiere el rango de permiso:</gray>\n<red><permission></red>" }
                val resolvedLore = loreRaw.replace("<permission>", config.permission).split("\n")
                itemBuilderFactory.builder(Material.RED_CONCRETE)
                    .displayName(name)
                    .lore(resolvedLore)
                    .build()
            }
            isCooldownActive -> {
                val timeStr = kitService.formatTime(remaining)
                val name = messageService.getRawTemplate(KitMessages.GUI_SHOWCASE_COOLDOWN_NAME).ifEmpty { "<yellow><bold>⏳ En Cooldown</bold></yellow>" }
                val loreRaw = messageService.getRawTemplate(KitMessages.GUI_SHOWCASE_COOLDOWN_LORE).ifEmpty { "<gray>Debes esperar:</gray>\n<yellow><time></yellow>\n<gray>para reclamar nuevamente.</gray>" }
                val resolvedLore = loreRaw.replace("<time>", timeStr).split("\n")
                itemBuilderFactory.builder(Material.YELLOW_CONCRETE)
                    .displayName(name)
                    .lore(resolvedLore)
                    .build()
            }
            else -> {
                val priceLore = if (config.cost > 0.0) "<gray>Costo: </gray><gold>${config.cost} ${config.currency.uppercase()}</gold>\n" else ""
                val bypassLore = if (remaining > 0 && hasBypass) "<yellow>¡Bypass de Cooldown Activo!</yellow>\n" else ""
                
                val claimDisplayName = messageService.getRawTemplate(KitMessages.GUI_SHOWCASE_CLAIM_NAME).ifEmpty {
                    claimItemConfig.displayName?.ifEmpty { "<green><bold>✔ ¡Reclamar Kit!</bold></green>" } ?: "<green><bold>✔ ¡Reclamar Kit!</bold></green>"
                }
                
                val rawLore = messageService.getRawTemplate(KitMessages.GUI_SHOWCASE_CLAIM_LORE).ifEmpty {
                    if (claimItemConfig.lore.isNotEmpty()) {
                        claimItemConfig.lore.joinToString("\n")
                    } else {
                        "%price_lore%%bypass_lore%<gray>Haz click aquí para recibir los items.</gray>"
                    }
                }
                val processedLore = rawLore.replace("%price_lore%", priceLore).replace("%bypass_lore%", bypassLore).split("\n")
                
                itemBuilderFactory.builder(claimItemConfig.copy(displayName = claimDisplayName, lore = processedLore)).build()
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
