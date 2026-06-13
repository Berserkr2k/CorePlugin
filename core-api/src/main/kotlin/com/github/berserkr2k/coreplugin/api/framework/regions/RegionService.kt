package com.github.berserkr2k.coreplugin.api.framework.regions

import org.bukkit.Location

interface RegionService {
    fun hasFlag(location: Location, flag: RegionFlag): Boolean
    fun getRegion(location: Location): Region?
    fun getRegions(location: Location): List<Region>
    fun getHighestPriorityRegion(location: Location): Region?

    fun createRegion(
        id: String,
        worldName: String,
        priority: Int,
        minX: Int, minY: Int, minZ: Int,
        maxX: Int, maxY: Int, maxZ: Int,
        allowFlags: List<String>,
        denyFlags: List<String>,
        tags: Map<String, String>,
        type: RegionType
    ): java.util.concurrent.CompletableFuture<Void>

    fun removeRegion(id: String): java.util.concurrent.CompletableFuture<Boolean>
}
