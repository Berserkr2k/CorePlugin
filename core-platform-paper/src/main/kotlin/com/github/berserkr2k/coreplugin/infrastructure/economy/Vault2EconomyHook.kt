package com.github.berserkr2k.coreplugin.infrastructure.economy

import net.milkbowl.vault2.economy.Economy
import net.milkbowl.vault2.economy.EconomyResponse
import net.milkbowl.vault2.economy.AccountPermission
import org.bukkit.Bukkit
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID
import com.github.berserkr2k.coreplugin.api.economy.EconomyService
import com.github.berserkr2k.coreplugin.api.economy.CurrencyInfo

class DummyCurrencyInfo(override val id: String) : CurrencyInfo {
    override val displayName: String = "Créditos"
    override val symbol: String? = "$"
    override val format: String = "%symbol%%amount%"
    override val isDecimal: Boolean = true
    override val maxDecimal: String? = "##"
    override val initialBalance: String = "100.0"
    override val maxBalance: String = "10000000.0"
    override val permissionRequired: String? = null
    override val p2pEnabled: Boolean = true
    override val minTransfer: String = "1.0"
    override val exchangeRates: Map<String, Double> = emptyMap()
    override val dbColumn: String = "balance_credits"
    override val crossServer: Boolean = false
    override val commands: List<String> = emptyList()
}

class Vault2EconomyHook(private val economyService: EconomyService) : Economy {

    private val primaryCurrencyId: String
        get() = if (economyService.currencies.containsKey("credits")) "credits" else economyService.currencies.keys.firstOrNull() ?: "credits"

    private val currency: CurrencyInfo
        get() = economyService.currencies[primaryCurrencyId] ?: DummyCurrencyInfo(primaryCurrencyId)

    override fun isEnabled(): Boolean = true

    override fun getName(): String = "CorePlugin-Economy2"

    override fun hasSharedAccountSupport(): Boolean = false

    override fun hasMultiCurrencySupport(): Boolean = true

    override fun fractionalDigits(pluginName: String): Int = if (currency.isDecimal) 2 else 0

    override fun fractionalDigits(pluginName: String, currency: String): Int = fractionalDigits(pluginName)

    @Deprecated("Deprecated in VaultUnlocked")
    override fun format(amount: BigDecimal): String {
        return economyService.formatBalance(primaryCurrencyId, amount)
    }

    override fun format(pluginName: String, amount: BigDecimal): String {
        return economyService.formatBalance(primaryCurrencyId, amount)
    }

    @Deprecated("Deprecated in VaultUnlocked")
    override fun format(amount: BigDecimal, currency: String): String {
        return format(amount)
    }

    override fun format(pluginName: String, amount: BigDecimal, currency: String): String {
        return format(pluginName, amount)
    }

    // ==========================================
    // METODOS NATIVOS DE VAULT2 (UUID & BigDecimal)
    // ==========================================

    override fun getBalance(pluginName: String, accountID: UUID): BigDecimal {
        return economyService.getBalance(accountID, primaryCurrencyId)
    }

    override fun getBalance(pluginName: String, accountID: UUID, world: String): BigDecimal {
        return getBalance(pluginName, accountID)
    }

    override fun getBalance(pluginName: String, accountID: UUID, world: String, currency: String): BigDecimal {
        val curId = if (economyService.currencies.containsKey(currency)) currency else primaryCurrencyId
        return economyService.getBalance(accountID, curId)
    }

    override fun hasAccount(accountID: UUID, worldName: String): Boolean = true

    override fun hasAccount(accountID: UUID): Boolean = true

    override fun has(pluginName: String, accountID: UUID, amount: BigDecimal): Boolean {
        return getBalance(pluginName, accountID) >= amount
    }

    override fun has(pluginName: String, accountID: UUID, worldName: String, amount: BigDecimal): Boolean {
        return has(pluginName, accountID, amount)
    }

    override fun has(pluginName: String, accountID: UUID, worldName: String, currency: String, amount: BigDecimal): Boolean {
        return getBalance(pluginName, accountID, worldName, currency) >= amount
    }

    override fun hasCurrency(currency: String): Boolean = economyService.currencies.containsKey(currency)

    override fun getDefaultCurrency(pluginName: String): String = primaryCurrencyId

    override fun defaultCurrencyNamePlural(pluginName: String): String = currency.displayName

    override fun defaultCurrencyNameSingular(pluginName: String): String = currency.displayName

    override fun currencies(): Collection<String> = economyService.currencies.keys

    override fun createAccount(accountID: UUID, name: String): Boolean = true

    override fun createAccount(accountID: UUID, name: String, player: Boolean): Boolean = true

    override fun createAccount(accountID: UUID, name: String, worldName: String): Boolean = true

    override fun createAccount(accountID: UUID, name: String, worldName: String, player: Boolean): Boolean = true

    override fun getUUIDNameMap(): Map<UUID, String> = emptyMap()

    override fun getAccountName(accountID: UUID): Optional<String> = Optional.ofNullable(Bukkit.getOfflinePlayer(accountID).name)

    override fun renameAccount(accountID: UUID, name: String): Boolean = false

    override fun renameAccount(plugin: String, accountID: UUID, name: String): Boolean = false

    override fun deleteAccount(plugin: String, accountID: UUID): Boolean = false

    override fun accountSupportsCurrency(plugin: String, accountID: UUID, currency: String): Boolean {
        return economyService.currencies.containsKey(currency)
    }

