package com.github.berserkr2k.coreplugin.infrastructure.regions.listener

import com.github.berserkr2k.coreplugin.infrastructure.regions.command.PlayerSelectionSession
import com.github.berserkr2k.coreplugin.infrastructure.regions.service.RegionManager
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.block.Action
import org.bukkit.inventory.EquipmentSlot

class SelectionListener(
    private val session: PlayerSelectionSession,
    private val regionManager: RegionManager
) : Listener {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val player = event.player
        if (!player.hasPermission("core.region.setup")) return
        
        val item = event.item ?: return
        val toolMaterial = Material.matchMaterial(regionManager.config.selectionTool) ?: Material.WOODEN_AXE
        if (item.type != toolMaterial) return

        val block = event.clickedBlock ?: return

        if (event.action == Action.LEFT_CLICK_BLOCK) {
            event.isCancelled = true
            val sel = session.getSelection(player.uniqueId)
            sel.pos1 = block.location
            player.sendMessage("§a[!] Posición 1 establecida en ${block.x}, ${block.y}, ${block.z}.")
        } else if (event.action == Action.RIGHT_CLICK_BLOCK) {
            event.isCancelled = true
            val sel = session.getSelection(player.uniqueId)
            sel.pos2 = block.location
            player.sendMessage("§a[!] Posición 2 establecida en ${block.x}, ${block.y}, ${block.z}.")
        }
    }
}
