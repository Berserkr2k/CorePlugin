package com.github.berserkr2k.coreplugin.api.warps

import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

data class CompiledWarp(
    val name: String,
    val location: Location,
    val permission: String,
    val warmupSeconds: Int,
    val cooldownSeconds: Int,
    val guiSlot: Int,
    val displayName: String
)

interface WarpService {
    fun getWarp(name: String): CompiledWarp?
    fun getAllWarps(): List<CompiledWarp>
    fun teleport(player: Player, warp: CompiledWarp): CompletableFuture<Boolean>
    fun setWarp(name: String, world: String, x: Double, y: Double, z: Double, yaw: Float, pitch: Float)
    fun deleteWarp(name: String): Boolean
}
