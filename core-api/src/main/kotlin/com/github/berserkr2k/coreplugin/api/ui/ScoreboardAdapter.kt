package com.github.berserkr2k.coreplugin.api.ui

import org.bukkit.entity.Player

/**
 * Contrato para el manejo de Scoreboards laterales (Sidebar).
 */
interface ScoreboardAdapter {
    /**
     * Crea o actualiza el Scoreboard de un jugador.
     * @param player El jugador objetivo.
     * @param title El título del menú (Soporta MiniMessage en 1.21).
     * @param lines La lista de líneas a mostrar.
     */
    fun updateScoreboard(player: Player, title: String, lines: List<String>)

    /**
     * Remueve el Scoreboard del jugador y limpia la memoria.
     */
    fun removeScoreboard(player: Player)
}