package com.github.berserkr2k.coreplugin.infrastructure.regions.listener

import com.github.berserkr2k.coreplugin.api.scheduler.RegionTaskScheduler
import com.github.berserkr2k.coreplugin.infrastructure.regions.service.RegionManager
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import java.util.UUID

class VoidDropListener(
    private val regionTaskScheduler: RegionTaskScheduler,
    private val regionManager: RegionManager
) : Listener {

    private val lastTeleportTickCache = Object2LongOpenHashMap<UUID>()

    @EventHandler
    fun onPlayerVoidDrop(event: PlayerMoveEvent) {
        if (event.from.blockY == event.to.blockY) return

        val player = event.player
        val currentServerTick = Bukkit.getCurrentTick().toLong()
        
        val cooldown = regionManager.config.voidTeleportTickCooldown.toLong()
        if (lastTeleportTickCache.getLong(player.uniqueId) >= currentServerTick - cooldown) return

        val thresholdY = regionManager.config.voidThresholdY
        if (event.to.blockY < thresholdY) {
            val destination = player.world.spawnLocation 
            
            lastTeleportTickCache.put(player.uniqueId, currentServerTick)

            regionTaskScheduler.runAtLocation(destination) {
                player.teleport(destination)
                player.fallDistance = 0.0f
            }
        }
    }
}
