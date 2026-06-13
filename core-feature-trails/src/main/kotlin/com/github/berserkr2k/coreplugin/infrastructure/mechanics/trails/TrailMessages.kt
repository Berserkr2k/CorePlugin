package com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails

import com.github.berserkr2k.coreplugin.api.core.message.MessageKey

enum class TrailMessages(override val path: String) : MessageKey {
    EQUIPPED("equipped"),
    NO_PERMISSION("no-permission"),
    REMOVED("removed"),
    RELOADED("reloaded"),
    NOT_FOUND("not-found"),
    ADMIN_EQUIPPED_SENDER("admin-equipped-sender"),
    ADMIN_EQUIPPED_TARGET("admin-equipped-target"),
    ADMIN_CLEARED_SENDER("admin-cleared-sender"),
    ADMIN_CLEARED_TARGET("admin-cleared-target"),
    ONLY_PLAYERS("only-players"),
    GUI_TRAIL_EQUIPPED("gui.trail-equipped"),
    GUI_TRAIL_EQUIPPED_LORE("gui.trail-equipped-lore"),
    GUI_TRAIL_SELECT("gui.trail-select"),
    GUI_TRAIL_LOCKED("gui.trail-locked"),
    GUI_TRAIL_LOCKED_LORE("gui.trail-locked-lore"),
    GUI_CLEAR_NAME("gui.clear-name"),
    GUI_CLEAR_LORE("gui.clear-lore");

    override val feature: String = "trails"

    companion object {
        val defaults = mapOf(
            "equipped" to "<green>¡Has equipado la estela <name>!</green>",
            "no-permission" to "<red>No tienes permisos para usar esta estela.</red>",
            "removed" to "<yellow>Has removido tu estela de partículas.</yellow>",
            "reloaded" to "<green>¡Configuraciones de Estelas de Proyectiles recargadas con éxito en tiempo real!</green>",
            "not-found" to "<red>La estela especificada '<id>' no existe.</red>",
            "admin-equipped-sender" to "<green>¡Estela '<name>' equipada con éxito a <target>!</green>",
            "admin-equipped-target" to "<green>¡Se te ha equipado la estela '<name>' por un administrador!</green>",
            "admin-cleared-sender" to "<green>¡Se ha removido la estela de <target> con éxito!</green>",
            "admin-cleared-target" to "<yellow>Tu estela de partículas ha sido removida por un administrador.</yellow>",
            "only-players" to "<red>Solo jugadores pueden abrir el menú de selección de estelas.</red>",
            "gui.trail-equipped" to "<green>⭐ ¡Estela Equipada!</green>",
            "gui.trail-equipped-lore" to "<gray>Tu proyectil ya tiene este efecto.</gray>",
            "gui.trail-select" to "<yellow>⚡ Click para Equipar</yellow>",
            "gui.trail-locked" to "<red>❌ Bloqueado</red>",
            "gui.trail-locked-lore" to "<gray>Requiere permiso: <red><permission></red></gray>",
            "gui.clear-name" to "<red><bold>❌ Quitar Estela</bold></red>",
            "gui.clear-lore" to "<gray>Haz click aquí para remover tu</gray>\n<gray>estela de partículas activa.</gray>\n \n<yellow>⚡ Click para remover</yellow>"
        )
    }
}
