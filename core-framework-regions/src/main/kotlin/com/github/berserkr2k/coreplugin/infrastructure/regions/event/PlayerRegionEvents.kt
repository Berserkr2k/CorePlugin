package com.github.berserkr2k.coreplugin.infrastructure.regions.event

import com.github.berserkr2k.coreplugin.infrastructure.regions.CompiledRegion
import org.bukkit.entity.Player

data class PlayerRegionEnterEvent(
    val player: Player,
    val region: CompiledRegion
)

data class PlayerRegionLeaveEvent(
    val player: Player,
    val region: CompiledRegion
)
