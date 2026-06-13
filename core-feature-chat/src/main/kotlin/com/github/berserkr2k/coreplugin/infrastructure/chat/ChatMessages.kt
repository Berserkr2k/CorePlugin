package com.github.berserkr2k.coreplugin.infrastructure.chat

import com.github.berserkr2k.coreplugin.api.core.message.MessageKey

enum class ChatMessages(override val path: String) : MessageKey {
    CHAT_SOCIALSPY_ENABLED("socialspy-enabled"),
    CHAT_SOCIALSPY_DISABLED("socialspy-disabled"),
    CHAT_PM_USAGE("pm-usage"),
    CHAT_PM_PLAYER_NOT_FOUND("pm-player-not-found"),
    CHAT_PM_CANNOT_MSG_SELF("pm-cannot-msg-self"),
    CHAT_REPLY_USAGE("reply-usage"),
    CHAT_REPLY_NO_TARGET("reply-no-target"),
    CHAT_COLOR_CHANGED("color-changed"),
    CHAT_COLOR_MENU_TITLE("color-menu-title"),
    CHAT_PM_SENT_FORMAT("pm-sent-format"),
    CHAT_PM_RECEIVED_FORMAT("pm-received-format"),
    CHAT_PM_SOCIALSPY_FORMAT("pm-socialspy-format"),
    CHAT_COOLDOWN("cooldown"),
    CHAT_LINK_BLOCKED("link-blocked"),
    CHAT_PROFILE_ERROR("profile-error"),
    CHAT_COLOR_NO_PERMISSION("color-no-permission"),
    CHAT_COLOR_RESET("color-reset");

    override val feature: String = "chat"

    companion object {
        val defaults = mapOf(
            "socialspy-enabled" to "<green>SocialSpy habilitado.</green>",
            "socialspy-disabled" to "<red>SocialSpy deshabilitado.</red>",
            "pm-usage" to "<red>Uso: /msg <jugador> <mensaje></red>",
            "pm-player-not-found" to "<red>Jugador no encontrado o desconectado.</red>",
            "pm-cannot-msg-self" to "<red>No puedes enviarte mensajes privados a ti mismo.</red>",
            "reply-usage" to "<red>Uso: /reply <mensaje></red>",
            "reply-no-target" to "<red>No tienes a nadie a quien responder.</red>",
            "color-changed" to "<green>¡Tu color de chat ha sido cambiado a <color>!</green>",
            "color-menu-title" to "<dark_gray><bold>Selecciona un Color</bold></dark_gray>",
            "pm-sent-format" to "<gray>[Yo -> <target>]: <message></gray>",
            "pm-received-format" to "<gray>[<sender> -> Yo]: <message></gray>",
            "pm-socialspy-format" to "<dark_gray>[Spy] [<sender> -> <target>]: <message></dark_gray>",
            "cooldown" to "<red>⚠️ Por favor espera <cooldown> segundos antes de enviar otro mensaje.</red>",
            "link-blocked" to "<red>❌ No está permitido enviar enlaces o URLs en el chat.</red>",
            "profile-error" to "<red>Error al cargar tu perfil de usuario.</red>",
            "color-no-permission" to "<red>No tienes permisos para usar este color.</red>",
            "color-reset" to "<yellow>Has restablecido tu color de chat al valor por defecto.</yellow>"
        )
    }
}
