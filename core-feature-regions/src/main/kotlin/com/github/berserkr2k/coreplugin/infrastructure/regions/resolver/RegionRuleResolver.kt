package com.github.berserkr2k.coreplugin.infrastructure.regions.resolver

import com.github.berserkr2k.coreplugin.api.regions.CompiledRegion
import com.github.berserkr2k.coreplugin.api.regions.RegionQueryContext
import com.github.berserkr2k.coreplugin.infrastructure.regions.service.RegionManager
import org.bukkit.GameMode
import java.util.ArrayList

class RegionRuleResolver(private val regionManager: RegionManager) {

    fun resolveActiveRegions(worldIndex: Int, x: Int, y: Int, z: Int): Array<CompiledRegion> {
        val candidates = regionManager.getCurrentIndex().getRegionsInChunk(x shr 4, z shr 4) ?: return emptyArray()
        val activeRegions = ArrayList<CompiledRegion>()

        for (i in 0 until candidates.size) {
            val region = candidates[i]
            if (region.worldIndex == worldIndex && region.contains(x, y, z)) {
                activeRegions.add(region)
            }
        }
        
        return activeRegions.toTypedArray()
    }

    fun isActionAllowed(worldIndex: Int, x: Int, y: Int, z: Int, flag: Int, context: RegionQueryContext): Boolean {
        if (context.bypassPermission || context.gameMode == GameMode.CREATIVE) return true

        val regions = resolveActiveRegions(worldIndex, x, y, z)
        if (regions.isEmpty()) return true

        var allowed = true
        for (i in 0 until regions.size) {
            val region = regions[i]
            if (region.hasFlag(flag)) {
                allowed = region.isAllowed(flag)
            }
        }
        return allowed
    }
}
