package com.github.berserkr2k.coreplugin.infrastructure.regions.spatial

import com.github.berserkr2k.coreplugin.api.regions.CompiledRegion
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import java.util.Collections

class SpatialRegionIndex {

    private val chunkMap = Long2ObjectOpenHashMap<List<CompiledRegion>>()

    fun buildFrom(rawRegions: Collection<CompiledRegion>) {
        val temporaryMap = Long2ObjectOpenHashMap<MutableList<CompiledRegion>>()

        for (region in rawRegions) {
            val minChunkX = region.minX shr 4
            val maxChunkX = region.maxX shr 4
            val minChunkZ = region.minZ shr 4
            val maxChunkZ = region.maxZ shr 4

            for (cx in minChunkX..maxChunkX) {
                for (cz in minChunkZ..maxChunkZ) {
                    val chunkKey = (cx.toLong() shl 32) or (cz.toLong() and 0xffffffffL)
                    temporaryMap.computeIfAbsent(chunkKey) { ArrayList() }.add(region)
                }
            }
        }

        for (entry in temporaryMap.long2ObjectEntrySet()) {
            chunkMap.put(entry.longKey, Collections.unmodifiableList(entry.value))
        }
    }

    fun getRegionsInChunk(chunkX: Int, chunkZ: Int): List<CompiledRegion>? {
        val chunkKey = (chunkX.toLong() shl 32) or (chunkZ.toLong() and 0xffffffffL)
        return chunkMap.get(chunkKey)
    }
}
