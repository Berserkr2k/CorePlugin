package com.github.berserkr2k.coreplugin.api.framework.economy

import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.CompletableFuture

interface EconomyService {
    val currencies: Map<String, CurrencyInfo>
    fun getBalance(uuid: UUID, currencyId: String): BigDecimal
    fun deposit(uuid: UUID, amount: BigDecimal, currencyId: String): CompletableFuture<Boolean>
    fun withdraw(uuid: UUID, amount: BigDecimal, currencyId: String): CompletableFuture<Boolean>
    fun hasAccount(uuid: UUID): Boolean
    fun formatBalance(currencyId: String, amount: BigDecimal): String
}
