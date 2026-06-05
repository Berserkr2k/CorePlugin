package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class EditorConfig(
    val permissionUseEditor: String = "core.leaderboard.editor",
    val editorItemMaterial: String = "GOLDEN_CARROT"
)
