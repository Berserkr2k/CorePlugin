package com.github.berserkr2k.coreplugin.infrastructure.hologram

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class HologramConfig(
    val holograms: Map<String, PersistedHologram> = emptyMap()
) {
    @ConfigSerializable
    data class PersistedHologram(
        val world: String = "world",
        val x: Double = 0.0,
        val y: Double = 0.0,
        val z: Double = 0.0,
        val lines: List<String> = emptyList(),
        val clickCommand: String? = null
    )
}
