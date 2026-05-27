package com.github.berserkr2k.coreplugin.scoreboard

import com.github.berserkr2k.coreplugin.CorePlugin
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask

class SidebarController(private val plugin: CorePlugin) : Listener {

    private var updateTask: BukkitTask? = null
    private val hasPapi: Boolean = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
        startTask()
    }

    private fun startTask() {
        // Actualizamos el Scoreboard cada 20 ticks (1 segundo)
        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable {
            for (player in Bukkit.getOnlinePlayers()) {
                updateBoardFor(player)
            }
        }, 20L, 20L)
    }

    private fun updateBoardFor(player: Player) {
        // Obtenemos el dinero directamente de SQLite.
        // ¡Como estamos en una tarea asíncrona, esto es 100% seguro y no da lag!
        val coins = plugin.databaseManager.getCoins(player.uniqueId)

        val rawTitle = "<gold><bold>MI SERVIDOR</bold></gold>"
        val rawLines = listOf(
            "<gray>------------------",
            " ",
            "<white>Perfil:",
            "  <gray>Jugador: <green>${player.name}", // Usamos la variable nativa de Bukkit
            "  <gray>Ping: <green>${player.ping} ms", // Usamos la variable nativa de Bukkit
            " ",
            "<white>Economía:",
            "  <gray>Dinero: <green>$<yellow>$coins", // Inyectamos nuestra variable local
            " ",
            "<yellow>play.miservidor.com",
            "<gray>------------------"
        )

        // 1. Traducimos las variables de PlaceholderAPI (Por si el usuario añade otras)
        val parsedTitle = parsePapi(player, rawTitle)
        val parsedLines = rawLines.map { parsePapi(player, it) }

        // 2. Le pasamos el texto limpio a nuestro Adaptador en el Hilo Principal
        Bukkit.getScheduler().runTask(plugin, Runnable {
            if (player.isOnline) {
                plugin.scoreboardAdapter.updateScoreboard(player, parsedTitle, parsedLines)
            }
        })
    }

    private fun parsePapi(player: Player, text: String): String {
        return if (hasPapi) {
            PlaceholderAPI.setPlaceholders(player, text)
        } else {
            text // Si no tienen PAPI, devolvemos el texto original
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        // Creamos el Scoreboard inicial al entrar
        updateBoardFor(event.player)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        // Limpiamos la memoria al salir
        plugin.scoreboardAdapter.removeScoreboard(event.player)
    }

    fun shutdown() {
        updateTask?.cancel()
        for (player in Bukkit.getOnlinePlayers()) {
            plugin.scoreboardAdapter.removeScoreboard(player)
        }
    }
}