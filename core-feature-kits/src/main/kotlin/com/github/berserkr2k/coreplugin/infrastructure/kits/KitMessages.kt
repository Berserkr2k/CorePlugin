package com.github.berserkr2k.coreplugin.infrastructure.kits

import com.github.berserkr2k.coreplugin.api.core.message.MessageKey

enum class KitMessages(override val path: String) : MessageKey {
    NOT_FOUND("not-found"),
    NO_PERMISSION("no-permission"),
    COOLDOWN("cooldown"),
    INSUFFICIENT_FUNDS("insufficient-funds"),
    NO_SPACE("no-space"),
    PURCHASE_FAILED("purchase-failed"),
    CLAIMED("claimed"),
    RELOADED("reloaded"),
    GIVE_SUCCESS_SENDER("give-success-sender"),
    GIVE_SUCCESS_RECEIVER("give-success-receiver"),
    PROFILE_ERROR("profile-error"),
    GIVE_FAILED("give-failed"),
    GUI_STATUS_LOCKED("gui.status-locked"),
    GUI_STATUS_COOLDOWN("gui.status-cooldown"),
    GUI_STATUS_COST("gui.status-cost"),
    GUI_STATUS_BYPASS("gui.status-bypass"),
    GUI_STATUS_AVAILABLE("gui.status-available"),
    GUI_ACTION_CLAIM("gui.action-claim"),
    GUI_ACTION_PREVIEW("gui.action-preview"),
    GUI_SHOWCASE_LOCKED_NAME("gui.showcase.locked-name"),
    GUI_SHOWCASE_LOCKED_LORE("gui.showcase.locked-lore"),
    GUI_SHOWCASE_COOLDOWN_NAME("gui.showcase.cooldown-name"),
    GUI_SHOWCASE_COOLDOWN_LORE("gui.showcase.cooldown-lore"),
    GUI_SHOWCASE_BACK_NAME("gui.showcase.back-name"),
    GUI_SHOWCASE_BACK_LORE("gui.showcase.back-lore"),
    GUI_SHOWCASE_CLAIM_NAME("gui.showcase.claim-name"),
    GUI_SHOWCASE_CLAIM_LORE("gui.showcase.claim-lore");

    override val feature: String = "kits"

    companion object {
        val defaults = mapOf(
            "not-found" to "<red>El kit especificado no existe.</red>",
            "no-permission" to "<red>No tienes permiso para esto.</red>",
            "cooldown" to "<red>Debes esperar <time> para volver a reclamar este kit.</red>",
            "insufficient-funds" to "<red>No tienes dinero suficiente para comprar este kit.</red>",
            "no-space" to "<red>No tienes espacio suficiente en el inventario (<required> ranuras libres requeridas).</red>",
            "purchase-failed" to "<red>Hubo un fallo al procesar la compra del kit.</red>",
            "claimed" to "<green>¡Has reclamado tu kit con éxito!</green>",
            "reloaded" to "<green>¡Configuraciones de Kits recargadas con éxito en tiempo real!</green>",
            "give-success-sender" to "<green>¡Has entregado con éxito el kit '<kit>' a <target>!</green>",
            "give-success-receiver" to "<green>¡Has recibido el kit '<kit>' de parte de un administrador!</green>",
            "profile-error" to "<red>Tu perfil no está cargado en el sistema.</red>",
            "give-failed" to "<red>Error al entregar kit: <reason></red>",
            "gui.status-locked" to "<red>❌ Bloqueado (Requiere Rango)</red>",
            "gui.status-cooldown" to "<red>⏳ En Cooldown (Espera: <time>)</red>",
            "gui.status-cost" to "<gold>⚖ Costo: <cost> <currency></gold>",
            "gui.status-bypass" to "<green>✔ ¡Disponible! (<yellow>Bypass de Cooldown Activo</yellow>)</green>",
            "gui.status-available" to "<green>✔ ¡Disponible para Reclamar!</green>",
            "gui.action-claim" to "<yellow>⚡ Click Izquierdo para Reclamar</yellow>",
            "gui.action-preview" to "<aqua>⚡ Click Derecho para Previsualizar</aqua>",
            "gui.showcase.locked-name" to "<red><bold>❌ Kit Bloqueado</bold></red>",
            "gui.showcase.locked-lore" to "<gray>Requiere el rango de permiso:</gray>\n<red><permission></red>",
            "gui.showcase.cooldown-name" to "<yellow><bold>⏳ En Cooldown</bold></yellow>",
            "gui.showcase.cooldown-lore" to "<gray>Debes esperar:</gray>\n<yellow><time></yellow>\n<gray>para reclamar nuevamente.</gray>",
            "gui.showcase.back-name" to "<red>Volver al Selector</red>",
            "gui.showcase.back-lore" to "<gray>Regresa al selector de kits principal.</gray>",
            "gui.showcase.claim-name" to "<green><bold>✔ ¡Reclamar Kit!</bold></green>",
            "gui.showcase.claim-lore" to "%price_lore%%bypass_lore%<gray>Haz click aquí para recibir los items.</gray>"
        )
    }
}
