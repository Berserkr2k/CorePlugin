package com.github.berserkr2k.coreplugin.api.core.lifecycle

interface ReloadCoordinator {
    fun register(featureId: String, reloadable: Reloadable)
    suspend fun reloadFeature(featureId: String): Long
    suspend fun reloadAll(): Map<String, Long>
}
