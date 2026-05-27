package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class LeaderboardConfig(
    val positions: Map<String, PersistedLeaderboard> = emptyMap()
) {
    @ConfigSerializable
    data class PersistedLeaderboard(
        val world: String = "world",
        val x: Double = 0.0,
        val y: Double = 0.0,
        val z: Double = 0.0,
        val yaw: Float = 0.0f,
        val pitch: Float = 0.0f
    )
}
