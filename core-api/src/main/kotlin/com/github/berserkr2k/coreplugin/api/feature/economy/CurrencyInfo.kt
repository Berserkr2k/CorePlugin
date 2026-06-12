package com.github.berserkr2k.coreplugin.api.feature.economy

interface CurrencyInfo {
    val id: String
    val displayName: String
    val symbol: String?
    val format: String
    val isDecimal: Boolean
    val maxDecimal: String?
    val initialBalance: String
    val maxBalance: String
    val permissionRequired: String?
    val p2pEnabled: Boolean
    val minTransfer: String
    val exchangeRates: Map<String, Double>
    val dbColumn: String
    val crossServer: Boolean
    val commands: List<String>
}
