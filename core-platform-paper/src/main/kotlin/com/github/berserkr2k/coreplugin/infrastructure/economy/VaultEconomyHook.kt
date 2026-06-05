package com.github.berserkr2k.coreplugin.infrastructure.economy

import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.math.BigDecimal
import java.util.UUID
import com.github.berserkr2k.coreplugin.api.economy.EconomyService
import com.github.berserkr2k.coreplugin.api.economy.CurrencyInfo

class VaultEconomyHook(private val economyService: EconomyService) : Economy {

    private val primaryCurrencyId: String
        get() = if (economyService.currencies.containsKey("credits")) "credits" else economyService.currencies.keys.firstOrNull() ?: "credits"

    private val currency: CurrencyInfo
        get() = economyService.currencies[primaryCurrencyId] ?: DummyCurrencyInfo(primaryCurrencyId)

    override fun isEnabled(): Boolean = true

    override fun getName(): String = "CorePlugin-Economy"

    override fun hasBankSupport(): Boolean = false

    override fun fractionalDigits(): Int = if (currency.isDecimal) 2 else 0

    override fun currencyNamePlural(): String = currency.displayName

    override fun currencyNameSingular(): String = currency.displayName

    override fun format(amount: Double): String {
        if (amount.isNaN() || amount.isInfinite()) return "0.0"
        return economyService.formatBalance(primaryCurrencyId, BigDecimal.valueOf(amount))
    }

    private fun resolveUUID(playerName: String): UUID {
        val player = Bukkit.getPlayerExact(playerName)
        return player?.uniqueId ?: Bukkit.getOfflinePlayer(playerName).uniqueId
    }

    private fun withdraw(uuid: UUID, amount: BigDecimal): EconomyResponse {
        if (amount <= BigDecimal.ZERO) {
            val bal = economyService.getBalance(uuid, primaryCurrencyId)
            return EconomyResponse(0.0, bal.toDouble(), EconomyResponse.ResponseType.FAILURE, "Monto inválido.")
        }

        val isOnline = economyService.isCached(uuid)
        val future = economyService.withdrawCacheBehind(uuid, primaryCurrencyId, amount, "VAULT_LEGACY_WITHDRAW")

        return if (isOnline) {
            // Write-Behind: si ya falló en memoria (completado con false), retornamos falla
            val success = !future.isDone || future.getNow(true) == true
            val currentBalance = economyService.getBalance(uuid, primaryCurrencyId)
            if (success) {
                EconomyResponse(amount.toDouble(), currentBalance.toDouble(), EconomyResponse.ResponseType.SUCCESS, null)
            } else {
                EconomyResponse(0.0, currentBalance.toDouble(), EconomyResponse.ResponseType.FAILURE, "Fondos insuficientes.")
            }
        } else {
            // Offline: bloqueo seguro en base de datos
            val success = future.join()
            val currentBalance = economyService.getBalance(uuid, primaryCurrencyId)
            if (success) {
                EconomyResponse(amount.toDouble(), currentBalance.toDouble(), EconomyResponse.ResponseType.SUCCESS, null)
            } else {
                EconomyResponse(0.0, currentBalance.toDouble(), EconomyResponse.ResponseType.FAILURE, "Fallo en la transacción.")
            }
        }
    }

    private fun deposit(uuid: UUID, amount: BigDecimal): EconomyResponse {
        if (amount <= BigDecimal.ZERO) {
            val bal = economyService.getBalance(uuid, primaryCurrencyId)
            return EconomyResponse(0.0, bal.toDouble(), EconomyResponse.ResponseType.FAILURE, "Monto inválido.")
        }

        val maxBal = BigDecimal(currency.maxBalance)
        val currentBalance = economyService.getBalance(uuid, primaryCurrencyId)
        if (currentBalance.add(amount) > maxBal) {
            return EconomyResponse(0.0, currentBalance.toDouble(), EconomyResponse.ResponseType.FAILURE, "Supera el límite máximo de la cuenta.")
        }

        val isOnline = economyService.isCached(uuid)
        val future = economyService.depositCacheBehind(uuid, primaryCurrencyId, amount, "VAULT_LEGACY_DEPOSIT")

        return if (isOnline) {
            // Write-Behind: si ya falló en memoria (completado con false), retornamos falla
            val success = !future.isDone || future.getNow(true) == true
            val newBalance = economyService.getBalance(uuid, primaryCurrencyId)
            if (success) {
                EconomyResponse(amount.toDouble(), newBalance.toDouble(), EconomyResponse.ResponseType.SUCCESS, null)
            } else {
                EconomyResponse(0.0, currentBalance.toDouble(), EconomyResponse.ResponseType.FAILURE, "Fallo al realizar el depósito.")
            }
        } else {
            // Offline: bloqueo seguro en base de datos
            val success = future.join()
            val newBalance = economyService.getBalance(uuid, primaryCurrencyId)
            if (success) {
                EconomyResponse(amount.toDouble(), newBalance.toDouble(), EconomyResponse.ResponseType.SUCCESS, null)
            } else {
                EconomyResponse(0.0, currentBalance.toDouble(), EconomyResponse.ResponseType.FAILURE, "Fallo al realizar el depósito.")
            }
        }
    }

