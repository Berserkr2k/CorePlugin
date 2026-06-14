package com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop

import com.github.berserkr2k.coreplugin.api.core.config.ConfigDefinition
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MarketConfig(
    val schemaVersion: Int = 1,
    val currencyId: String = "credits",
    val taxRate: Double = 0.16, // 16% IVA / IGV
    val purgeIntervalMinutes: Int = 5,
    val historyWindowHours: Int = 24,
    val defaultSaturation: Int = 5000,
    val defaultPriceFloorPercent: Double = 10.0, // 10% de P_base
    val defaultPriceCeilingPercent: Double = 500.0, // 500% de P_base
    val defaultSpread: Double = 0.15, // 15% spread para venta
    val blockedSellPdcKeys: List<String> = listOf("core:kit_item")
)

object MarketConfigDefinition : ConfigDefinition<MarketConfig> {
    override val fileName = "config.conf"
    override val schemaVersion = 1
    override val configType = MarketConfig::class.java
}
