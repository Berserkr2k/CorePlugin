package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ArmorStandEditorListener(
    private val plugin: Plugin,
    private val leaderboardService: LeaderboardService,
    private val messagesConfig: MessagesConfig
) : Listener {

    private val editorKey = NamespacedKey(plugin, "stand_editor_tool")
    
    companion object {
        // Almacenamiento seguro multihilo para seguir el objetivo editado de cada jugador en Folia
        val playerEditingSessions = ConcurrentHashMap<UUID, UUID>()
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEntityEvent) {
        val player = event.player
        val target = event.rightClicked as? ArmorStand ?: return
        val item = player.inventory.itemInMainHand

        if (item.type.isAir) return
        val meta = item.itemMeta ?: return

        // Validación atómica de la Vara de Edición mediante PDC
        if (meta.persistentDataContainer.has(editorKey, PersistentDataType.BOOLEAN)) {
            event.isCancelled = true
            playerEditingSessions[player.uniqueId] = target.uniqueId
            
            // Abrir la GUI nativa del editor
            ArmorStandEditorGui.open(player, target)
        }
    }
}
