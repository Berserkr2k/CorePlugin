package com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class TrailLayerConfig(
    val particle: String = "FLAME",
    val count: Int = 1,
    val speed: Double = 0.015,
    val radius: Double = 0.28,
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0,
    val offsetZ: Double = 0.0
)

@ConfigSerializable
data class TrailConfig(
    val id: String = "fire",
    val displayName: String = "<red><bold>Estela de Fuego</bold></red>",
    val permission: String = "core.trail.use.fire",
    val particleType: String = "FLAME",
    val particleCount: Int = 1,
    val baseInterval: Int = 2,
    val density: Double = 3.0,
    val speed: Double = 0.02,
    val offsetX: Double = 0.05,
    val offsetY: Double = 0.05,
    val offsetZ: Double = 0.05,
    val colorR: Int = 255,
    val colorG: Int = 255,
    val colorB: Int = 255,
    val dustSize: Float = 1.0f,
    val style: String = "LINEAR", // LINEAR, SPIRAL, DOUBLE_HELIX, ORBIT, RIBBON, CHAOS, HELIX, VORTEX
    val spiralRadius: Double = 0.35,
    val spiralSpeed: Double = 2.5,
    val waveAmplitude: Double = 0.35,
    val waveSpeed: Double = 3.0,
    val randomness: Double = 0.0,
    val pulse: Boolean = false,
    val speedScaling: Boolean = false,
    val lifetimeFade: Boolean = false,
    val emissionMode: String = "CONTINUOUS", // CONTINUOUS, BURST
    val burstEvery: Int = 3,
    val burstCount: Int = 10,
    val layers: List<TrailLayerConfig> = emptyList(),
    val modifiers: List<String> = emptyList(),
    val conditions: List<String> = emptyList(),
    val animations: List<String> = emptyList(),
    val gradient: List<String> = emptyList(),
    val gradients: List<String> = emptyList(),
    val guiSlot: Int = -1,
    val guiIcon: String = "FLINT_AND_STEEL",
    val guiLore: List<String> = listOf("<gray>Una ardiente estela de fuego</gray>", "<gray>que rodea tus proyectiles.</gray>")
)
