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
     * Obtiene el template crudo (MiniMessage string) para una llave de mensaje específica.
     */
    fun getRawTemplate(key: MessageKey): String
}
