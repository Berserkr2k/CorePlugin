package com.github.berserkr2k.coreplugin.api.core.lifecycle

enum class ReloadSupport {
    NONE,
    CONFIG_ONLY,
    FULL
}

data class FeatureDescriptor(
    val id: String,
    val version: String = "1.0.0",
    val apiVersion: Int = 1,
    val dependencies: Set<String> = emptySet(),
    val optionalDependencies: Set<String> = emptySet(),
    val reloadSupport: ReloadSupport = ReloadSupport.FULL,
    val provides: Set<Class<*>> = emptySet()
)

enum class FeatureState {
    DISCOVERED,
    RESOLVING_DEPENDENCIES,
    LOADING_CONFIG,
    VALIDATING,
    REGISTERING_SERVICES,
    ENABLED,
    DISABLED,
    FAILED
}

data class FeatureStatus(
    val id: String,
    val state: FeatureState,
    val startupTimeMs: Long = 0L,
    val error: Throwable? = null
)
