package com.github.berserkr2k.coreplugin.infrastructure.regions.listener

import com.github.berserkr2k.coreplugin.api.regions.RegionFlags
import com.github.berserkr2k.coreplugin.api.regions.RegionQueryContext
import com.github.berserkr2k.coreplugin.api.regions.WorldIndexRegistry
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
        val block = event.block
        val loc = block.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        
        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.BLOCK_BREAK, context)) {
            event.isCancelled = true
            player.sendMessage("§c[!] No tienes permiso para romper bloques aquí.")
        }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val block = event.block
        val loc = block.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)

        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.BLOCK_PLACE, context)) {
            event.isCancelled = true
            player.sendMessage("§c[!] No tienes permiso para colocar bloques aquí.")
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return
        val damager = event.damager as? Player ?: return

        val loc = victim.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)

        val context = RegionQueryContext(victim, victim.gameMode, victim.hasPermission("core.region.bypass"))
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.PVP, context)) {
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

        val loc = block.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))

        val mat = block.type
        val flag = when {
            mat == Material.CHEST || mat == Material.TRAPPED_CHEST || mat == Material.BARREL || mat.name.contains("SHULKER_BOX") -> RegionFlags.CHEST_ACCESS
            mat == Material.ENDER_CHEST -> RegionFlags.ENDERCHEST_ACCESS
            mat.name.contains("ANVIL") -> RegionFlags.ANVIL_USE
            mat == Material.ENCHANTING_TABLE -> RegionFlags.ENCHANTING_USE
            else -> RegionFlags.INTERACT
        }

        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, flag, context)) {
            event.isCancelled = true
            val errMsg = when (flag) {
                RegionFlags.CHEST_ACCESS -> "§c[!] No tienes permiso para abrir cofres aquí."
                RegionFlags.ENDERCHEST_ACCESS -> "§c[!] No tienes permiso para abrir tu cofre de ender aquí."
                RegionFlags.ANVIL_USE -> "§c[!] No tienes permiso para usar yunques aquí."
                RegionFlags.ENCHANTING_USE -> "§c[!] No tienes permiso para usar la mesa de encantamientos aquí."
                else -> "§c[!] No tienes permiso para interactuar aquí."
            }
            player.sendMessage(errMsg)
        }
    }
}
