package com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import com.github.berserkr2k.coreplugin.infrastructure.config.ItemConfig

@ConfigSerializable
data class ShopItemConfig(
    val material: String = "STONE",
    val basePrice: String = "10.0", // BigDecimal string
    val priceFloorPercent: Double? = null,
    val priceCeilingPercent: Double? = null,
    val saturationConstant: Int? = null,
    val spread: Double? = null,
    val guiSlot: Int = -1, // -1 para posicionamiento automático
    val allowBuy: Boolean = true,
    val allowSell: Boolean = true,
    val customModelData: Int? = null,
    val enchantments: Map<String, Int> = emptyMap(),
    val displayName: String? = null,
    val lore: List<String> = emptyList()
)

@ConfigSerializable
data class ShopConfig(
    val shopId: String = "blocks",
    val displayName: String = "<yellow>Tienda de Bloques</yellow>",
    val guiSize: Int = 45,
    val backItemMaterial: String? = null,
    val backItemName: String? = null,
    val backItemLore: List<String> = emptyList(),
    val items: List<ShopItemConfig> = emptyList(),
    val paginated: Boolean = false,
    val dynamicSlots: List<Int> = emptyList(),
    val previousPageSlot: Int? = null,
    val nextPageSlot: Int? = null,
    val previousPageItem: ItemConfig = ItemConfig(material = "ARROW", displayName = "<yellow>Página Anterior</yellow>"),
    val nextPageItem: ItemConfig = ItemConfig(material = "ARROW", displayName = "<yellow>Siguiente Página</yellow>")
)
