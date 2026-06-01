package com.github.berserkr2k.coreplugin.common

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.util.regex.Pattern

object ColorUtility {
    private val miniMessage = MiniMessage.miniMessage()
    private val legacySectionSerializer = LegacyComponentSerializer.legacySection()
    private val HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})")

    /**
     * Parsea un texto soportando tanto MiniMessage (<red>, <bold>) como el formato heredado 
     * (&c, &l, &#FF0000) de forma combinada y segura.
     */
    fun parse(text: String?): Component {
        if (text == null || text.isEmpty()) return Component.empty()

        // Traducir <purple> y </purple> no soportados nativamente por MiniMessage a <dark_purple> y </dark_purple>
        var processed = text.replace(Regex("(?i)<purple>"), "<dark_purple>")
        processed = processed.replace(Regex("(?i)</purple>"), "</dark_purple>")

        // 1. Traducir códigos hex legacy &#rrggbb a &x&r&r&g&g&b&b
        val processedHex = translateHexColorCodes(processed)

        // 2. Deserializar con MiniMessage para resolver etiquetas modernas (<red>, etc.)
        val mmComp = miniMessage.deserialize(processedHex)

        // 3. Serializar a formato heredado con caracteres de sección (§)
        val legacyStr = legacySectionSerializer.serialize(mmComp)

        // 4. Reemplazar todos los '&' por '§' para activar códigos legados originales en el texto
        val translated = legacyStr.replace('&', '§')

        // 5. Deserializar de nuevo mediante el serializador de secciones heredadas
        return legacySectionSerializer.deserialize(translated)
    }

    private fun translateHexColorCodes(text: String): String {
        val matcher = HEX_PATTERN.matcher(text)
        val sb = StringBuffer()
        while (matcher.find()) {
            val hex = matcher.group(1)
            val replacement = StringBuilder("&x")
            for (char in hex) {
                replacement.append("&").append(char)
            }
            matcher.appendReplacement(sb, replacement.toString())
        }
        matcher.appendTail(sb)
        return sb.toString()
    }
}
