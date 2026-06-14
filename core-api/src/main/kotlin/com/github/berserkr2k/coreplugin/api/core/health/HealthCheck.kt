package com.github.berserkr2k.coreplugin.api.core.health

enum class HealthStatus {
    HEALTHY,
    UNHEALTHY,
    DEGRADED
}

data class HealthResult(
    val status: HealthStatus,
    val details: String = "",
    val error: Throwable? = null
)

interface HealthCheck {
    val name: String
    fun check(): HealthResult
}

interface HealthCheckRegistry {
    fun register(healthCheck: HealthCheck)
}
