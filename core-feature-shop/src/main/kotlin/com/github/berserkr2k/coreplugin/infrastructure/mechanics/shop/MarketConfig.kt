package com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop

import com.github.berserkr2k.coreplugin.api.framework.menu.MenuConfig
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuItemConfig
import com.github.berserkr2k.coreplugin.api.framework.menu.FillerConfig
import com.github.berserkr2k.coreplugin.api.config.ItemConfig
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
    val historyItemBuyFormat: String = "<green><bold>✔ COMPRA</bold></green> <yellow><material></yellow>",
    val historyItemSellFormat: String = "<red><bold>▼ VENTA</bold></red> <yellow><material></yellow>",
    val historyItemLoreFormat: List<String> = listOf(
        "<gray>Categoría: <white><category></white></gray>",
        "<gray>Cantidad: <yellow>x<quantity></yellow></gray>",
        "<gray>Total: <aqua><total></aqua></gray>",
        "<gray>Fecha: <white><date></white></gray>"
    ),
    val historyDateFormat: String = "dd/MM/yyyy HH:mm:ss",
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
            ),
            "history" to MenuItemConfig(
                slots = listOf(22),
                item = ItemConfig(
                    material = "BOOK",
                    displayName = "<gray><bold>📜 Historial de Transacciones</bold></gray>",
                    lore = listOf(
                        "<gray>Ver tus compras y ventas</gray>",
                        "<gray>realizadas en los últimos 7 días.</gray>",
                        "",
                        "<yellow>▶ Haz clic para abrir historial</yellow>"
                    )
                ),
                action = "open_shop_history"
            )
        )
    ),
    val historyMenu: MenuConfig = MenuConfig(
        title = "<dark_gray><bold>Historial (7 días)</bold></dark_gray>",
        size = 54,
        paginated = true,
        dynamicSlots = (9..44).toList(),
        previousPageSlot = 45,
        nextPageSlot = 53,
        previousPageItem = ItemConfig(material = "ARROW", displayName = "<yellow>◀ Página Anterior</yellow>"),
        nextPageItem = ItemConfig(material = "ARROW", displayName = "<yellow>Siguiente Página ▶</yellow>"),
        filler = FillerConfig(
            enabled = true,
            item = ItemConfig(
                material = "GRAY_STAINED_GLASS_PANE",
                displayName = " "
            )
        )
    )
)
