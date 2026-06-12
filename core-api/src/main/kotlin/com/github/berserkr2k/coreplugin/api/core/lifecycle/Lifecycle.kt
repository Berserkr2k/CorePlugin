package com.github.berserkr2k.coreplugin.api.core.lifecycle

import java.util.concurrent.CompletableFuture

enum class LifecyclePhase {
    LOAD,       // Loading configs and registry setup
    INIT,       // DB setup and service registering
    POST_INIT,  // Internal event listeners setup
    START,      // Tasks scheduling, enable commands
    SHUTDOWN    // Sync locks release, cache saving
}

interface LifecycleListener {
    fun onPhase(phase: LifecyclePhase)
}

interface Reloadable {
    suspend fun reload()
}

interface FeatureModule {
    val id: String
    val dependencies: Set<String>
    
    fun onLoad()
    fun onEnable()
    fun onDisable()
}
