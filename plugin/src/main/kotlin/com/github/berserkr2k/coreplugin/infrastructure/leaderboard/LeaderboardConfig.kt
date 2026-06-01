package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class PersistedPodium(
    val world: String = "world",
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val yaw: Float = 0.0f,
    val pitch: Float = 0.0f
)

@ConfigSerializable
data class CustomLeaderboardConfig(
    val id: String = "credits",
    val placeholder: String = "%coreplugin_balance_credits%",
    val updateIntervalSeconds: Int = 300,
    val displayName: String = "<gold><bold>TOP CRÉDITOS</bold></gold>",
    val headerAboveRank: Int = 1,
    val header: String = "<gold><bold>★ CLASIFICACIÓN DE %top_id% ★</bold></gold>",
    val formats: Map<String, String> = mapOf(
        "1" to "<yellow>★ #1</yellow> <white><player></white> » <gold>$<balance></gold>",
        "2" to "<gray>#2</gray> <white><player></white> » <white>$<balance></white>",
        "3" to "<gold>#3</gold> <white><player></white> » <yellow>$<balance></yellow>",
        "default" to "<yellow>#<pos></yellow> <gray><player></gray> » <green>$<balance></green>"
    ),
    val podiums: Map<String, PersistedPodium> = emptyMap()
)
