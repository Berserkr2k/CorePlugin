package com.github.berserkr2k.coreplugin.infrastructure.ui

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class UiConfig(
    @Setting("tablist-header") val tablistHeader: String = "<gradient:gold:yellow><bold>MI SERVIDOR MASIVO</bold></gradient>\n<gray>Lobbies activos: <server_name>",
    @Setting("tablist-footer") val tablistFooter: String = "<gray>Jugadores en línea: <yellow><server_online></yellow>",
    @Setting("tablist-priorities") val tablistPriorities: List<TablistPriorityGroup> = listOf(
        TablistPriorityGroup("admin", "core.tablist.priority.admin", 1),
        TablistPriorityGroup("mod", "core.tablist.priority.mod", 10),
        TablistPriorityGroup("vip", "core.tablist.priority.vip", 50),
        TablistPriorityGroup("default", "core.tablist.priority.default", 100)
    )
) {
    @ConfigSerializable
    data class TablistPriorityGroup(
        val name: String = "",
        val permission: String = "",
        val priority: Int = 100
    )
}
