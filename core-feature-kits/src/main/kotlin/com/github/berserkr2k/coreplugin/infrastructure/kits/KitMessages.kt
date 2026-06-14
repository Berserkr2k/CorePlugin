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
    GIVE_FAILED("give-failed");

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
            "give-failed" to "<red>Error al entregar kit: <reason></red>"
        )
    }
}
