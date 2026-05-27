package com.github.berserkr2k.coreplugin.command

import org.bukkit.entity.Player

/**
 * Contrato que todos los subcomandos (ej. /core testui, /core reload) deben cumplir.
 */
interface SubCommand {
    val name: String
    val permission: String?

    fun execute(player: Player, args: Array<out String>)
}