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
    ONLY_PLAYERS("only-players");

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
            "only-players" to "<red>Solo jugadores pueden abrir el menú de selección de estelas.</red>"
        )
    }
}
