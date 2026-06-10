package com.github.berserkr2k.coreplugin.infrastructure.regions.listener

import com.github.berserkr2k.coreplugin.api.regions.RegionFlags
import com.github.berserkr2k.coreplugin.api.regions.RegionQueryContext
import com.github.berserkr2k.coreplugin.infrastructure.regions.resolver.RegionRuleResolver
import com.github.berserkr2k.coreplugin.infrastructure.regions.service.RegionManager
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent

class ProtectionListener(
    private val resolver: RegionRuleResolver,
    private val regionManager: RegionManager
) : Listener {

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
        if (!resolver.isActionAllowed(event.block.location, RegionFlags.BLOCK_BREAK, context)) {
            event.isCancelled = true
            player.sendMessage("§c[!] No tienes permiso para romper bloques aquí.")
        }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
        if (!resolver.isActionAllowed(event.block.location, RegionFlags.BLOCK_PLACE, context)) {
            event.isCancelled = true
            player.sendMessage("§c[!] No tienes permiso para colocar bloques aquí.")
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return
        val damager = event.damager as? Player ?: return

        val context = RegionQueryContext(victim, victim.gameMode, victim.hasPermission("core.region.bypass"))
        if (!resolver.isActionAllowed(victim.location, RegionFlags.PVP, context)) {
            event.isCancelled = true
            damager.sendMessage("§c[!] No se permite el PVP en esta región.")
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.LEFT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        val player = event.player

        val item = event.item
        if (item != null) {
            val toolMaterial = Material.matchMaterial(regionManager.config.selectionTool) ?: Material.WOODEN_AXE
            if (item.type == toolMaterial && player.hasPermission("core.region.setup")) {
                return
            }
        }

        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
        if (!resolver.isActionAllowed(block.location, RegionFlags.INTERACT, context)) {
            event.isCancelled = true
            player.sendMessage("§c[!] No tienes permiso para interactuar aquí.")
        }
    }
}
