package com.github.berserkr2k.coreplugin.infrastructure.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class UtilityConfig(
    val fly: FlySettings = FlySettings(),
    val anvil: AnvilSettings = AnvilSettings(),
    val bossbar: BossBarSettings = BossBarSettings()
) {
    @ConfigSerializable
    data class FlySettings(
        val allowedWorlds: List<String> = listOf("world", "world_nether", "world_the_end")
    )

    @ConfigSerializable
    data class AnvilSettings(
        val sound: String = "BLOCK_ANVIL_PLACE"
    )

    @ConfigSerializable
    data class BossBarSettings(
        val defaultColor: String = "PURPLE",
        val defaultOverlay: String = "PROGRESS",
        val defaultDurationSeconds: Int = 10
    )
}
