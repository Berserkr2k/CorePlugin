package com.github.berserkr2k.coreplugin.infrastructure.regions.listener

import com.github.berserkr2k.coreplugin.api.framework.regions.RegionFlags
import com.github.berserkr2k.coreplugin.api.framework.regions.RegionQueryContext
import com.github.berserkr2k.coreplugin.infrastructure.regions.WorldIndexRegistry
import com.github.berserkr2k.coreplugin.infrastructure.regions.resolver.RegionRuleResolver
import com.github.berserkr2k.coreplugin.infrastructure.regions.service.RegionManager
import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.common.sendRawMessage
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
import org.bukkit.entity.TNTPrimed
import org.bukkit.entity.Creeper
import org.bukkit.entity.Fireball
import org.bukkit.entity.LargeFireball
import org.bukkit.entity.Enderman
import org.bukkit.entity.Animals
import org.bukkit.entity.Ambient
import org.bukkit.entity.Fish
import org.bukkit.entity.Firework
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.block.BlockFormEvent
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.event.block.BlockGrowEvent
import org.bukkit.event.world.StructureGrowEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.player.PlayerExpChangeEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.vehicle.VehicleCreateEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.event.vehicle.VehicleDamageEvent
import com.destroystokyo.paper.event.block.AnvilDamagedEvent
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.infrastructure.regions.RegionMessages

