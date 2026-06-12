package com.github.berserkr2k.coreplugin.api.kits

import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.CompletableFuture

sealed class ClaimResult {
    data class Success(val message: String) : ClaimResult()
    data class Failure(val reason: String) : ClaimResult()
}

interface KitService {
    fun claimKit(player: Player, kitId: String, isGift: Boolean): CompletableFuture<ClaimResult>
    fun getRemainingCooldown(uuid: UUID, kitId: String): Long
    fun formatTime(seconds: Long): String
    fun loadAllKits()
}
