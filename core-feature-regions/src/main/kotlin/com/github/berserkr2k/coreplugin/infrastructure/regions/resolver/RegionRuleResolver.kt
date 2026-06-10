package com.github.berserkr2k.coreplugin.infrastructure.regions.resolver

import com.github.berserkr2k.coreplugin.api.regions.CompiledRegion
import com.github.berserkr2k.coreplugin.api.regions.RegionQueryContext
import com.github.berserkr2k.coreplugin.infrastructure.regions.service.RegionManager
import org.bukkit.Location
import org.bukkit.GameMode
import java.util.ArrayList

class RegionRuleResolver(private val regionManager: RegionManager) {

    fun resolveActiveRegions(loc: Location): List<CompiledRegion> {
        val blockX = loc.blockX
        val blockY = loc.blockY
        val blockZ = loc.blockZ
        
        val candidates = regionManager.getCurrentIndex().getRegionsInChunk(blockX shr 4, blockZ shr 4) ?: return emptyList()
        val activeRegions = ArrayList<CompiledRegion>()
        val worldId = loc.world.uid 

        for (i in 0 until candidates.size) {
            val region = candidates[i]
            if (region.worldId == worldId && region.contains(blockX, blockY, blockZ)) {
                activeRegions.add(region)
            }
        }
        
        activeRegions.sortBy { it.priority }
        return activeRegions
    }

    fun isActionAllowed(loc: Location, flag: Int, context: RegionQueryContext): Boolean {
        if (context.bypassPermission || context.gameMode == GameMode.CREATIVE) return true

        val regions = resolveActiveRegions(loc)
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
