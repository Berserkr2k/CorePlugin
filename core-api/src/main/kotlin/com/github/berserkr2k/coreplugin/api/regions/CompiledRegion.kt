package com.github.berserkr2k.coreplugin.api.regions

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.util.UUID

@ConfigSerializable
data class CompiledRegion(
    val id: String = "",
    val worldId: UUID = UUID.randomUUID(),
    val priority: Int = 0,
    val minX: Int = 0, val minY: Int = 0, val minZ: Int = 0,
    val maxX: Int = 0, val maxY: Int = 0, val maxZ: Int = 0,
    val definedFlags: Int = 0,
    val allowedFlags: Int = 0
) {
    fun contains(x: Int, y: Int, z: Int): Boolean {
        return x in minX..maxX && y in minY..maxY && z in minZ..maxZ
    }

    fun hasFlag(flag: Int): Boolean {
        return (definedFlags and flag) != 0
    }

    fun isAllowed(flag: Int): Boolean {
        return (allowedFlags and flag) != 0
    }
}
