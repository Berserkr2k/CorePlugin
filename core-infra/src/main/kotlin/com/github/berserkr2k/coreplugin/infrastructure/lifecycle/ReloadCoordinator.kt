package com.github.berserkr2k.coreplugin.infrastructure.lifecycle

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Reloadable
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.system.measureTimeMillis

class ReloadCoordinator(private val logger: Logger) {
    private val reloadables = ConcurrentHashMap<String, Reloadable>()

    /**
     * Registra una feature recargable en el coordinador.
     */
    fun register(featureId: String, reloadable: Reloadable) {
        reloadables[featureId.lowercase()] = reloadable
    }

    /**
     * Recarga una feature específica de forma asíncrona.
     */
    suspend fun reloadFeature(featureId: String): Long {
        val fid = featureId.lowercase()
        val reloadable = reloadables[fid] ?: throw IllegalArgumentException("La feature '$featureId' no está registrada o no es recargable.")
        
        var duration: Long = 0
        try {
            duration = measureTimeMillis {
                reloadable.reload()
            }
        } catch (e: Exception) {
            logger.severe("❌ Error crítico durante la recarga de '$featureId': ${e.message}")
            throw e
        }
        return duration
    }

    /**
     * Recarga todas las features registradas respetando el orden de dependencias.
     */
    suspend fun reloadAll(): Map<String, Long> {
        val metrics = mutableMapOf<String, Long>()
        
        // Orden recomendado para evitar desalineación de dependencias
        val order = listOf(
            "core",
            "economy",
            "regions",
            "spawn",
            "kits",
            "holograms",
            "shops",
            "menus",
            "utility"
        )
        
        // Recargar en el orden especificado
        for (feature in order) {
            if (reloadables.containsKey(feature)) {
                try {
                    val duration = reloadFeature(feature)
                    metrics[feature] = duration
                } catch (e: Exception) {
                    logger.severe("❌ No se pudo completar la recarga del módulo '$feature'.")
                }
            }
        }

        // Recargar cualquier módulo residual no ordenado
        for (feature in reloadables.keys) {
            if (!order.contains(feature) && !metrics.containsKey(feature)) {
                try {
                    val duration = reloadFeature(feature)
                    metrics[feature] = duration
                } catch (e: Exception) {
                    logger.severe("❌ No se pudo completar la recarga del módulo '$feature'.")
                }
            }
        }

        return metrics
    }
}
