package com.github.berserkr2k.coreplugin.infrastructure.economy

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object TransactionLockManager {
    private val activeLocks = ConcurrentHashMap.newKeySet<UUID>()

    /**
     * Intenta adquirir un bloqueo para el jugador de forma atómica.
     * Retorna true si se adquirió con éxito, false si ya estaba bloqueado.
     */
    fun acquire(uuid: UUID): Boolean {
        return activeLocks.add(uuid)
    }

    /**
     * Libera el bloqueo para el jugador.
     */
    fun release(uuid: UUID) {
        activeLocks.remove(uuid)
    }

    /**
     * Verifica si el jugador está bloqueado.
     */
    fun isLocked(uuid: UUID): Boolean {
        return activeLocks.contains(uuid)
    }
}
