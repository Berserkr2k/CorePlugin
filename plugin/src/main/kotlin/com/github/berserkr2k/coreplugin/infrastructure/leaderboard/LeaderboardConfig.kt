package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class LeaderboardConfig(
    val positions: Map<String, PersistedLeaderboard> = emptyMap(),
    val settings: Map<String, LeaderboardSettings> = mapOf(
        "economy" to LeaderboardSettings(placeholder = "%vault_eco_balance%"),
        "kills" to LeaderboardSettings(placeholder = "%statistic_player_kills%")
    )
) {
    @ConfigSerializable
    data class PersistedLeaderboard(
        val leaderboardId: String = "economy",
        val rank: Int = 1,
        val world: String = "world",
        val x: Double = 0.0,
        val y: Double = 0.0,
        val z: Double = 0.0,
        val yaw: Float = 0.0f,
        val pitch: Float = 0.0f
    )

    @ConfigSerializable
    data class LeaderboardSettings(
        val placeholder: String = "",
        val updateIntervalSeconds: Int = 300
    )
}
