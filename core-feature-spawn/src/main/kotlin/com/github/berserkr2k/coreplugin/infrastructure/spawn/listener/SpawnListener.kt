package com.github.berserkr2k.coreplugin.infrastructure.spawn.listener

import com.github.berserkr2k.coreplugin.infrastructure.spawn.service.SpawnService
import com.github.berserkr2k.coreplugin.api.scheduler.RegionTaskScheduler
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import java.util.UUID

class SpawnListener(
    private val spawnService: SpawnService,
    private val regionTaskScheduler: RegionTaskScheduler
) : Listener {

    private val lastTeleportTickCache = Object2LongOpenHashMap<UUID>()

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        // 1. Force spawn on first join check
        if (!player.hasPlayedBefore() && spawnService.config.forceSpawnOnFirstJoin) {
            val spawnLoc = spawnService.getSpawnLocation(player.world.name)
            if (spawnLoc != null) {
                // Teleport safely regionalized
                regionTaskScheduler.runAtLocation(spawnLoc) {
                    player.teleport(spawnLoc)
                }
            }
            return
        }

        // 2. Limbo recovery check (if player coordinates are corrupted or in deep void)
        if (spawnService.config.limboRecoveryEnabled) {
            val loc = player.location
            if (loc.world != null) {
                val settings = spawnService.getWorldSettings(loc.world.name)
                if (settings.voidTeleportEnabled && loc.blockY < settings.voidThresholdY) {
                    val dest = spawnService.resolveLocation(settings.spawnLocation)
                        ?: spawnService.resolveLocation(settings.safeFallbackLocation)
                        ?: spawnService.getSpawnLocation(loc.world.name)
                    if (dest != null) {
                        regionTaskScheduler.runAtLocation(dest) {
                            player.teleport(dest)
                            player.fallDistance = 0f
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val settings = spawnService.getWorldSettings(player.world.name)
        val spawnLoc = spawnService.resolveLocation(settings.spawnLocation)
            ?: spawnService.resolveLocation(settings.safeFallbackLocation)
            ?: spawnService.getSpawnLocation(player.world.name)
        if (spawnLoc != null) {
            event.respawnLocation = spawnLoc
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val uuid = player.uniqueId

        // 1. Warmup cancellation on movement check
        val from = event.from
        val to = event.to
        if (from.blockX != to.blockX || from.blockY != to.blockY || from.blockZ != to.blockZ) {
            spawnService.cancelWarmup(player, "cancelled-movement")
        }

        // 2. Void protection check
        if (event.from.blockY == event.to.blockY) return
        val currentServerTick = Bukkit.getCurrentTick().toLong()
        val cooldown = spawnService.config.voidTeleportTickCooldown.toLong()
        if (lastTeleportTickCache.getLong(uuid) >= currentServerTick - cooldown) return

        val world = event.to.world
        if (world != null) {
            val settings = spawnService.getWorldSettings(world.name)
            if (settings.voidTeleportEnabled && event.to.blockY < settings.voidThresholdY) {
                val destination = spawnService.resolveLocation(settings.spawnLocation)
                    ?: spawnService.resolveLocation(settings.safeFallbackLocation)
                    ?: spawnService.getSpawnLocation(world.name)
                    ?: player.world.spawnLocation

                lastTeleportTickCache.put(uuid, currentServerTick)

                regionTaskScheduler.runAtLocation(destination) {
                    player.teleport(destination)
                    player.fallDistance = 0f
                }
            }
        }
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        spawnService.cancelWarmup(player, "cancelled-damage")
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        spawnService.removeActiveWarmup(event.player.uniqueId)
        lastTeleportTickCache.removeLong(event.player.uniqueId)
    }
}
