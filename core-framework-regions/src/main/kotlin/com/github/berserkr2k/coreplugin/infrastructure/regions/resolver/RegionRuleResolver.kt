package com.github.berserkr2k.coreplugin.infrastructure.regions.resolver

import com.github.berserkr2k.coreplugin.infrastructure.regions.CompiledRegion
import com.github.berserkr2k.coreplugin.api.framework.regions.RegionQueryContext
import com.github.berserkr2k.coreplugin.api.framework.regions.RegionFlags
import com.github.berserkr2k.coreplugin.infrastructure.regions.service.RegionManager
import org.bukkit.GameMode
import java.util.ArrayList

class RegionRuleResolver(private val regionManager: RegionManager) {

    fun resolveActiveRegions(worldIndex: Int, x: Int, y: Int, z: Int): Array<CompiledRegion> {
        val candidates = regionManager.getCurrentIndex().getRegionsInChunk(worldIndex, x shr 4, z shr 4) ?: return emptyArray()
        val activeRegions = ArrayList<CompiledRegion>()

        for (i in 0 until candidates.size) {
            val region = candidates[i]
            if (region.worldIndex == worldIndex && region.contains(x, y, z)) {
                activeRegions.add(region)
            }
        }
        
        return activeRegions.toTypedArray()
    }

    fun isActionAllowed(worldIndex: Int, x: Int, y: Int, z: Int, flag: Long, context: RegionQueryContext): Boolean {
        if (RegionFlags.bypassesFlag(flag) && (context.bypassPermission || context.gameMode == GameMode.CREATIVE)) return true

        val regions = resolveActiveRegions(worldIndex, x, y, z)
        if (regions.isEmpty()) {
            return RegionFlags.isAllowedByDefault(flag)
        }

        val regionFlag = RegionFlags.getFlagByMask(flag)
        if (regionFlag != null) {
            // Buscar la bandera específica de mayor a menor prioridad
            for (i in regions.indices.reversed()) {
                val region = regions[i]
                if (region.hasFlag(regionFlag)) {
                    return region.isAllowed(regionFlag)
                }
            }
        }

        // Si no está definida, comprobar fallbacks (por ejemplo, 'build' para 'block-break' o 'block-place')
        val fallback = getFallbackFlag(flag)
        if (fallback != null) {
            val fallbackFlag = RegionFlags.getFlagByMask(fallback)
            if (fallbackFlag != null) {
                for (i in regions.indices.reversed()) {
                    val region = regions[i]
                    if (region.hasFlag(fallbackFlag)) {
                        return region.isAllowed(fallbackFlag)
                    }
                }
            }
        }

        return RegionFlags.isAllowedByDefault(flag)
    }

    private fun getFallbackFlag(flag: Long): Long? {
        return when (flag) {
            RegionFlags.BLOCK_BREAK, RegionFlags.BLOCK_PLACE -> RegionFlags.BUILD
            else -> null
        }
    }
}
