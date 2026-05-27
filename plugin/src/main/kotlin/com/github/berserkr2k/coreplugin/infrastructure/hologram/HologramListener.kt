package com.github.berserkr2k.coreplugin.infrastructure.hologram

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.plugin.Plugin
import org.bukkit.Bukkit

class HologramListener(
    private val plugin: Plugin,
    private val hologramService: HologramService
) : Listener {

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val clickedEntity = event.rightClicked
        val holo = hologramService.getHologramByInteraction(clickedEntity.uniqueId) ?: return

        // Cancelamos el evento de interacción por defecto para que no interfiera
        event.isCancelled = true

        val command = holo.clickCommand ?: return
        val player = event.player

        // Ejecución segura del comando asociado en la región espacial del jugador
        val resolvedCommand = command.replace("<player>", player.name)
        
        // El comando se ejecuta de forma asíncrona o en el planificador de región del jugador
        Bukkit.getRegionScheduler().execute(plugin, player.location) {
            player.performCommand(resolvedCommand)
        }
    }
}
