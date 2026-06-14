package com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails

import com.github.berserkr2k.coreplugin.api.framework.menu.*
import com.github.berserkr2k.coreplugin.api.framework.item.*
import com.github.berserkr2k.coreplugin.api.config.ItemConfig
import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player

class TrailGuis(
    private val trailManager: ProjectileTrailManager,
    private val regionTaskScheduler: com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler,
    private val menuService: MenuService,
    private val itemBuilderFactory: ItemBuilderFactory,
    private val messageService: com.github.berserkr2k.coreplugin.api.core.message.MessageService
) {

    fun openTrailSelector(player: Player) {
        val selectorConfig = trailManager.selectorConfig
        val displayConfig = trailManager.displayConfig
        val builder = menuService.createBuilder()
            .title(ColorUtility.parse(selectorConfig.menu.title))
            .slots(selectorConfig.menu.size)

        // Rellenar con paneles decorativos
        if (selectorConfig.menu.filler.enabled) {
            val fillerItem = itemBuilderFactory.builder(selectorConfig.menu.filler.item).build()
            val fillerButton = Button.builder().icon(fillerItem).build()
            builder.fill(fillerButton)
        }

        val activeTrail = trailManager.getActiveTrail(player.uniqueId)

        // 1. Determinar qué ranuras están ocupadas por botones estáticos
        val occupiedSlots = mutableSetOf<Int>()
        val clearItemConfig = selectorConfig.menu.items["clear"]
        if (clearItemConfig != null) {
            occupiedSlots.addAll(clearItemConfig.slots)
        } else {
            occupiedSlots.add(22)
        }

        val sortedTrails = trailManager.trails.values.sortedBy { it.id }

        val drawTrail = { config: TrailConfig, slot: Int ->
            val trailId = config.id
            val hasPerm = player.hasPermission(config.permission)
            val isActive = activeTrail == trailId
            val baseItem = config.item

            val loreLines = mutableListOf<String>()
            baseItem.lore.forEach { line ->
                loreLines.add(line)
            }
            loreLines.add(" ")

            when {
                isActive -> {
                    loreLines.add(displayConfig.statusEquipped)
                    loreLines.add(displayConfig.statusEquippedLore)
                }
                hasPerm -> {
                    loreLines.add(displayConfig.statusSelect)
                }
                else -> {
                    loreLines.add(displayConfig.statusLocked)
                    loreLines.add(displayConfig.statusLockedLore.replace("<permission>", config.permission))
                }
            }

            val icon = itemBuilderFactory.builder(baseItem)
                .lore(loreLines)
                .glow(isActive)
                .build()

            val btn = Button.builder()
                .icon(icon)
                .onClick { p ->
                    if (isActive) {
                        p.playSound(p.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f)
                        return@onClick
                    }

                    if (hasPerm) {
                        trailManager.savePlayerTrail(p.uniqueId, trailId).thenRun {
                            p.playSound(p.location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f)
                            messageService.send(p, TrailMessages.EQUIPPED, PlaceholderContext.of("name" to config.displayName))
                            // Reabrir regionalmente en sincronismo
                            regionTaskScheduler.runAtLocation(p.location, Runnable {
                                openTrailSelector(p)
                            })
                        }
                    } else {
                        p.playSound(p.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f)
                        messageService.send(p, TrailMessages.NO_PERMISSION)
                    }
                }
                .build()
            builder.button(slot, btn)
        }

        if (selectorConfig.menu.paginated) {
            builder.placePaginatedItems(
                selectorConfig.menu,
                sortedTrails,
                selectorConfig.menu.previousPageItem,
                selectorConfig.menu.nextPageItem
            ) { trailConfig, slot ->
                drawTrail(trailConfig, slot)
            }
        } else {
            builder.placeDynamicItems(
                selectorConfig.menu,
                sortedTrails,
                { it.guiSlot },
                startSlot = 10
            ) { trailConfig, slot ->
                drawTrail(trailConfig, slot)
            }
        }

        // Botón de Quitar Estela cargado dinámicamente desde HOCON
        val clearBtnConfig = selectorConfig.menu.items["clear"] ?: MenuItemConfig(
            slots = listOf(22),
            item = ItemConfig(
                material = "BARRIER",
                displayName = "",
                lore = emptyList()
            ),
            action = "clear",
            sound = "BLOCK_LAVA_EXTINGUISH"
        )

        val clearSlot = clearBtnConfig.slots.firstOrNull() ?: 22

        val clearDisplayName = displayConfig.clearName
        val clearLore = displayConfig.clearLore

        val clearItem = itemBuilderFactory.builder(clearBtnConfig.item.copy(displayName = clearDisplayName, lore = clearLore)).build()

        val clearBtn = Button.builder()
            .icon(clearItem)
            .onClick { p ->
                if (activeTrail == null) {
                    p.playSound(p.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f)
                    return@onClick
                }

                trailManager.savePlayerTrail(p.uniqueId, null).thenRun {
                    val soundEnum = try {
                        val snd = clearBtnConfig.sound
                        if (snd != null) Sound.valueOf(snd.uppercase()) else Sound.BLOCK_LAVA_EXTINGUISH
                    } catch (e: Exception) {
                        Sound.BLOCK_LAVA_EXTINGUISH
                    }
                    p.playSound(p.location, soundEnum, 1.0f, 1.5f)
                    messageService.send(p, TrailMessages.REMOVED)
                    // Reabrir regionalmente
                    regionTaskScheduler.runAtLocation(p.location, Runnable {
                        openTrailSelector(p)
                    })
                }
            }
            .build()
        builder.button(clearSlot, clearBtn)

        builder.build().open(player)
    }
}
