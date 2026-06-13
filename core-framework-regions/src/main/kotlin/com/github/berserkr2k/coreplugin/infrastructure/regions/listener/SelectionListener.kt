package com.github.berserkr2k.coreplugin.infrastructure.regions.listener

import com.github.berserkr2k.coreplugin.infrastructure.regions.command.PlayerSelectionSession
import com.github.berserkr2k.coreplugin.infrastructure.regions.service.RegionManager
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import com.github.berserkr2k.coreplugin.infrastructure.regions.RegionMessages
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.block.Action
import org.bukkit.inventory.EquipmentSlot

class SelectionListener(
    private val regionManager: RegionManager
) : Listener {

    private val registry = org.bukkit.Bukkit.getServicesManager().load(com.github.berserkr2k.coreplugin.api.di.ServiceRegistry::class.java)
        ?: throw IllegalStateException("ServiceRegistry not found in ServicesManager")
    private val messageService = registry.get(MessageService::class.java)!!
    private val session = regionManager.selectionSession

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
            messageService.send(player, RegionMessages.SELECTION_POS1, PlaceholderContext.of(
                "x" to block.x.toString(),
                "y" to block.y.toString(),
                "z" to block.z.toString()
            ))
        } else if (event.action == Action.RIGHT_CLICK_BLOCK) {
            event.isCancelled = true
            val sel = session.getSelection(player.uniqueId)
            sel.pos2 = block.location
            messageService.send(player, RegionMessages.SELECTION_POS2, PlaceholderContext.of(
                "x" to block.x.toString(),
                "y" to block.y.toString(),
                "z" to block.z.toString()
            ))
        }
    }
}