    // ==========================================
    // METODOS DE COMPATIBILIDAD HEREDADOS (DOUBLE & NAMES)
    // ==========================================

    override fun hasAccount(playerName: String): Boolean = true

    override fun hasAccount(offlinePlayer: OfflinePlayer): Boolean = true

    override fun hasAccount(playerName: String, worldName: String?): Boolean = true

    override fun hasAccount(offlinePlayer: OfflinePlayer, worldName: String?): Boolean = true

    override fun getBalance(playerName: String): Double {
        return economyService.getBalance(resolveUUID(playerName), primaryCurrencyId).toDouble()
    }

    override fun getBalance(offlinePlayer: OfflinePlayer): Double {
        return economyService.getBalance(offlinePlayer.uniqueId, primaryCurrencyId).toDouble()
    }

    override fun getBalance(playerName: String, world: String?): Double = getBalance(playerName)

    override fun getBalance(offlinePlayer: OfflinePlayer, world: String?): Double = getBalance(offlinePlayer)

    override fun has(playerName: String, amount: Double): Boolean = getBalance(playerName) >= amount

    override fun has(offlinePlayer: OfflinePlayer, amount: Double): Boolean = getBalance(offlinePlayer) >= amount

    override fun has(playerName: String, worldName: String?, amount: Double): Boolean = has(playerName, amount)

    override fun has(offlinePlayer: OfflinePlayer, worldName: String?, amount: Double): Boolean = has(offlinePlayer, amount)

    override fun withdrawPlayer(playerName: String, amount: Double): EconomyResponse {
        return withdraw(resolveUUID(playerName), BigDecimal.valueOf(amount))
    }

    override fun withdrawPlayer(offlinePlayer: OfflinePlayer, amount: Double): EconomyResponse {
        return withdraw(offlinePlayer.uniqueId, BigDecimal.valueOf(amount))
    }

    override fun withdrawPlayer(playerName: String, worldName: String?, amount: Double): EconomyResponse = withdrawPlayer(playerName, amount)

    override fun withdrawPlayer(offlinePlayer: OfflinePlayer, worldName: String?, amount: Double): EconomyResponse = withdrawPlayer(offlinePlayer, amount)

    override fun depositPlayer(playerName: String, amount: Double): EconomyResponse {
        return deposit(resolveUUID(playerName), BigDecimal.valueOf(amount))
    }

    override fun depositPlayer(offlinePlayer: OfflinePlayer, amount: Double): EconomyResponse {
        return deposit(offlinePlayer.uniqueId, BigDecimal.valueOf(amount))
    }

    override fun depositPlayer(playerName: String, worldName: String?, amount: Double): EconomyResponse = depositPlayer(playerName, amount)

    override fun depositPlayer(offlinePlayer: OfflinePlayer, worldName: String?, amount: Double): EconomyResponse = depositPlayer(offlinePlayer, amount)

    // Bancos no están soportados (Legacy Vault feature)
    override fun createBank(name: String?, player: String?): EconomyResponse = EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos no soportados.")
    override fun createBank(name: String?, player: OfflinePlayer?): EconomyResponse = EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos no soportados.")
    override fun deleteBank(name: String?): EconomyResponse = EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos no soportados.")
    override fun bankBalance(name: String?): EconomyResponse = EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos no soportados.")
    override fun bankHas(name: String?, amount: Double): EconomyResponse = EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos no soportados.")
    override fun bankWithdraw(name: String?, amount: Double): EconomyResponse = EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos no soportados.")
    override fun bankDeposit(name: String?, amount: Double): EconomyResponse = EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos no soportados.")
    override fun isBankOwner(name: String?, playerName: String?): EconomyResponse = EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos no soportados.")
    override fun isBankOwner(name: String?, player: OfflinePlayer?): EconomyResponse = EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos no soportados.")
    override fun isBankMember(name: String?, playerName: String?): EconomyResponse = EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos no soportados.")
    override fun isBankMember(name: String?, player: OfflinePlayer?): EconomyResponse = EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos no soportados.")
    override fun getBanks(): List<String> = emptyList()

    override fun createPlayerAccount(playerName: String): Boolean = true
    override fun createPlayerAccount(offlinePlayer: OfflinePlayer): Boolean = true
    override fun createPlayerAccount(playerName: String, worldName: String?): Boolean = true
    override fun createPlayerAccount(offlinePlayer: OfflinePlayer, worldName: String?): Boolean = true
}
