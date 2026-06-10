package com.github.berserkr2k.coreplugin.api.regions

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object WorldIndexRegistry {
    private val uuidToIndex = ConcurrentHashMap<UUID, Int>()
    private val indexToUuid = ConcurrentHashMap<Int, UUID>()
    private val counter = AtomicInteger(0)

    fun getIndex(uuid: UUID): Int {
        return uuidToIndex.computeIfAbsent(uuid) {
            val idx = counter.getAndIncrement()
            indexToUuid[idx] = uuid
            idx
        }
    }

    fun getUuid(index: Int): UUID? {
        return indexToUuid[index]
    }
}

data class CompiledRegion(
    val id: String,
    val worldIndex: Int,
    val priority: Int,
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int,
    val allowMask: Int,
    val denyMask: Int
) {
    fun contains(x: Int, y: Int, z: Int): Boolean {
        return x >= minX &&
               x <= maxX &&
               y >= minY &&
               y <= maxY &&
               z >= minZ &&
               z <= maxZ
    }

    fun hasFlag(flag: Int): Boolean {
        return ((allowMask or denyMask) and flag) != 0
    }

    fun isAllowed(flag: Int): Boolean {
        if ((denyMask and flag) != 0) return false
        return (allowMask and flag) != 0
    }
}