class ProtectionListener(
    private val regionManager: RegionManager
) : Listener {

    private val registry = org.bukkit.Bukkit.getServicesManager().load(com.github.berserkr2k.coreplugin.api.di.ServiceRegistry::class.java)
        ?: throw IllegalStateException("ServiceRegistry not found in ServicesManager")

    private val resolver = regionManager.resolver
    private val messageService = registry.get(MessageService::class.java)!!

    private fun shouldSendMessage(flag: Long): Boolean {
        val category = RegionFlags.getCategoryOfFlag(flag) ?: return true
        val config = regionManager.config
        return when (category) {
            "COMBAT" -> config.enableCombatMessages
            "BUILDING" -> config.enableWorldMessages
            "INTERACTIONS" -> config.enableInteractionMessages
            "ENVIRONMENT" -> config.enableWorldMessages
            "ENTITIES" -> config.enableEntityMessages
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
                messageService.send(player, RegionMessages.NO_BREAK)
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
                messageService.send(player, RegionMessages.NO_PLACE)
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
                if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.PVP, context)) {
                    event.isCancelled = true
                    val shooter = damager.shooter as? Player
                    if (shooter != null && shouldSendMessage(RegionFlags.PVP)) {
                        messageService.send(shooter, RegionMessages.NO_PVP)
                    }
                }
            } else if (damager is Player) {
                val context = RegionQueryContext(victim, victim.gameMode, victim.hasPermission("core.region.bypass"))
                if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.PVP, context)) {
                    event.isCancelled = true
                    if (shouldSendMessage(RegionFlags.PVP)) {
                        messageService.send(damager, RegionMessages.NO_PVP)
                    }
                }
            }
        } else if (victim is Animals || victim is Ambient || victim is Fish) {
            val playerDamager = if (damager is Player) damager else if (damager is Projectile) damager.shooter as? Player else null
            val context = playerDamager?.let { RegionQueryContext(it, it.gameMode, it.hasPermission("core.region.bypass")) }
                ?: RegionQueryContext(null, null, false)
            if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.DAMAGE_ANIMALS, context)) {
                event.isCancelled = true
                if (playerDamager != null && shouldSendMessage(RegionFlags.DAMAGE_ANIMALS)) {
                    messageService.send(playerDamager, RegionMessages.NO_DAMAGE_ANIMALS)
                }
            }
        } else if (isHostile(victim)) {
            val playerDamager = if (damager is Player) damager else if (damager is Projectile) damager.shooter as? Player else null
            val context = playerDamager?.let { RegionQueryContext(it, it.gameMode, it.hasPermission("core.region.bypass")) }
                ?: RegionQueryContext(null, null, false)
            if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.MOB_DAMAGE, context)) {
                event.isCancelled = true
            }
        } else if (victim is ArmorStand || victim is ItemFrame || victim is GlowItemFrame) {
            val playerDamager = if (damager is Player) damager else if (damager is Projectile) damager.shooter as? Player else null
            val context = playerDamager?.let { RegionQueryContext(it, it.gameMode, it.hasPermission("core.region.bypass")) }
                ?: RegionQueryContext(null, null, false)
            if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.INTERACT, context)) {
                event.isCancelled = true
                if (playerDamager != null && shouldSendMessage(RegionFlags.INTERACT)) {
                    messageService.send(playerDamager, RegionMessages.NO_INTERACT)
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
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.INTERACT, context)) {
            event.isCancelled = true
            if (shouldSendMessage(RegionFlags.INTERACT)) {
                messageService.send(player, RegionMessages.NO_INTERACT)
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

        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.INTERACT, context)) {
            event.isCancelled = true
            if (shouldSendMessage(RegionFlags.INTERACT)) {
                messageService.send(player, RegionMessages.NO_INTERACT)
            }
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val block = event.clickedBlock

        val item = event.item
        if (item != null) {
            val toolMaterial = Material.matchMaterial(regionManager.config.selectionTool) ?: Material.WOODEN_AXE
            if (item.type == toolMaterial && player.hasPermission("core.region.setup")) {
                return
            }
            // Check vehicle-place directly from hand
            if (event.action == Action.RIGHT_CLICK_BLOCK && (item.type.name.contains("MINECART") || item.type.name.contains("BOAT"))) {
                val loc = block?.location ?: player.location
                val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
                val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
                if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.VEHICLE_PLACE, context)) {
                    event.isCancelled = true
                    if (shouldSendMessage(RegionFlags.VEHICLE_PLACE)) {
                        messageService.send(player, RegionMessages.NO_VEHICLE_PLACE)
                    }
                    return
                }
            }
        }

        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (block == null) return

        val loc = block.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))

        val mat = block.type
        val isUseBlock = mat.name.contains("DOOR") ||
                         mat.name.contains("GATE") ||
                         mat.name.contains("TRAPDOOR") ||
                         mat.name.contains("BUTTON") ||
                         mat == Material.LEVER ||
                         mat.name.contains("PRESSURE_PLATE") ||
                         mat == Material.TRIPWIRE_HOOK ||
                         mat == Material.DAYLIGHT_DETECTOR ||
                         mat == Material.CHEST || mat == Material.TRAPPED_CHEST || mat == Material.BARREL || mat.name.contains("SHULKER_BOX") ||
                         mat == Material.ENDER_CHEST ||
                         mat.name.contains("ANVIL") ||
                         mat == Material.ENCHANTING_TABLE ||
                         mat == Material.LECTERN ||
                         mat == Material.REPEATER ||
                         mat == Material.COMPARATOR ||
                         block.state is org.bukkit.block.Container || mat.name.contains("FURNACE") || mat == Material.DISPENSER || mat == Material.DROPPER || mat == Material.HOPPER || mat == Material.BREWING_STAND ||
                         mat == Material.BELL || mat == Material.JUKEBOX || mat == Material.NOTE_BLOCK || mat == Material.BEACON || mat == Material.CAULDRON || mat.name.contains("BED")

        val flag = when (mat) {
            Material.RESPAWN_ANCHOR -> RegionFlags.RESPAWN_ANCHORS
            else -> if (isUseBlock) RegionFlags.USE else RegionFlags.INTERACT
        }

        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, flag, context)) {
            event.isCancelled = true
            if (shouldSendMessage(flag)) {
                messageService.send(player, RegionMessages.NO_INTERACT)
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
        // Check general build or block-break/place? We can skip checking block physics unless specifically desired, or fallback to build
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.BUILD, context)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        val loc = player.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
        // Item dropping can fall under interact or be allowed by default. We will check interact as a generic action
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.INTERACT, context)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onEntityPickupItem(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        val loc = player.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.INTERACT, context)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onEntityTarget(event: EntityTargetLivingEntityEvent) {
        // We will keep this default allowed for mobs targetting unless they are protected.
        // Usually mob-targeting is a minor flag; we can check INTERACT or keep it allowed
    }

    @EventHandler
    fun onBlockFromTo(event: BlockFromToEvent) {
        val fromBlock = event.block
        val toBlock = event.toBlock
        val loc = toBlock.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(null, null, false)
        val mat = fromBlock.type
        val flag = when {
            mat == Material.WATER -> RegionFlags.WATER_FLOW
            mat == Material.LAVA -> RegionFlags.LAVA_FLOW
            else -> return
        }
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, flag, context)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        val victim = event.entity
        val loc = victim.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)

        if (victim is Player) {
            val context = RegionQueryContext(victim, victim.gameMode, victim.hasPermission("core.region.bypass"))
            // 1. Invincible check
            if (resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.INVINCIBLE, context)) {
                event.isCancelled = true
                return
            }
            // 2. Fall damage check
            if (event.cause == EntityDamageEvent.DamageCause.FALL) {
                if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.FALL_DAMAGE, context)) {
                    event.isCancelled = true
                    return
                }
            }
        } else {
            val context = RegionQueryContext(null, null, false)
            if (victim is Animals || victim is Ambient || victim is Fish) {
                if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.DAMAGE_ANIMALS, context)) {
                    event.isCancelled = true
                    return
                }
            } else if (isHostile(victim)) {
                if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.MOB_DAMAGE, context)) {
                    event.isCancelled = true
                    return
                }
            }
        }
    }

    @EventHandler
    fun onEntityToggleGlide(event: EntityToggleGlideEvent) {
        // Glide is allowed
    }

    @EventHandler
    fun onBlockRedstone(event: org.bukkit.event.block.BlockRedstoneEvent) {
        // Redstone interaction is allowed by default, we don't block physics redstone lines
    }

    @EventHandler
    fun onVehicleCreate(event: VehicleCreateEvent) {
        val vehicle = event.vehicle
        val loc = vehicle.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(null, null, false)
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.VEHICLE_PLACE, context)) {
            // Remove vehicle if not allowed
            vehicle.remove()
        }
    }

    @EventHandler
    fun onVehicleEnter(event: VehicleEnterEvent) {
        val player = event.entered as? Player ?: return
        val loc = event.vehicle.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.USE, context)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onVehicleDamage(event: VehicleDamageEvent) {
        val attacker = event.attacker as? Player
        val loc = event.vehicle.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = attacker?.let { RegionQueryContext(it, it.gameMode, it.hasPermission("core.region.bypass")) }
            ?: RegionQueryContext(null, null, false)
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.VEHICLE_DESTROY, context)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerExpChange(event: PlayerExpChangeEvent) {
        // Exp gain allowed
    }

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        // Hunger loss allowed
    }

    @EventHandler
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        val entity = event.entity
        val loc = event.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(null, null, false)
        
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.MOB_SPAWNING, context)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockBurn(event: BlockBurnEvent) {
        val block = event.block
        val loc = block.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(null, null, false)
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.FIRE_SPREAD, context)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockSpread(event: BlockSpreadEvent) {
        val block = event.block
        val source = event.source
        val loc = block.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(null, null, false)
        val flag = when (source.type) {
            Material.FIRE -> RegionFlags.FIRE_SPREAD
            Material.GRASS_BLOCK -> RegionFlags.GRASS_GROWTH
            Material.MYCELIUM -> RegionFlags.MYCELIUM_SPREAD
            else -> return
        }
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, flag, context)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockIgnite(event: BlockIgniteEvent) {
        val block = event.block
        val loc = block.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(null, null, false)
        if (event.cause == BlockIgniteEvent.IgniteCause.LAVA) {
            if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.LAVA_FIRE, context)) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onBlockForm(event: BlockFormEvent) {
        val block = event.block
        val loc = block.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(null, null, false)
        val mat = event.newState.type
        val flag = when (mat) {
            Material.SNOW, Material.SNOW_BLOCK -> RegionFlags.SNOW_FALL
            Material.ICE -> RegionFlags.ICE_FORM
            else -> return
        }
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, flag, context)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockFade(event: BlockFadeEvent) {
        val block = event.block
        val loc = block.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(null, null, false)
        val mat = block.type
        val flag = when (mat) {
            Material.SNOW, Material.SNOW_BLOCK -> RegionFlags.SNOW_MELT
            Material.ICE, Material.PACKED_ICE, Material.FROSTED_ICE -> RegionFlags.ICE_MELT
            else -> return
        }
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, flag, context)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onLeavesDecay(event: LeavesDecayEvent) {
        val block = event.block
        val loc = block.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(null, null, false)
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.LEAF_DECAY, context)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockGrow(event: BlockGrowEvent) {
        val block = event.block
        val loc = block.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(null, null, false)
        val mat = event.newState.type
        if (mat == Material.BROWN_MUSHROOM || mat == Material.RED_MUSHROOM) {
            if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.MUSHROOM_GROWTH, context)) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onStructureGrow(event: StructureGrowEvent) {
        val loc = event.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(null, null, false)
        val type = event.species
        if (type == org.bukkit.TreeType.BROWN_MUSHROOM || type == org.bukkit.TreeType.RED_MUSHROOM) {
            if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.MUSHROOM_GROWTH, context)) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        val player = event.player
        val to = event.to
        val worldIndex = WorldIndexRegistry.getIndex(to.world.uid)
        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
        val cause = event.cause
        val flag = when (cause) {
            PlayerTeleportEvent.TeleportCause.ENDER_PEARL -> RegionFlags.ENDERPEARL
            PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT -> RegionFlags.CHORUS_FRUIT_TELEPORT
            else -> return
        }
        if (!resolver.isActionAllowed(worldIndex, to.blockX, to.blockY, to.blockZ, flag, context)) {
            event.isCancelled = true
            if (shouldSendMessage(flag)) {
                if (flag == RegionFlags.ENDERPEARL) {
                    player.sendRawMessage(ColorUtility.parse("<red>❌ No se permite el uso de enderpearls aquí.</red>"))
                } else {
                    player.sendRawMessage(ColorUtility.parse("<red>❌ No se permite el teletransporte con fruta de coro aquí.</red>"))
                }
            }
        }
    }

    @EventHandler
    fun onPlayerBedEnter(event: PlayerBedEnterEvent) {
        val player = event.player
        val bed = event.bed
        val loc = bed.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(player, player.gameMode, player.hasPermission("core.region.bypass"))
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.SLEEP, context)) {
            event.isCancelled = true
            player.sendRawMessage(ColorUtility.parse("<red>❌ No tienes permiso para dormir aquí.</red>"))
        }
    }

    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) {
        val entity = event.entity
        val loc = event.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(null, null, false)
        val flag = when (entity) {
            is TNTPrimed -> RegionFlags.TNT
            is Creeper -> RegionFlags.CREEPER_EXPLOSION
            is Fireball, is LargeFireball -> RegionFlags.GHAST_FIREBALL
            else -> RegionFlags.OTHER_EXPLOSION
        }
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, flag, context)) {
            event.blockList().clear()
            event.yield = 0.0f
        }
    }

    @EventHandler
    fun onBlockExplode(event: BlockExplodeEvent) {
        val block = event.block
        val loc = block.location
        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
        val context = RegionQueryContext(null, null, false)
        if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.OTHER_EXPLOSION, context)) {
            event.blockList().clear()
            event.yield = 0.0f
        }
    }

    @EventHandler
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        val entity = event.entity
        if (entity is Enderman) {
            val block = event.block
            val loc = block.location
            val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
            val context = RegionQueryContext(null, null, false)
            if (!resolver.isActionAllowed(worldIndex, loc.blockX, loc.blockY, loc.blockZ, RegionFlags.ENDERMAN_GRIEF, context)) {
                event.isCancelled = true
            }
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
