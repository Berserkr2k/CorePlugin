package com.github.berserkr2k.coreplugin.infrastructure.hologram

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin

class HologramListener(
    private val plugin: Plugin,
    private val hologramService: HologramService
) : Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        hologramService.handleQuit(event.player)
    }
}