    override fun accountSupportsCurrency(plugin: String, accountID: UUID, currency: String, world: String): Boolean {
        return accountSupportsCurrency(plugin, accountID, currency)
    }

    private fun withdraw(uuid: UUID, amount: BigDecimal, currencyId: String): EconomyResponse {
        val curId = if (economyService.currencies.containsKey(currencyId)) currencyId else primaryCurrencyId
        
        if (amount <= BigDecimal.ZERO) {
            val bal = economyService.getBalance(uuid, curId)
            return EconomyResponse(BigDecimal.ZERO, bal, EconomyResponse.ResponseType.FAILURE, "Monto inválido.")
        }

        val isOnline = economyService.isCached(uuid)
        val future = economyService.withdrawCacheBehind(uuid, curId, amount, "VAULT2_WITHDRAW")

        return if (isOnline) {
            val success = !future.isDone || future.getNow(true) == true
            val currentBalance = economyService.getBalance(uuid, curId)
            if (success) {
                EconomyResponse(amount, currentBalance, EconomyResponse.ResponseType.SUCCESS, "")
            } else {
                EconomyResponse(BigDecimal.ZERO, currentBalance, EconomyResponse.ResponseType.FAILURE, "Fondos insuficientes.")
            }
        } else {
            val success = future.join()
            val currentBalance = economyService.getBalance(uuid, curId)
            if (success) {
                EconomyResponse(amount, currentBalance, EconomyResponse.ResponseType.SUCCESS, "")
            } else {
                EconomyResponse(BigDecimal.ZERO, currentBalance, EconomyResponse.ResponseType.FAILURE, "Fallo en la transacción.")
            }
        }
    }

    override fun withdraw(pluginName: String, accountID: UUID, amount: BigDecimal): EconomyResponse {
        return withdraw(accountID, amount, primaryCurrencyId)
    }

    override fun withdraw(pluginName: String, accountID: UUID, worldName: String, amount: BigDecimal): EconomyResponse {
        return withdraw(accountID, amount, primaryCurrencyId)
    }

    override fun withdraw(pluginName: String, accountID: UUID, worldName: String, currency: String, amount: BigDecimal): EconomyResponse {
        return withdraw(accountID, amount, currency)
    }

    private fun deposit(uuid: UUID, amount: BigDecimal, currencyId: String): EconomyResponse {
        val curId = if (economyService.currencies.containsKey(currencyId)) currencyId else primaryCurrencyId
        val curConfig = economyService.currencies[curId] ?: currency

        if (amount <= BigDecimal.ZERO) {
            val bal = economyService.getBalance(uuid, curId)
            return EconomyResponse(BigDecimal.ZERO, bal, EconomyResponse.ResponseType.FAILURE, "Monto inválido.")
        }

        val maxBal = BigDecimal(curConfig.maxBalance)
        val currentBalance = economyService.getBalance(uuid, curId)
        if (currentBalance.add(amount) > maxBal) {
            return EconomyResponse(BigDecimal.ZERO, currentBalance, EconomyResponse.ResponseType.FAILURE, "Supera el límite máximo de la cuenta.")
        }

        val isOnline = economyService.isCached(uuid)
        val future = economyService.depositCacheBehind(uuid, curId, amount, "VAULT2_DEPOSIT")

        return if (isOnline) {
            val success = !future.isDone || future.getNow(true) == true
            val newBalance = economyService.getBalance(uuid, curId)
            if (success) {
                EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "")
            } else {
                EconomyResponse(BigDecimal.ZERO, currentBalance, EconomyResponse.ResponseType.FAILURE, "Fallo al realizar el depósito.")
            }
        } else {
            val success = future.join()
            val newBalance = economyService.getBalance(uuid, curId)
            if (success) {
                EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "")
            } else {
                EconomyResponse(BigDecimal.ZERO, currentBalance, EconomyResponse.ResponseType.FAILURE, "Fallo al realizar el depósito.")
            }
        }
    }

    override fun deposit(pluginName: String, accountID: UUID, amount: BigDecimal): EconomyResponse {
        return deposit(accountID, amount, primaryCurrencyId)
    }

    override fun deposit(pluginName: String, accountID: UUID, worldName: String, amount: BigDecimal): EconomyResponse {
        return deposit(accountID, amount, primaryCurrencyId)
    }

    override fun deposit(pluginName: String, accountID: UUID, worldName: String, currency: String, amount: BigDecimal): EconomyResponse {
        return deposit(accountID, amount, currency)
    }

    override fun createSharedAccount(pluginName: String, accountID: UUID, name: String, owner: UUID): Boolean = false

    override fun isAccountOwner(pluginName: String, accountID: UUID, uuid: UUID): Boolean = uuid == accountID

    override fun setOwner(pluginName: String, accountID: UUID, uuid: UUID): Boolean = false

    override fun isAccountMember(pluginName: String, accountID: UUID, uuid: UUID): Boolean = uuid == accountID

    override fun addAccountMember(pluginName: String, accountID: UUID, uuid: UUID): Boolean = false

    override fun addAccountMember(pluginName: String, accountID: UUID, uuid: UUID, vararg initialPermissions: AccountPermission): Boolean = false

    override fun removeAccountMember(pluginName: String, accountID: UUID, uuid: UUID): Boolean = false

    override fun hasAccountPermission(pluginName: String, accountID: UUID, uuid: UUID, permission: AccountPermission): Boolean = uuid == accountID

    override fun updateAccountPermission(pluginName: String, accountID: UUID, uuid: UUID, permission: AccountPermission, value: Boolean): Boolean = false
}
