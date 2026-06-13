package com.github.berserkr2k.coreplugin.infrastructure.regions.gui

import com.github.berserkr2k.coreplugin.api.framework.menu.*
import com.github.berserkr2k.coreplugin.api.framework.item.*
import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.infrastructure.regions.service.RegionManager
import com.github.berserkr2k.coreplugin.api.framework.regions.RegionFlags
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import com.github.berserkr2k.coreplugin.infrastructure.regions.RegionMessages
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.inventory.ItemStack
import java.util.ArrayList

class RegionFlagEditorGui(
    private val plugin: Plugin,
    private val regionManager: RegionManager,
    private val serviceRegistry: ServiceRegistry
) {
    private val regionTaskScheduler = serviceRegistry.get(RegionTaskScheduler::class.java)!!
    private val menuService = serviceRegistry.get(MenuService::class.java)!!
    private val itemBuilderFactory = serviceRegistry.get(ItemBuilderFactory::class.java)!!
    private val messageService = serviceRegistry.get(MessageService::class.java)!!

    /**
     * Abre el menú principal de selección de categorías para la región especificada.
     */
    fun openCategorySelection(player: Player, regionId: String) {
        val dto = regionManager.getRegionDTO(regionId)
        if (dto == null) {
            messageService.send(player, RegionMessages.REGION_NOT_FOUND, PlaceholderContext.of("id" to regionId))
            return
        }

        val builder = menuService.createBuilder()
            .title(ColorUtility.parse("<gold><bold>Editor: ${dto.id}</bold></gold>"))
            .slots(27)

        // Rellenar fondo decorativo
        val filler = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val fillerMeta = filler.itemMeta
        fillerMeta?.displayName(ColorUtility.parse(" "))
        filler.itemMeta = fillerMeta
        
        val fillerButton = Button.builder().icon(filler).build()
        builder.fill(fillerButton)

        // Colocar las 6 categorías
        val slots = listOf(10, 11, 12, 13, 14, 15)
        RegionFlags.CATEGORIES.forEachIndexed { index, category ->
            if (index >= slots.size) return@forEachIndexed
            val slot = slots[index]

            val iconMat = try {
                Material.valueOf(category.iconMaterial)
            } catch (e: Exception) {
                Material.BOOK
            }

            val item = itemBuilderFactory.builder(iconMat)
                .displayName("<yellow><bold>${category.displayName}</bold></yellow>")
                .lore(
                    listOf(
                        category.description,
                        "",
                        "<yellow>⚡ Click para configurar banderas</yellow>"
                    )
                )
                .build()

            val btn = Button.builder()
                .icon(item)
                .onClick { p ->
                    p.playSound(p.location, Sound.UI_BUTTON_CLICK, 1.0f, 1.0f)
                    regionTaskScheduler.runAtLocation(p.location) {
                        openFlagsList(p, dto.id, category.id)
                    }
                }
                .build()
            builder.button(slot, btn)
        }

        // Botón de salir
        val closeItem = itemBuilderFactory.builder(Material.BARRIER)
            .displayName("<red><bold>❌ Cerrar</bold></red>")
            .build()
        
        val closeBtn = Button.builder()
            .icon(closeItem)
            .onClick { p ->
                p.playSound(p.location, Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.0f)
                p.closeInventory()
            }
            .build()
        builder.button(22, closeBtn)

        builder.build().open(player)
    }

    /**
     * Abre el menú de edición de banderas para una categoría específica de la región.
     */
    fun openFlagsList(player: Player, regionId: String, categoryId: String) {
        val dto = regionManager.getRegionDTO(regionId)
        if (dto == null) {
            messageService.send(player, RegionMessages.REGION_NOT_FOUND, PlaceholderContext.of("id" to regionId))
            return
        }

        val category = RegionFlags.CATEGORIES.firstOrNull { it.id == categoryId } ?: return

        val builder = menuService.createBuilder()
            .title(ColorUtility.parse("<gold><bold>${category.displayName} - ${dto.id}</bold></gold>"))
            .slots(45)

        // Rellenar fondo decorativo
        val filler = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val fillerMeta = filler.itemMeta
        fillerMeta?.displayName(ColorUtility.parse(" "))
        filler.itemMeta = fillerMeta
        
        val fillerButton = Button.builder().icon(filler).build()
        builder.fill(fillerButton)

        val flagsInCategory = RegionFlags.ALL_FLAGS.filter { it.categoryId == categoryId }
        val flagSlots = listOf(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
        )

        flagsInCategory.forEachIndexed { index, flag ->
            if (index >= flagSlots.size) return@forEachIndexed
            val slot = flagSlots[index]

            val state = when {
                dto.allowFlags.contains(flag.name) -> "ALLOW"
                dto.denyFlags.contains(flag.name) -> "DENY"
                else -> "DEFAULT"
            }

            val iconMat = when (state) {
                "ALLOW" -> Material.LIME_WOOL
                "DENY" -> Material.RED_WOOL
                else -> Material.LIGHT_GRAY_WOOL
            }

            val stateText = when (state) {
                "ALLOW" -> "<green><bold>ALLOW (Permitir)</bold></green>"
                "DENY" -> "<red><bold>DENY (Denegar)</bold></red>"
                else -> "<gray>DEFAULT (Heredar)</gray>"
            }

            val nextStateText = when (state) {
                "ALLOW" -> "<red>DENY (Denegar)</red>"
                "DENY" -> "<gray>DEFAULT (Heredar)</gray>"
                else -> "<green>ALLOW (Permitir)</green>"
            }

            val item = itemBuilderFactory.builder(iconMat)
                .displayName("<yellow><bold>${flag.displayName}</bold></yellow>")
                .lore(
                    listOf(
                        "<gray>Bandera: <white>${flag.name}</white></gray>",
                        "<gray>Estado: $stateText</gray>",
                        "",
                        flag.description,
                        "",
                        "<yellow>⚡ Click para cambiar a: $nextStateText</yellow>"
                    )
                )
                .build()

            val btn = Button.builder()
                .icon(item)
                .onClick { p ->
                    p.playSound(p.location, Sound.UI_BUTTON_CLICK, 1.0f, 1.2f)

                    // Calcular el siguiente estado
                    val nextState = when (state) {
                        "ALLOW" -> "deny"
                        "DENY" -> "remove"
                        else -> "allow"
                    }

                    // Aplicar el cambio
                    val updatedAllow = ArrayList(dto.allowFlags)
                    val updatedDeny = ArrayList(dto.denyFlags)

                    when (nextState) {
                        "allow" -> {
                            if (!updatedAllow.contains(flag.name)) updatedAllow.add(flag.name)
                            updatedDeny.remove(flag.name)
                        }
                        "deny" -> {
                            if (!updatedDeny.contains(flag.name)) updatedDeny.add(flag.name)
                            updatedAllow.remove(flag.name)
                        }
                        "remove" -> {
                            updatedAllow.remove(flag.name)
                            updatedDeny.remove(flag.name)
                        }
                    }

                    val updatedDto = dto.copy(allowFlags = updatedAllow, denyFlags = updatedDeny)

                    regionManager.createRegion(updatedDto).thenRun {
                        regionTaskScheduler.runAtLocation(p.location) {
                            openFlagsList(p, regionId, categoryId)
                        }
                    }
                }
                .build()
            builder.button(slot, btn)
        }

        // Botón de Volver
        val backItem = itemBuilderFactory.builder(Material.ARROW)
            .displayName("<yellow><bold>← Volver</bold></yellow>")
            .build()
        
        val backBtn = Button.builder()
            .icon(backItem)
            .onClick { p ->
                p.playSound(p.location, Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f)
                regionTaskScheduler.runAtLocation(p.location) {
                    openCategorySelection(p, regionId)
                }
            }
            .build()
        builder.button(40, backBtn)

        builder.build().open(player)
    }
}
