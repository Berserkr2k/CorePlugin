package com.github.berserkr2k.coreplugin.infrastructure.economy

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import com.github.berserkr2k.coreplugin.api.feature.economy.CurrencyInfo

@ConfigSerializable
data class CurrencyConfig(
    override val id: String = "credits",
    override val displayName: String = "Créditos",
    override val symbol: String? = "$",
    override val format: String = "%symbol%%amount%",
    override val isDecimal: Boolean = true,
    override val maxDecimal: String? = "##",
    override val initialBalance: String = "100.0",
    override val maxBalance: String = "10000000.0",
    override val permissionRequired: String? = null,
    override val p2pEnabled: Boolean = true,
    override val minTransfer: String = "1.0",
    override val exchangeRates: Map<String, Double> = emptyMap(),
    override val dbColumn: String = "balance_credits",
    override val crossServer: Boolean = false,
    override val commands: List<String> = listOf("money", "coins")
) : CurrencyInfo
