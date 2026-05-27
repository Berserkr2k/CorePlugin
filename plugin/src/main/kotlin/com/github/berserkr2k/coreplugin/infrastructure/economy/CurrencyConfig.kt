package com.github.berserkr2k.coreplugin.infrastructure.economy

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class CurrencyConfig(
    val id: String = "credits",
    val displayName: String = "Créditos",
    val symbol: String? = "$",
    val format: String = "%symbol%%amount%",
    val isDecimal: Boolean = true,
    val maxDecimal: String? = "##",
    val initialBalance: String = "100.0",
    val maxBalance: String = "10000000.0",
    val permissionRequired: String? = null,
    val p2pEnabled: Boolean = true,
    val minTransfer: String = "1.0",
    val exchangeRates: Map<String, Double> = emptyMap(),
    val dbColumn: String = "balance_credits",
    val crossServer: Boolean = false,
    val commands: List<String> = listOf("money", "coins")
)
