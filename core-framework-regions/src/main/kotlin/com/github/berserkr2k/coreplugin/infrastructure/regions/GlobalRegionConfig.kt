package com.github.berserkr2k.coreplugin.infrastructure.regions

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class GlobalRegionConfig(
    @Setting("selection-tool") val selectionTool: String = "WOODEN_AXE",
    @Setting("enable-combat-messages") val enableCombatMessages: Boolean = true,
    @Setting("enable-world-messages") val enableWorldMessages: Boolean = true,
    @Setting("enable-interaction-messages") val enableInteractionMessages: Boolean = true,
    @Setting("enable-player-messages") val enablePlayerMessages: Boolean = true,
    @Setting("enable-entity-messages") val enableEntityMessages: Boolean = true
)
