package com.github.berserkr2k.coreplugin.api.core.message

/**
 * Platform-level message keys shared across the entire CorePlugin platform.
 *
 * Feature-specific keys belong in the respective feature's own Messages enum
 * (e.g. SpawnMessages, ChatMessages, KitMessages, etc.) NOT here.
 */
enum class CoreMessages(override val path: String) : MessageKey {
    ONLY_PLAYERS("only-players"),
    NO_PERMISSION("no-permission"),
    PROFILE_LOAD_ERROR("profile-load-error");

    override val feature: String = "core"

    companion object {
        val defaults = mapOf(
            "only-players" to "<red>Solo jugadores pueden ejecutar este comando.</red>",
            "no-permission" to "<red>❌ No tienes permisos para realizar esta acción.</red>",
            "profile-load-error" to "<red>Error al inicializar tu perfil de usuario. Por favor reintenta ingresar en unos momentos.</red>"
        )
    }
}

