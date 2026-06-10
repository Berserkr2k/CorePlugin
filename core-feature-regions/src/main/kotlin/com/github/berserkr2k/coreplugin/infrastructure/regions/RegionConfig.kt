package com.github.berserkr2k.coreplugin.infrastructure.regions

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class RegionConfig(
    @Setting("selection-tool") val selectionTool: String = "WOODEN_AXE",
    @Setting("void-threshold-y") val voidThresholdY: Int = 0,
    @Setting("void-teleport-tick-cooldown") val voidTeleportTickCooldown: Int = 10
)
