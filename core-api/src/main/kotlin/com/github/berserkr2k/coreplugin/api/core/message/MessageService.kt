package com.github.berserkr2k.coreplugin.api.core.message

import net.kyori.adventure.audience.Audience

interface MessageService {
    /**
     * Envía un mensaje traducido y formateado a una audiencia específica.
     */
    fun send(
        audience: Audience,
        key: MessageKey,
        placeholders: PlaceholderContext = PlaceholderContext.empty()
    )

    /**
     * Envía un mensaje raw (MiniMessage string) a una audiencia específica sin usar una key.
     */
    fun sendRaw(audience: Audience, message: String)

    /**
     * Obtiene el template crudo (MiniMessage string) para una llave de mensaje específica.
     */
    fun getRawTemplate(key: MessageKey): String

    /**
     * Registra las llaves y mensajes por defecto de una característica.
     */
    fun registerFeature(featureId: String, defaultMessages: Map<String, String> = emptyMap())
}
