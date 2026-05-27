package com.github.berserkr2k.coreplugin.nametag

/**
 * Representa un grupo extraído del archivo nametags.yml
 */
data class NameTagGroup(
    val id: String,
    val permission: String,
    val prefix: String,
    val suffix: String,
    val priority: Int,
    val nameColor: String // <-- AÑADIDO
)