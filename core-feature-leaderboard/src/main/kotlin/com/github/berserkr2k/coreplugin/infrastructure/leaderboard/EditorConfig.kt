package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class EditorConfig(
    val permissionUseEditor: String = "core.leaderboard.editor",
    val editorItemMaterial: String = "GOLDEN_CARROT",
    val editorItemName: String = "<gold><bold>Vara de Edición de ArmorStands</bold></gold>",
    val editorItemLore: List<String> = listOf(
        "<gray>Haz click derecho sobre cualquier ArmorStand</gray>",
        "<gray>para abrir el editor de pose y equipamiento.</gray>"
    ),
    val poseToolMaterial: String = "LEVER",
    val poseToolName: String = "<gold><bold>Herramienta de Pose: %part% (%axis%)</bold></gold>",
    val poseToolLore: List<String> = listOf(
        "<yellow>Parte: <white>%part%</white></yellow>",
        "<yellow>Eje: <white>%axis%</white></yellow>",
        "",
        "<green>◀ Click Izquierdo: Aumentar ángulo</green>",
        "<red>▶ Click Derecho: Disminuir ángulo</red>",
        "<gray>Sneak + Click Derecho: Volver al menú</gray>"
    )
)
