package com.github.berserkr2k.coreplugin.infrastructure.regions

import com.github.berserkr2k.coreplugin.api.framework.regions.Region
import com.github.berserkr2k.coreplugin.api.framework.regions.RegionFlag
import com.github.berserkr2k.coreplugin.api.framework.regions.RegionType
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
    override val id: String,
    val worldIndex: Int,
    override val priority: Int,
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int,
    val allowMask: Long,
    val denyMask: Long,
    override val tags: Map<String, String>,
    override val type: RegionType
) : Region {
    override val worldUniqueId: UUID?
        get() = WorldIndexRegistry.getUuid(worldIndex)

    override fun contains(x: Int, y: Int, z: Int): Boolean {
        return x >= minX &&
               x <= maxX &&
               y >= minY &&
               y <= maxY &&
               z >= minZ &&
               z <= maxZ
    }

    override fun hasFlag(flag: RegionFlag): Boolean {
        return ((allowMask or denyMask) and flag.mask) != 0L
    }

    override fun isAllowed(flag: RegionFlag): Boolean {
        if ((denyMask and flag.mask) != 0L) return false
        return (allowMask and flag.mask) != 0L
    }
}
