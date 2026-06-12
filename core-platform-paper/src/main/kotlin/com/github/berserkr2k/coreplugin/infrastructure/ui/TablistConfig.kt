package com.github.berserkr2k.coreplugin.infrastructure.ui

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class TablistConfig(
    @Setting("tablist-header") val tablistHeader: String = "<gradient:gold:yellow><bold>MI SERVIDOR MASIVO</bold></gradient>\n<gray>Lobbies activos: <server_name>",
    @Setting("tablist-footer") val tablistFooter: String = "<gray>Jugadores en línea: <yellow><server_online></yellow>",
    @Setting("tablist-priorities") val tablistPriorities: List<TablistPriorityGroup> = listOf(
        TablistPriorityGroup("admin", "core.tablist.priority.admin", 1, "<red>"),
        TablistPriorityGroup("mod", "core.tablist.priority.mod", 10, "<green>"),
        TablistPriorityGroup("vip", "core.tablist.priority.vip", 50, "<gold>"),
        TablistPriorityGroup("default", "core.tablist.priority.default", 100, "<gray>")
    ),
    @Setting("update-interval-ticks") val updateIntervalTicks: Long = 20L
) {
    @ConfigSerializable
    data class TablistPriorityGroup(
        val name: String = "",
        val permission: String = "",
        val priority: Int = 100,
        val color: String = "<white>"
    )
}
