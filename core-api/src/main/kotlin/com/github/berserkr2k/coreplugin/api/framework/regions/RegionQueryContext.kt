package com.github.berserkr2k.coreplugin.api.framework.regions

import org.bukkit.GameMode
import org.bukkit.entity.Player

data class RegionQueryContext(
    val player: Player?,
    val gameMode: GameMode?,
    val bypassPermission: Boolean
)
