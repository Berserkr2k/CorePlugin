package com.github.berserkr2k.coreplugin.infrastructure.spawn

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class PersistedLocation(
    val world: String = "world",
    val x: Double = 0.0,
    val y: Double = 100.0,
    val z: Double = 0.0,
    val yaw: Float = 0f,
    val pitch: Float = 0f
)

@ConfigSerializable
data class WorldSpawnSettings(
    @Setting("void-teleport-enabled") val voidTeleportEnabled: Boolean = true,
    @Setting("spawn-location") val spawnLocation: PersistedLocation = PersistedLocation(),
    @Setting("void-threshold-y") val voidThresholdY: Int = -64,
    @Setting("safe-fallback-location") val safeFallbackLocation: PersistedLocation = PersistedLocation()
)

@ConfigSerializable
data class SpawnConfig(
    @Setting("void-teleport-tick-cooldown") val voidTeleportTickCooldown: Int = 10,
    @Setting("warmup-seconds") val warmupSeconds: Int = 3,
    @Setting("limbo-recovery-enabled") val limboRecoveryEnabled: Boolean = true,
    @Setting("force-spawn-on-first-join") val forceSpawnOnFirstJoin: Boolean = true,
    @Setting("worlds") val worlds: Map<String, WorldSpawnSettings> = mapOf("world" to WorldSpawnSettings())
)
