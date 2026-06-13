package com.github.berserkr2k.coreplugin.infrastructure.warps

import com.github.berserkr2k.coreplugin.api.core.message.MessageKey

enum class WarpMessages(override val path: String) : MessageKey {
    SET("set"),
    DELETED("deleted"),
    NOT_FOUND("not-found"),
    SUCCESS("success"),
    NO_PERMISSION("no-permission"),
    WARMUP("warmup"),
    COOLDOWN_ACTIVE("cooldown-active"),
    CANCELLED_MOVEMENT("cancelled-movement"),
    CANCELLED_DAMAGE("cancelled-damage"),
    MENU_TITLE("menu-title"),
    WORLD_NOT_LOADED("world-not-loaded"),
    TELEPORT_FAILED("teleport-failed"),
    RELOADED("reloaded"),
    GUI_WARP_LOCKED("gui.warp-locked"),
    GUI_WARP_REQUIRES_PERMISSION("gui.warp-requires-permission"),
    GUI_WARP_DEFAULT_DISPLAYNAME("gui.warp-default-displayname");

    override val feature: String = "warps"

    companion object {
        val defaults = mapOf(
            "set" to "<green>¡Warp '<name>' establecido correctamente en tu posición!</green>",
            "deleted" to "<red>¡Warp '<name>' eliminado correctamente!</red>",
            "not-found" to "<red>El warp '<name>' no existe.</red>",
            "success" to "<green>¡Teletransportado al warp '<name>' con éxito!</green>",
            "no-permission" to "<red>No tienes permiso para usar este warp.</red>",
            "warmup" to "<yellow>Teletransportándote en <time> segundos... ¡No te muevas ni recibas daño!</yellow>",
            "cooldown-active" to "<red>Debes esperar <time> segundos antes de volver a usar este warp.</red>",
            "cancelled-movement" to "<red>Teletransportación cancelada por movimiento.</red>",
            "cancelled-damage" to "<red>Teletransportación cancelada por daño recibido.</red>",
            "menu-title" to "<dark_gray>Puntos de Teletransporte</dark_gray>",
            "world-not-loaded" to "<red>El mundo de destino '<world>' no está cargado.</red>",
            "teleport-failed" to "<red>No se pudo realizar la teletransportación.</red>",
            "reloaded" to "<green>¡Configuraciones de warps recargadas con éxito!</green>",
            "gui.warp-locked" to "<red>❌ Bloqueado</red>",
            "gui.warp-requires-permission" to "<gray>Requiere permiso: <red><permission></red></gray>",
            "gui.warp-default-displayname" to "<green><bold>Warp <name></bold></green>"
        )
    }
}
