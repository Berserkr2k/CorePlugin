package com.github.berserkr2k.coreplugin.infrastructure.regions.spatial

import com.github.berserkr2k.coreplugin.infrastructure.regions.CompiledRegion
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap

class SpatialRegionIndex {

    private val chunkMap = Long2ObjectOpenHashMap<Array<CompiledRegion>>()

    fun buildFrom(rawRegions: Collection<CompiledRegion>) {
        val temporaryMap = Long2ObjectOpenHashMap<MutableList<CompiledRegion>>()

        for (region in rawRegions) {
            val minChunkX = region.minX shr 4
            val maxChunkX = region.maxX shr 4
            val minChunkZ = region.minZ shr 4
            val maxChunkZ = region.maxZ shr 4

            val worldIndex = region.worldIndex

            for (cx in minChunkX..maxChunkX) {
                for (cz in minChunkZ..maxChunkZ) {
                    val chunkKey = (worldIndex.toLong() shl 48) or ((cx.toLong() and 0xffffffL) shl 24) or (cz.toLong() and 0xffffffL)
                    temporaryMap.computeIfAbsent(chunkKey) { ArrayList() }.add(region)
                }
            }
        }

        for (entry in temporaryMap.long2ObjectEntrySet()) {
            val sortedList = entry.value.sortedBy { it.priority }
            chunkMap.put(entry.longKey, sortedList.toTypedArray())
        }
    }

    fun getRegionsInChunk(worldIndex: Int, chunkX: Int, chunkZ: Int): Array<CompiledRegion>? {
        val chunkKey = (worldIndex.toLong() shl 48) or ((chunkX.toLong() and 0xffffffL) shl 24) or (chunkZ.toLong() and 0xffffffL)
        return chunkMap.get(chunkKey)
    }
}
