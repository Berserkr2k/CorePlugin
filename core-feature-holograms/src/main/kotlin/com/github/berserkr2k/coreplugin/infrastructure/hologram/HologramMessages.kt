package com.github.berserkr2k.coreplugin.infrastructure.hologram

import com.github.berserkr2k.coreplugin.api.core.message.MessageKey

enum class HologramMessages(override val path: String) : MessageKey {
    ONLY_PLAYERS("only-players"),
    CREATED("created"),
    DELETED("deleted"),
    NOT_FOUND("not-found"),
    LIST_EMPTY("list-empty"),
    LIST_HEADER("list-header"),
    LIST_ITEM("list-item"),
    EDIT_SUCCESS("edit-success"),
    MOVE_SUCCESS("move-success"),
    CENTER_SUCCESS("center-success"),
    CENTER_ERROR("center-error"),
    RELOAD_START("reload-start"),
    RELOAD_SUCCESS("reload-success"),
    RELOAD_ERROR("reload-error");

    override val feature: String = "holograms"

    companion object {
        val defaults = mapOf(
            "only-players" to "<red>Solo jugadores pueden ejecutar este comando.</red>",
            "created" to "<green>¡Holograma '<id>' creado con éxito en tu posición!</green>",
            "deleted" to "<green>¡Holograma '<id>' eliminado con éxito!</green>",
            "not-found" to "<red>No se encontró ningún holograma activo con el ID '<id>'.</red>",
            "list-empty" to "<yellow>No hay hologramas activos en el servidor.</yellow>",
            "list-header" to "<gold><bold>Hologramas Activos (<size>):</bold></gold>",
            "list-item" to " <gray>-</gray> <yellow><id></yellow> <gray>(Mundo: <world>, X: <x>, Y: <y>, Z: <z>)</gray>",
            "edit-success" to "<green>¡Holograma '<id>' editado con éxito!</green>",
            "move-success" to "<green>¡Holograma '<id>' trasladado a tu posición actual!</green>",
            "center-success" to "<green>¡Holograma '<id>' centrado en el bloque (X: <x>, Z: <z>)!</green>",
            "center-error" to "<red>Error al intentar trasladar el holograma '<id>'.</red>",
            "reload-start" to "<yellow>Recargando hologramas desde la configuración...</yellow>",
            "reload-success" to "<green>¡Todos los hologramas han sido recargados exitosamente!</green>",
            "reload-error" to "<red>Error crítico al recargar hologramas: <error></red>"
        )
    }
}
