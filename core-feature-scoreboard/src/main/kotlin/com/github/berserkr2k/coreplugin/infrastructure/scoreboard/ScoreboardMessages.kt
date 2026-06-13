package com.github.berserkr2k.coreplugin.infrastructure.scoreboard

import com.github.berserkr2k.coreplugin.api.core.message.MessageKey

enum class ScoreboardMessages(override val path: String) : MessageKey {
    SCOREBOARD_TOGGLE_ON("toggle-on"),
    SCOREBOARD_TOGGLE_OFF("toggle-off"),
    SCOREBOARD_RELOADED("reloaded");

    override val feature: String = "scoreboard"

    companion object {
        val defaults = mapOf(
            "toggle-on" to "<green>✔ Scoreboard activado.</green>",
            "toggle-off" to "<red>❌ Scoreboard desactivado.</red>",
            "reloaded" to "<green>✔ Configuración de scoreboard recargada con éxito.</green>"
        )
    }
}
