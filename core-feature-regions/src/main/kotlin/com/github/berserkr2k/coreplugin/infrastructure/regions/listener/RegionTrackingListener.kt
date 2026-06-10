package com.github.berserkr2k.coreplugin.infrastructure.regions.listener

import com.github.berserkr2k.coreplugin.api.regions.WorldIndexRegistry
import com.github.berserkr2k.coreplugin.api.event.CoreEventBus
import com.github.berserkr2k.coreplugin.api.state.PlayerStateService
import com.github.berserkr2k.coreplugin.infrastructure.regions.event.PlayerRegionEnterEvent
import com.github.berserkr2k.coreplugin.infrastructure.regions.event.PlayerRegionLeaveEvent
import com.github.berserkr2k.coreplugin.infrastructure.regions.resolver.RegionRuleResolver
import com.github.berserkr2k.coreplugin.infrastructure.regions.state.PlayerRegionState
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent

class RegionTrackingListener(
    private val resolver: RegionRuleResolver,
    private val playerStateService: PlayerStateService,
    private val eventBus: CoreEventBus
) : Listener {

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val from = event.from
        val to = event.to
        if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ && from.world == to.world) {
            return
        }

        val player = event.player
        val state = playerStateService.getContainer(player.uniqueId, PlayerRegionState.STATE_TYPE)
        val oldRegions = state.currentRegions

        val worldIndex = WorldIndexRegistry.getIndex(to.world.uid)
        val newRegions = resolver.resolveActiveRegions(worldIndex, to.blockX, to.blockY, to.blockZ)

        // Find leaving regions (present in old but not in new)
        for (i in 0 until oldRegions.size) {
            val oldReg = oldRegions[i]
            var found = false
            for (j in 0 until newRegions.size) {
                if (newRegions[j].id == oldReg.id) {
                    found = true
                    break
                }
            }
            if (!found) {
                eventBus.postSync(PlayerRegionLeaveEvent(player, oldReg))
            }
        }

        // Find entering regions (present in new but not in old)
        for (i in 0 until newRegions.size) {
            val newReg = newRegions[i]
            var found = false
            for (j in 0 until oldRegions.size) {
                if (oldRegions[j].id == newReg.id) {
                    found = true
                    break
                }
            }
            if (!found) {
                eventBus.postSync(PlayerRegionEnterEvent(player, newReg))
            }
        }

        state.currentRegions = newRegions
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // PlayerStateService automatically handles cleaning player state containers on quit
    }
}
