package com.github.berserkr2k.coreplugin.infrastructure.scoreboard

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ScoreboardLayoutConfig(
    val title: List<String> = listOf("<gold><bold>★ SERVIDOR CORE ★</bold></gold>"),
    val lines: List<String> = listOf(
        "<gray>----------------------</gray>",
        "  <yellow>Jugador:</yellow> <white>%player_name%</white>",
        "  <yellow>Rango:</yellow> <white>%vault_prefix%</white>",
        " ",
        "  <green>Créditos:</green> <white>%coreplugin_balance_formatted_credits%</white>",
        "  <green>Puntos:</green> <white>%coreplugin_balance_formatted_points%</white>",
        " ",
        "  <aqua>Mundo:</aqua> <white>%player_world%</white>",
        "  <aqua>Online:</aqua> <white>%server_online%</white>",
        "<gray>----------------------</gray>"
    ),
    val footer: List<String> = listOf(
        "<gradient:gray:gold><bold>play.mistycube.net</bold></gradient>"
    ),
    val centerFooter: Boolean = true
)

@ConfigSerializable
data class ScoreboardModuleConfig(
    val enabled: Boolean = true,
    val updateIntervalTicks: Long = 20L,
    val titleUpdateIntervalTicks: Long = 20L,
    val defaultLayout: ScoreboardLayoutConfig = ScoreboardLayoutConfig(),
    val worlds: Map<String, ScoreboardLayoutConfig> = mapOf(
        "spawn" to ScoreboardLayoutConfig(
            title = listOf(
                "<light_purple><bold>★ SPAWN CORE ★</bold></light_purple>",
                "<white><bold>★ SPAWN CORE ★</bold></white>"
            ),
            lines = listOf(
                "<gray>----------------------</gray>",
                " <yellow>Bienvenido,</yellow> <white>%player_name%</white>",
                " ",
                " <pink>Disfruta tu estadía</pink>",
                " <pink>en la zona segura.</pink>",
                "<gray>----------------------</gray>"
            )
        )
    )
)
