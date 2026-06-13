package com.github.berserkr2k.coreplugin.api.core.user

import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class UserProfile(
    val internalId: Int,
    val uuid: UUID,
    var username: String,
    val economies: ConcurrentHashMap<String, BigDecimal> = ConcurrentHashMap(),
    val kitCooldowns: ConcurrentHashMap<String, Long> = ConcurrentHashMap(),
    var activeTrailId: String? = null,
    var chatColor: String? = null,
    var socialSpy: Boolean = false,
    val featureData: ConcurrentHashMap<String, FeatureDataContainer> = ConcurrentHashMap()
) {
    @Volatile
    var isDirty: Boolean = false
        private set

    inline fun <reified T : FeatureDataContainer> getFeatureData(key: String): T? {
        return featureData[key.lowercase()] as? T
    }

    fun setFeatureData(key: String, data: FeatureDataContainer) {
        featureData[key.lowercase()] = data
        markDirty()
    }

    fun markDirty() {
        isDirty = true
    }

    fun clearDirty() {
        isDirty = false
    }

    fun setBalance(currencyId: String, amount: BigDecimal) {
        economies[currencyId.lowercase()] = amount
        markDirty()
    }

    fun getBalance(currencyId: String): BigDecimal {
        return economies[currencyId.lowercase()] ?: BigDecimal.ZERO
    }

    fun setCooldown(kitId: String, timestamp: Long) {
        kitCooldowns[kitId.lowercase()] = timestamp
        markDirty()
    }

    fun getCooldown(kitId: String): Long {
        return kitCooldowns[kitId.lowercase()] ?: 0L
    }

    fun setTrail(trailId: String?) {
        this.activeTrailId = trailId
        markDirty()
    }
}
