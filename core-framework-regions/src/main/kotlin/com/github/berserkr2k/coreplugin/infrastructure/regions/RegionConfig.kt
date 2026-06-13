package com.github.berserkr2k.coreplugin.infrastructure.regions

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class RegionConfig(
    val id: String = "",
    val world: String = "",
    val priority: Int = 0,
    val minX: Int = 0, val minY: Int = 0, val minZ: Int = 0,
    val maxX: Int = 0, val maxY: Int = 0, val maxZ: Int = 0,
    val allowFlags: List<String> = emptyList(),
    val denyFlags: List<String> = emptyList(),
    val tags: Map<String, String> = emptyMap(),
    val type: com.github.berserkr2k.coreplugin.api.framework.regions.RegionType = com.github.berserkr2k.coreplugin.api.framework.regions.RegionType.STATIC
)
