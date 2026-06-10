package com.github.berserkr2k.coreplugin.infrastructure.regions.listener

import com.github.berserkr2k.coreplugin.api.regions.RegionFlags
import com.github.berserkr2k.coreplugin.api.regions.RegionQueryContext
import com.github.berserkr2k.coreplugin.api.regions.WorldIndexRegistry
import com.github.berserkr2k.coreplugin.infrastructure.regions.resolver.RegionRuleResolver
import com.github.berserkr2k.coreplugin.infrastructure.regions.service.RegionManager
import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.getRegions
import com.github.berserkr2k.coreplugin.common.ColorUtility
import org.bukkit.Material
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.Enemy
import org.bukkit.entity.Monster
import org.bukkit.entity.Slime
import org.bukkit.entity.Ghast
import org.bukkit.entity.Phantom
import org.bukkit.entity.Shulker
import org.bukkit.entity.Boss
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.GlowItemFrame
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockRedstoneEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.player.PlayerExpChangeEvent
import org.bukkit.event.vehicle.VehicleCreateEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.event.vehicle.VehicleDamageEvent
import com.destroystokyo.paper.event.block.AnvilDamagedEvent

class ProtectionListener(
    private val resolver: RegionRuleResolver,
    private val regionManager: RegionManager,
    private val messagesConfig: MessagesConfig
) : Listener {

    private fun shouldSendMessage(flag: Int): Boolean {
        val category = RegionFlags.getCategoryOfFlag(flag) ?: return true
        val config = regionManager.config
        return when (category) {
            "COMBAT" -> config.enableCombatMessages
            "WORLD" -> config.enableWorldMessages
            "INTERACTION" -> config.enableInteractionMessages
            "PLAYER" -> config.enablePlayerMessages
            "ENTITY" -> config.enableEntityMessages
            else -> true
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block
        val loc = block.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        
        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.BLOCK_BREAK, context)) {
            event.isCancelled = true
            if (shouldSendMessage(RegionFlags.BLOCK_BREAK)) {
                player.sendMessage(ColorUtility.parse(messagesConfig.getRegions("no-break")))
            }
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
            if (shouldSendMessage(RegionFlags.BLOCK_PLACE)) {
                player.sendMessage(ColorUtility.parse(messagesConfig.getRegions("no-place")))
            }
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val victim = event.entity
        val damager = event.damager
        val loc = victim.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)

        if (victim is Player) {
            if (damager is Projectile) {
                val context = RegionQueryContext(victim, victim.gameMode, victim.hasPermission("core.region.bypass"))
                if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.PROJECTILE_DAMAGE, context)) {
                    event.isCancelled = true
                    val shooter = damager.shooter as? Player
                    if (shooter != null && shouldSendMessage(RegionFlags.PROJECTILE_DAMAGE)) {
                        shooter.sendMessage(ColorUtility.parse(messagesConfig.getRegions("no-projectile-damage")))
                    }
                }
            } else if (damager is Player) {
                val context = RegionQueryContext(victim, victim.gameMode, victim.hasPermission("core.region.bypass"))
                if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.PVP, context)) {
                    event.isCancelled = true
                    if (shouldSendMessage(RegionFlags.PVP)) {
                        damager.sendMessage(ColorUtility.parse(messagesConfig.getRegions("no-pvp")))
                    }
                }
            }
        } else if (victim is ArmorStand) {
            val playerDamager = damager as? Player
            val context = playerDamager?.let { RegionQueryContext(it, it.gameMode, it.hasPermission("core.region.bypass")) }
                ?: RegionQueryContext(null, null, false)
            if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.ARMOR_STAND_INTERACTION, context)) {
                event.isCancelled = true
                if (playerDamager != null && shouldSendMessage(RegionFlags.ARMOR_STAND_INTERACTION)) {
                    playerDamager.sendMessage(ColorUtility.parse(messagesConfig.getRegions("no-armor-stand-interaction")))
                }
            }
        } else if (victim is ItemFrame || victim is GlowItemFrame) {
            val playerDamager = damager as? Player
            val context = playerDamager?.let { RegionQueryContext(it, it.gameMode, it.hasPermission("core.region.bypass")) }
                ?: RegionQueryContext(null, null, false)
            if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.ITEM_FRAME_INTERACTION, context)) {
                event.isCancelled = true
                if (playerDamager != null && shouldSendMessage(RegionFlags.ITEM_FRAME_INTERACTION)) {
                    playerDamager.sendMessage(ColorUtility.parse(messagesConfig.getRegions("no-item-frame-interaction")))
                }
            }
        }
    }

    @EventHandler
    fun onPlayerInteractAtEntity(event: PlayerInteractAtEntityEvent) {
        val stand = event.rightClicked as? ArmorStand ?: return
        val player = event.player
        val loc = stand.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.ARMOR_STAND_INTERACTION, context)) {
            event.isCancelled = true
            if (shouldSendMessage(RegionFlags.ARMOR_STAND_INTERACTION)) {
                player.sendMessage(ColorUtility.parse(messagesConfig.getRegions("no-armor-stand-interaction")))
            }
        }
    }

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked
        val player = event.player
        val loc = entity.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))

        val flag = when (entity) {
            is ItemFrame, is GlowItemFrame -> RegionFlags.ITEM_FRAME_INTERACTION
            is ArmorStand -> return // handled in onPlayerInteractAtEntity
            else -> RegionFlags.ENTITY_INTERACTION
        }

        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, flag, context)) {
            event.isCancelled = true
            if (shouldSendMessage(flag)) {
                val errMsgKey = when (flag) {
                    RegionFlags.ITEM_FRAME_INTERACTION -> "no-item-frame-interaction"
                    else -> "no-entity-interaction"
                }
                player.sendMessage(ColorUtility.parse(messagesConfig.getRegions(errMsgKey)))
            }
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
            mat.name.contains("BUTTON") || mat == Material.LEVER || mat.name.contains("PRESSURE_PLATE") || mat == Material.TRIPWIRE_HOOK || mat == Material.DAYLIGHT_DETECTOR -> RegionFlags.REDSTONE_INTERACTION
            block.state is org.bukkit.block.Container || mat.name.contains("FURNACE") || mat == Material.DISPENSER || mat == Material.DROPPER || mat == Material.HOPPER || mat == Material.BREWING_STAND -> RegionFlags.CONTAINER_INTERACTION
            else -> RegionFlags.INTERACT
        }

        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, flag, context)) {
            event.isCancelled = true
            if (shouldSendMessage(flag)) {
                val errMsgKey = when (flag) {
                    RegionFlags.CHEST_ACCESS -> "no-chest-access"
                    RegionFlags.ENDERCHEST_ACCESS -> "no-enderchest-access"
                    RegionFlags.ANVIL_USE -> "no-anvil-use"
                    RegionFlags.ENCHANTING_USE -> "no-enchanting-use"
                    RegionFlags.REDSTONE_INTERACTION -> "no-redstone-interaction"
                    RegionFlags.CONTAINER_INTERACTION -> "no-container-interaction"
                    else -> "no-interact"
                }
                player.sendMessage(ColorUtility.parse(messagesConfig.getRegions(errMsgKey)))
            }
        }
    }

    @EventHandler
    fun onPlayerItemDamage(event: PlayerItemDamageEvent) {
        val player = event.player
        val loc = player.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
        if (resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.USE_WITHOUT_BREAK, context)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onAnvilDamaged(event: AnvilDamagedEvent) {
        val loc = event.inventory.location ?: return
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(null, null, false)
        if (resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.USE_WITHOUT_BREAK, context)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockPhysics(event: BlockPhysicsEvent) {
        val block = event.block
        val loc = block.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(null, null, false)
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.BLOCK_PHYSICS, context)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        val loc = player.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.ITEM_DROP, context)) {
            event.isCancelled = true
            if (shouldSendMessage(RegionFlags.ITEM_DROP)) {
                player.sendMessage(ColorUtility.parse(messagesConfig.getRegions("no-item-drop")))
            }
        }
    }

    @EventHandler
    fun onEntityPickupItem(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        val loc = player.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.ITEM_PICKUP, context)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onEntityTarget(event: EntityTargetLivingEntityEvent) {
        val player = event.target as? Player ?: return
        val loc = player.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.MOB_TARGETING, context)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockFromTo(event: BlockFromToEvent) {
        val toBlock = event.toBlock
        val loc = toBlock.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(null, null, false)
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.LIQUID_FLOW, context)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (event.cause == EntityDamageEvent.DamageCause.FALL) {
            val loc = player.location
            val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
            val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
            if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.FALL_DAMAGE, context)) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onEntityToggleGlide(event: EntityToggleGlideEvent) {
        val player = event.entity as? Player ?: return
        if (event.isGliding) {
            val loc = player.location
            val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
            val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
            if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.ELYTRA_USAGE, context)) {
                event.isCancelled = true
                if (shouldSendMessage(RegionFlags.ELYTRA_USAGE)) {
                    player.sendMessage(ColorUtility.parse(messagesConfig.getRegions("no-elytra-usage")))
                }
            }
        }
    }

    @EventHandler
    fun onBlockRedstone(event: BlockRedstoneEvent) {
        val block = event.block
        val loc = block.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(null, null, false)
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.REDSTONE_INTERACTION, context)) {
            event.newCurrent = event.oldCurrent
        }
    }

    @EventHandler
    fun onVehicleCreate(event: VehicleCreateEvent) {
        val vehicle = event.vehicle
        val loc = vehicle.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(null, null, false)
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.VEHICLE_USAGE, context)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onVehicleEnter(event: VehicleEnterEvent) {
        val player = event.entered as? Player ?: return
        val loc = event.vehicle.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.VEHICLE_USAGE, context)) {
            event.isCancelled = true
            if (shouldSendMessage(RegionFlags.VEHICLE_USAGE)) {
                player.sendMessage(ColorUtility.parse(messagesConfig.getRegions("no-vehicle-usage")))
            }
        }
    }

    @EventHandler
    fun onVehicleDamage(event: VehicleDamageEvent) {
        val attacker = event.attacker as? Player
        val loc = event.vehicle.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = attacker?.let { RegionQueryContext(it, it.gameMode, it.hasPermission("core.region.bypass")) }
            ?: RegionQueryContext(null, null, false)
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.VEHICLE_USAGE, context)) {
            event.isCancelled = true
            if (attacker != null && shouldSendMessage(RegionFlags.VEHICLE_USAGE)) {
                attacker.sendMessage(ColorUtility.parse(messagesConfig.getRegions("no-vehicle-usage")))
            }
        }
    }

    @EventHandler
    fun onPlayerExpChange(event: PlayerExpChangeEvent) {
        val player = event.player
        val loc = player.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.EXP_GAIN, context)) {
            event.amount = 0
        }
    }

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        val player = event.entity as? Player ?: return
        if (event.foodLevel < player.foodLevel) {
            val loc = player.location
            val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
            val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
            if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.HUNGER_LOSS, context)) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        val entity = event.entity
        val loc = event.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(null, null, false)
        
        val isHostileEntity = isHostile(entity)
        val flagToCheck = if (isHostileEntity) RegionFlags.HOSTILE_SPAWN else RegionFlags.PASSIVE_SPAWN
        
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, flagToCheck, context)) {
            event.isCancelled = true
        }
    }

    private fun isHostile(entity: org.bukkit.entity.Entity): Boolean {
        return entity is Monster ||
               entity is Enemy ||
               entity is Slime ||
               entity is Ghast ||
               entity is Phantom ||
               entity is Shulker ||
               entity is Boss ||
               entity.javaClass.simpleName.contains("Dragon") ||
               entity.javaClass.simpleName.contains("Wither")
    }
}
