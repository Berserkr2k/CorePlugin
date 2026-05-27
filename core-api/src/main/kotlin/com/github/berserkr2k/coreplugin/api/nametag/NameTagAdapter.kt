package com.github.berserkr2k.coreplugin.api.nametag

import org.bukkit.entity.Player

/**
 * Contrato para inyectar prefijos, sufijos y orden de prioridad (TAB) a los jugadores.
 */
interface NameTagAdapter {
    /**
     * Aplica el NameTag a un jugador.
     * @param groupName El nombre interno del grupo (ej. "Admin")
     * @param priority La prioridad para el orden del TAB (1 va más arriba que 2)
     */
    fun update(player: Player, groupName: String, priority: Int, prefix: String, suffix: String, nameColor: String)
    /**
     * Limpia los datos del jugador al desconectarse para evitar fugas de memoria.
     */
    fun remove(player: Player)
}