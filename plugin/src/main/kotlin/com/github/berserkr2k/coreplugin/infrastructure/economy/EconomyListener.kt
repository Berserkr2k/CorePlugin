package com.github.berserkr2k.coreplugin.infrastructure.economy

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin

class EconomyListener(
    private val plugin: Plugin,
    private val economyService: EconomyService
) : Listener {

    @EventHandler
    fun onPlayerPreLogin(event: AsyncPlayerPreLoginEvent) {
        val uuid = event.uniqueId
        
        // Verificar si la divisa requiere sincronización cross-server
        val hasCrossServer = economyService.currencies.values.any { it.crossServer }
        if (!hasCrossServer) return

        val startTime = System.currentTimeMillis()
        val maxWaitMs = 3000L // Espera máxima de 3 segundos
        var lockedAt = economyService.getCrossServerLockTime(uuid)

        // Bucle de espera asíncrono para dar tiempo a que el servidor de origen termine de guardar
        while (lockedAt > 0 && (System.currentTimeMillis() - lockedAt) < 5000L && (System.currentTimeMillis() - startTime) < maxWaitMs) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                break
            }
            lockedAt = economyService.getCrossServerLockTime(uuid)
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        // Cargar balances de forma asíncrona en el caché local
        economyService.loadPlayerCache(player.uniqueId)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val uuid = player.uniqueId
        
        // Bloquear transacciones en memoria para evitar tareas asíncronas fantasma durante la salida
        TransactionLockManager.acquire(uuid)

        // Si hay divisas de tipo Cross-Server, activamos el bloqueo en base de datos de forma inmediata y síncrona
        val hasCrossServer = economyService.currencies.values.any { it.crossServer }
        if (hasCrossServer) {
            economyService.acquireCrossServerLock(uuid).join()
        }

        try {
            // Guardar saldos de forma síncrona/bloqueante en la salida para evitar doble gasto y descargar del caché
            economyService.saveAndUnloadPlayer(uuid)
        } finally {
            if (hasCrossServer) {
                economyService.releaseCrossServerLock(uuid).join()
            }
            TransactionLockManager.release(uuid)
        }
    }
}
