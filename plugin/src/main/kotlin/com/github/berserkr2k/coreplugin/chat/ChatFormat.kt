package com.github.berserkr2k.coreplugin.chat

/**
 * Representa un formato de chat extraído directamente de config.yml (Estilo DeluxeChat).
 */
data class ChatFormat(
    val id: String,
    val priority: Int,
    val prefix: String,
    val nameColor: String,
    val name: String,
    val suffix: String,
    val chatColor: String,
    val prefixTooltip: List<String>,
    val nameTooltip: List<String>,
    val suffixTooltip: List<String>,
    val prefixClickCmd: String,
    val nameClickCmd: String,
    val suffixClickCmd: String
)