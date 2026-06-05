package com.github.berserkr2k.coreplugin.api.leaderboard

import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

interface LeaderboardService {
    fun spawnOrFindLeaderboard(location: Location, leaderboardId: String, rank: Int)
    fun registerLeaderboard(id: String, rank: Int, loc: Location): CompletableFuture<Void>
    fun unregisterLeaderboard(id: String, rank: Int): CompletableFuture<Boolean>
    fun updatePlayerStats(player: Player)
    fun refreshAllLeaderboards()
    fun reloadLeaderboards(): CompletableFuture<Void>
}
