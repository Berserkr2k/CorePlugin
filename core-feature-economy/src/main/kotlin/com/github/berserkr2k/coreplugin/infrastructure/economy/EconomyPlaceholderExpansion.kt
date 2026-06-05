package com.github.berserkr2k.coreplugin.infrastructure.economy

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import java.math.BigDecimal

class EconomyPlaceholderExpansion(private val economyService: EconomyService) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "coreplugin"

    override fun getAuthor(): String = "Berserkr2K"

    override fun getVersion(): String = "1.0.0"

    override fun persist(): Boolean = true

    override fun canRegister(): Boolean = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (player == null) return null

        // Formatos: balance_formatted_<id> o balance_<id>
        if (params.startsWith("balance_")) {
            val isFormatted = params.startsWith("balance_formatted_")
            val currencyId = if (isFormatted) {
                params.substring("balance_formatted_".length)
            } else {
                params.substring("balance_".length)
            }

            val currency = economyService.currencies[currencyId] ?: return null
            val bal = try {
                economyService.getBalance(player.uniqueId, currency.id)
            } catch (e: Exception) {
                BigDecimal.ZERO
            }

            return if (isFormatted) {
                economyService.formatBalance(currency.id, bal)
            } else {
                bal.toPlainString()
            }
        }

        // Formato: symbol_<id>
        if (params.startsWith("symbol_")) {
            val currencyId = params.substring("symbol_".length)
            val currency = economyService.currencies[currencyId] ?: return null
            return currency.symbol ?: ""
        }

        return null
    }
}
