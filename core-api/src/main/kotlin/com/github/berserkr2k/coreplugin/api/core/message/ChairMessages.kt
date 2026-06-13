package com.github.berserkr2k.coreplugin.api.core.message

/**
 * Message keys for the platform-level Chair (sit) system.
 *
 * Chair is a core platform mechanic (not a feature), so its messages live in core-api.
 */
enum class ChairMessages(override val path: String) : MessageKey {
    CHAIR_OCCUPIED("chair-occupied"),
    CHAIR_UNSAFE("chair-unsafe");

    override val feature: String = "core"

    companion object {
        val defaults = mapOf(
            "chair-occupied" to "<red>❌ ¡Esta silla ya está ocupada!</red>",
            "chair-unsafe" to "<red>❌ No puedes sentarte aquí (espacio obstruido o inseguro).</red>"
        )
    }
}
