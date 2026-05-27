package com.github.berserkr2k.coreplugin.placeholder

import com.github.berserkr2k.coreplugin.CorePlugin
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player

/**
 * Registra nuestras propias variables en PlaceholderAPI para que cualquier plugin pueda usarlas.
 */
class CorePlaceholderExpansion(private val plugin: CorePlugin) : PlaceholderExpansion() {

    // El prefijo de nuestras variables (ej. %core_...%)
    override fun getIdentifier(): String = "core"

    override fun getAuthor(): String = plugin.description.authors.joinToString(", ")

    override fun getVersion(): String = plugin.description.version

    // Importante: Esto evita que PAPI borre nuestras variables si un admin hace /papi reload
    override fun persist(): Boolean = true

    // Aquí es donde ocurre la magia cuando alguien pide una variable
    override fun onPlaceholderRequest(player: Player?, params: String): String {
        if (player == null) return ""

        return when (params.lowercase()) {
            "nickname" -> {
                // Obtenemos el color desde el caché de nuestro ChatManager
                val color = plugin.chatManager.getPlayerColor(player.uniqueId)
                "$color${player.name}"
            }
            "coins" -> {
                // Consultamos directamente desde SQLite
                plugin.databaseManager.getCoins(player.uniqueId).toString()
            }
            else -> "" // Si no existe la variable, devolvemos vacío
        }
    }
}