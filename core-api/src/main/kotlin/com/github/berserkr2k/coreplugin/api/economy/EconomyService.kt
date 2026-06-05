package com.github.berserkr2k.coreplugin.api.economy

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
    
    fun isCached(uuid: UUID): Boolean
    fun depositCacheBehind(uuid: UUID, currencyId: String, amount: BigDecimal, type: String): CompletableFuture<Boolean>
    fun withdrawCacheBehind(uuid: UUID, currencyId: String, amount: BigDecimal, type: String): CompletableFuture<Boolean>
}
