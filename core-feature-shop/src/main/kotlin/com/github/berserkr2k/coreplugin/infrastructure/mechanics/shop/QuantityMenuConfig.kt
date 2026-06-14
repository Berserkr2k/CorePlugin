package com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop

import com.github.berserkr2k.coreplugin.api.core.config.ConfigDefinition
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class QuantityMenuConfig(
    val schemaVersion: Int = 1,
    val title: String = "<dark_gray>Transacción Simétrica</dark_gray>",
    val backgroundMaterial: String = "GRAY_STAINED_GLASS_PANE",
    val divisorMaterial: String = "BLACK_STAINED_GLASS_PANE",
    val disabledMaterial: String = "RED_STAINED_GLASS_PANE",
    
    val buyQtyMaterial: String = "GREEN_STAINED_GLASS_PANE",
    val buyQtyName: String = "<green><bold>Comprar <qty></bold></green>",
    val buyQtyLore: List<String> = listOf(
        "<gray>Compra una cantidad de <qty> unidades.</gray>",
        "",
        "<gray>Costo Total: <green><price></green></gray>",
        "",
        "<yellow>▶ Haz clic para comprar</yellow>"
    ),
    
    val sellQtyMaterial: String = "RED_STAINED_GLASS_PANE",
    val sellQtyName: String = "<red><bold>Vender <qty></bold></red>",
    val sellQtyLore: List<String> = listOf(
        "<gray>Vende una cantidad de <qty> unidades.</gray>",
        "",
        "<gray>Valor Total: <red><price></red></gray>",
        "",
        "<yellow>▶ Haz clic para vender</yellow>"
    ),
    
    val buyMaxMaterial: String = "EMERALD_BLOCK",
    val buyMaxName: String = "<green><bold>Comprar Máximo</bold></green>",
    val buyMaxLore: List<String> = listOf(
        "<gray>Llena tu inventario con este ítem.</gray>",
        "",
        "<gray>Cantidad a comprar: <gold><qty> uds.</gold></gray>",
        "<gray>Costo Estimado:    <green><price></green></gray>",
        "",
        "<yellow>▶ Haz clic para comprar</yellow>"
    ),
    
    val sellAllMaterial: String = "REDSTONE_BLOCK",
    val sellAllName: String = "<red><bold>Vender Todo</bold></red>",
    val sellAllLore: List<String> = listOf(
        "<gray>Vacía tu inventario de este ítem.</gray>",
        "",
        "<gray>Cantidad a vender: <gold><qty> uds.</gold></gray>",
        "<gray>Valor Estimado:    <red><price></red></gray>",
        "",
        "<yellow>▶ Haz clic para vender</yellow>"
    ),
    
    val buyDisabledName: String = "<red>Compra Deshabilitada</red>",
    val sellDisabledName: String = "<red>Venta Deshabilitada</red>"
)

object QuantityMenuConfigDefinition : ConfigDefinition<QuantityMenuConfig> {
    override val fileName = "menus/quantity.conf"
    override val schemaVersion = 1
    override val configType = QuantityMenuConfig::class.java
}
