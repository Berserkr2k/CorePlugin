package com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop

import com.github.berserkr2k.coreplugin.common.gui.MenuConfig
import com.github.berserkr2k.coreplugin.common.gui.MenuItemConfig
import com.github.berserkr2k.coreplugin.common.gui.FillerConfig
import com.github.berserkr2k.coreplugin.common.gui.ItemConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MarketConfig(
    val currencyId: String = "credits",
    val taxRate: Double = 0.16, // 16% IVA / IGV
    val purgeIntervalMinutes: Int = 5,
    val historyWindowHours: Int = 24,
    val defaultSaturation: Int = 5000,
    val defaultPriceFloorPercent: Double = 10.0, // 10% de P_base
    val defaultPriceCeilingPercent: Double = 500.0, // 500% de P_base
    val defaultSpread: Double = 0.15, // 15% spread para venta
    val blockedSellPdcKeys: List<String> = listOf("core:kit_item"),
    val categoriesMenu: MenuConfig = MenuConfig(
        title = "<gold><bold>Mercado Dinámico</bold></gold>",
        size = 27,
        filler = FillerConfig(
            enabled = true,
            item = ItemConfig(
                material = "GRAY_STAINED_GLASS_PANE",
                displayName = " "
            )
        ),
        items = mapOf(
            "blocks" to MenuItemConfig(
                slots = listOf(11),
                item = ItemConfig(
                    material = "BRICKS",
                    displayName = "<yellow><bold>Bloques de Construcción</bold></yellow>",
                    lore = listOf(
                        "<gray>Explora y comercia con bloques</gray>",
                        "<gray>estructurales y decorativos.</gray>",
                        "",
                        "<yellow>▶ Haz clic para abrir</yellow>"
                    )
                ),
                action = "open_shop_blocks"
            ),
            "food" to MenuItemConfig(
                slots = listOf(13),
                item = ItemConfig(
                    material = "COOKED_BEEF",
                    displayName = "<gold><bold>Alimentos y Comida</bold></gold>",
                    lore = listOf(
                        "<gray>Compra raciones de comida o vende</gray>",
                        "<gray>los excedentes de tus granjas.</gray>",
                        "",
                        "<yellow>▶ Haz clic para abrir</yellow>"
                    )
                ),
                action = "open_shop_food"
            ),
            "minerals" to MenuItemConfig(
                slots = listOf(15),
                item = ItemConfig(
                    material = "DIAMOND",
                    displayName = "<aqua><bold>Minerales y Materiales</bold></aqua>",
                    lore = listOf(
                        "<gray>El corazón de la economía.</gray>",
                        "<gray>Fluctuaciones constantes de precios.</gray>",
                        "",
                        "<yellow>▶ Haz clic para abrir</yellow>"
                    )
                ),
                action = "open_shop_minerals"
            )
        )
    )
)
