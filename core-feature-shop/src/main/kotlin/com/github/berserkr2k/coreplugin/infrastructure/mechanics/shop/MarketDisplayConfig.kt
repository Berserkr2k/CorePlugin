package com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop

import com.github.berserkr2k.coreplugin.api.core.config.ConfigDefinition
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MarketDisplayConfig(
    val schemaVersion: Int = 1,
    val backItemMaterial: String = "BARRIER",
    val backItemName: String = "<red>Volver</red>",
    val backItemLore: List<String> = listOf("<gray>Haz clic para regresar</gray>"),
    
    val buyPriceFormat: String = "<gray>Precio Compra: <green><price></green>",
    val buyTaxFormat: String = "<dark_gray>  (IVA incl.: <tax>)</dark_gray>",
    val buyDisabled: String = "<red>No se puede comprar</red>",
    
    val sellPriceFormat: String = "<gray>Precio Venta:  <red><price></red>",
    val sellTaxFormat: String = "<dark_gray>  (IVA incl.: <tax>)</dark_gray>",
    val sellDisabled: String = "<red>No se puede vender</red>",
    
    val loreSeparator: String = "<gray>──────────────────────────────</gray>",
    val loreVolume: String = "<gray>Volumen (24h): <gold><volume> uds.</gold>",
    val loreTrend: String = "<gray>Tendencia:     <trend>",
    val loreClick: String = "<yellow>▶ Haz clic para transaccionar</yellow>",
    
    val trendUp: String = "<green>▲ Al alza</green>",
    val trendDown: String = "<red>▼ A la baja</red>",
    val trendStable: String = "<gray>■ Estable</gray>",
    
    val historyItemBuyFormat: String = "<green><bold>✔ COMPRA</bold></green> <yellow><material></yellow>",
    val historyItemSellFormat: String = "<red><bold>▼ VENTA</bold></red> <yellow><material></yellow>",
    val historyItemLoreFormat: List<String> = listOf(
        "<gray>Categoría: <white><category></white></gray>",
        "<gray>Cantidad: <yellow>x<quantity></yellow></gray>",
        "<gray>Total: <aqua><total></aqua></gray>",
        "<gray>Fecha: <white><date></white></gray>"
    ),
    val historyDateFormat: String = "dd/MM/yyyy HH:mm:ss"
)

object MarketDisplayConfigDefinition : ConfigDefinition<MarketDisplayConfig> {
    override val fileName = "display.conf"
    override val schemaVersion = 1
    override val configType = MarketDisplayConfig::class.java
}
