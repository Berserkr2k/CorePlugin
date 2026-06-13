package com.github.berserkr2k.coreplugin.infrastructure.regions.compiler

import com.github.berserkr2k.coreplugin.infrastructure.regions.CompiledRegion
import com.github.berserkr2k.coreplugin.api.framework.regions.RegionFlags
import com.github.berserkr2k.coreplugin.infrastructure.regions.WorldIndexRegistry
import com.github.berserkr2k.coreplugin.infrastructure.regions.RegionConfig
import org.bukkit.Bukkit

object RegionCompiler {

    fun compile(dto: RegionConfig): CompiledRegion {
        val world = Bukkit.getWorld(dto.world) 
            ?: throw IllegalArgumentException("El mundo '${dto.world}' para la región '${dto.id}' no está cargado o no existe.")

        val worldIndex = WorldIndexRegistry.getIndex(world.uid)

        val minX = minOf(dto.minX, dto.maxX)
        val minY = minOf(dto.minY, dto.maxY)
        val minZ = minOf(dto.minZ, dto.maxZ)
        val maxX = maxOf(dto.minX, dto.maxX)
        val maxY = maxOf(dto.minY, dto.maxY)
        val maxZ = maxOf(dto.minZ, dto.maxZ)

        var allowMask = 0L
        for (flagStr in dto.allowFlags) {
            val flag = RegionFlags.parse(flagStr)
            if (flag != RegionFlags.NONE) {
                allowMask = allowMask or flag
            }
        }

        var denyMask = 0L
        for (flagStr in dto.denyFlags) {
            val flag = RegionFlags.parse(flagStr)
            if (flag != RegionFlags.NONE) {
                denyMask = denyMask or flag
            }
        }

        return CompiledRegion(
            id = dto.id,
            worldIndex = worldIndex,
            priority = dto.priority,
            minX = minX,
            minY = minY,
            minZ = minZ,
            maxX = maxX,
            maxY = maxY,
            maxZ = maxZ,
            allowMask = allowMask,
            denyMask = denyMask,
            tags = dto.tags,
            type = dto.type
        )
    }
}
