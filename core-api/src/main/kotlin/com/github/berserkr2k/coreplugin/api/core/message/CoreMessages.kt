package com.github.berserkr2k.coreplugin.api.core.message

enum class CoreMessages(override val path: String) : MessageKey {
    ONLY_PLAYERS("only-players"),

    // Chat messages
    CHAT_SOCIALSPY_ENABLED("chat.socialspy-enabled"),
    CHAT_SOCIALSPY_DISABLED("chat.socialspy-disabled"),
    CHAT_PM_USAGE("chat.pm-usage"),
    CHAT_PM_PLAYER_NOT_FOUND("chat.pm-player-not-found"),
    CHAT_PM_CANNOT_MSG_SELF("chat.pm-cannot-msg-self"),
    CHAT_REPLY_USAGE("chat.reply-usage"),
    CHAT_REPLY_NO_TARGET("chat.reply-no-target"),
    CHAT_COLOR_CHANGED("chat.color-changed"),
    CHAT_COLOR_MENU_TITLE("chat.color-menu-title"),
    CHAT_PM_SENT_FORMAT("chat.pm-sent-format"),
    CHAT_PM_RECEIVED_FORMAT("chat.pm-received-format"),
    CHAT_PM_SOCIALSPY_FORMAT("chat.pm-socialspy-format"),
    CHAT_COOLDOWN("chat.cooldown"),
    CHAT_LINK_BLOCKED("chat.link-blocked"),
    CHAT_PROFILE_ERROR("chat.profile-error"),
    CHAT_COLOR_NO_PERMISSION("chat.color-no-permission"),
    CHAT_COLOR_RESET("chat.color-reset"),

    // Leaderboards messages
    LEADERBOARD_HEADER("leaderboards.header"),
    LEADERBOARD_LOADING("leaderboards.loading"),
    LEADERBOARD_VACANT("leaderboards.vacant"),
    LEADERBOARD_ARMORSTAND_NOT_FOUND("leaderboards.armorstand-not-found"),
    LEADERBOARD_NAME_ASSIGNED("leaderboards.name-assigned"),
    LEADERBOARD_POSE_ACTIONBAR("leaderboards.pose-actionbar"),

    // Chair messages
    CHAIR_OCCUPIED("chair-occupied"),
    CHAIR_UNSAFE("chair-unsafe"),

    // Scoreboard messages
    SCOREBOARD_TOGGLE_ON("scoreboard.toggle-on"),
    SCOREBOARD_TOGGLE_OFF("scoreboard.toggle-off"),
    SCOREBOARD_RELOADED("scoreboard.reloaded");

    override val feature: String = "core"

    companion object {
        val defaults = mapOf(
            "only-players" to "<red>Solo jugadores pueden ejecutar este comando.</red>",
            "chat.socialspy-enabled" to "<green>SocialSpy habilitado.</green>",
            "chat.socialspy-disabled" to "<red>SocialSpy deshabilitado.</red>",
            "chat.pm-usage" to "<red>Uso: /msg <jugador> <mensaje></red>",
            "chat.pm-player-not-found" to "<red>Jugador no encontrado o desconectado.</red>",
            "chat.pm-cannot-msg-self" to "<red>No puedes enviarte mensajes privados a ti mismo.</red>",
            "chat.reply-usage" to "<red>Uso: /reply <mensaje></red>",
            "chat.reply-no-target" to "<red>No tienes a nadie a quien responder.</red>",
            "chat.color-changed" to "<green>¡Tu color de chat ha sido cambiado a <color>!</green>",
            "chat.color-menu-title" to "<dark_gray><bold>Selecciona un Color</bold></dark_gray>",
            "chat.pm-sent-format" to "<gray>[Yo -> <target>]: <message></gray>",
            "chat.pm-received-format" to "<gray>[<sender> -> Yo]: <message></gray>",
            "chat.pm-socialspy-format" to "<dark_gray>[Spy] [<sender> -> <target>]: <message></dark_gray>",
            "chat.cooldown" to "<red>⚠️ Por favor espera <cooldown> segundos antes de enviar otro mensaje.</red>",
            "chat.link-blocked" to "<red>❌ No está permitido enviar enlaces o URLs en el chat.</red>",
            "chat.profile-error" to "<red>Error al cargar tu perfil de usuario.</red>",
            "chat.color-no-permission" to "<red>No tienes permisos para usar este color.</red>",
            "chat.color-reset" to "<yellow>Has restablecido tu color de chat al valor por defecto.</yellow>",
            
            "leaderboards.header" to "<gold><bold>★ BILLETERA DE <top_id> ★</bold></gold>\n",
            "leaderboards.loading" to "<gold>Cargando datos del podio...</gold>",
            "leaderboards.vacant" to "<gray>#<pos> - Vacante</gray>",
            "leaderboards.armorstand-not-found" to "<red>El ArmorStand ya no existe.</red>",
            "leaderboards.name-assigned" to "<green>¡Nombre asignado con éxito!</green>",
            "leaderboards.pose-actionbar" to "<gold><bold><part> (<axis>): <angle>°</bold></gold> <gray>(Modo: <mode>)</gray>",
            "chair-occupied" to "<red>❌ ¡Esta silla ya está ocupada!</red>",
            "chair-unsafe" to "<red>❌ No puedes sentarte aquí (espacio obstruido o inseguro).</red>",
            
            "scoreboard.toggle-on" to "<green>✔ Scoreboard activado.</green>",
            "scoreboard.toggle-off" to "<red>❌ Scoreboard desactivado.</red>",
            "scoreboard.reloaded" to "<green>✔ Configuración de scoreboard recargada con éxito.</green>"
        )
    }
}
