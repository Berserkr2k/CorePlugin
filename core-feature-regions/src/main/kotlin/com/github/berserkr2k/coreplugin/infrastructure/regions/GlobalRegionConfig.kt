package com.github.berserkr2k.coreplugin.infrastructure.regions

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class GlobalRegionConfig(
    @Setting("selection-tool") val selectionTool: String = "WOODEN_AXE"
)
