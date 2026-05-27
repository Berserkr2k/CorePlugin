package com.github.berserkr2k.coreplugin.v1_21_R3.ui

import com.github.berserkr2k.coreplugin.api.ui.ScoreboardAdapter
import io.papermc.paper.scoreboard.numbers.NumberFormat
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import java.util.UUID

class ModernScoreboardAdapter : ScoreboardAdapter {

    private val boards = mutableMapOf<UUID, org.bukkit.scoreboard.Scoreboard>()
    private val mm = MiniMessage.miniMessage()
    private val serializer = LegacyComponentSerializer.legacySection()

    // Traducimos el Component (renombrado por Shadow) a un String nativo de Java para evitar crasheos
    private fun translate(text: String): String {
        return serializer.serialize(mm.deserialize(text))
    }

    override fun updateScoreboard(player: Player, title: String, lines: List<String>) {
        val board = boards.getOrPut(player.uniqueId) {
            val newBoard = Bukkit.getScoreboardManager()!!.newScoreboard

            // Usamos String nativo (Compatible con todo)
            val obj = newBoard.registerNewObjective("sidebar", "dummy", translate(title))
            obj.displaySlot = DisplaySlot.SIDEBAR

            // ¡MAGIA DE PAPER! Esto sí funciona porque no depende de Kyori
            obj.numberFormat(NumberFormat.blank())

            player.scoreboard = newBoard
            newBoard
        }

        val objective = board.getObjective("sidebar") ?: return
        objective.displayName = translate(title) // Usamos String

        val reversedLines = lines.reversed()

        for (i in 0 until 15) {
            val entry = ChatColor.values()[i].toString()

            if (i < reversedLines.size) {
                val lineText = reversedLines[i]
                val team = board.getTeam("line_$i") ?: board.registerNewTeam("line_$i").apply {
                    addEntry(entry)
                }

                // Usamos String
                team.prefix = translate(lineText)
                objective.getScore(entry).score = i
            } else {
                board.resetScores(entry)
            }
        }
    }

    override fun removeScoreboard(player: Player) {
        boards.remove(player.uniqueId)
        player.scoreboard = Bukkit.getScoreboardManager()!!.mainScoreboard
    }
}