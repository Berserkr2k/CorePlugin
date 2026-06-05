package com.github.berserkr2k.coreplugin.infrastructure.hologram

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class HologramConfig(
    val id: String = "default",
    val world: String = "world",
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val lines: List<String> = emptyList(),
    val clickCommand: String? = null,
    val lineSpacing: Double = 0.28,
    val backgroundColor: Int = 1073741824, // 0x40000000
    val updatable: Boolean = false,
    val updateInterval: Int = 1, // in minutes
    val renderDistance: Int = 48
)
