package com.github.berserkr2k.coreplugin.infrastructure.lifecycle

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Reloadable
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.system.measureTimeMillis

class ReloadCoordinator(
    private val registry: ServiceRegistry,
    private val logger: Logger
) : com.github.berserkr2k.coreplugin.api.core.lifecycle.ReloadCoordinator {
    private val reloadables = ConcurrentHashMap<String, Reloadable>()

    var featureManager: FeatureManager? = null

    /**
     * Registra una feature recargable en el coordinador.
     */
    override fun register(featureId: String, reloadable: Reloadable) {
        reloadables[featureId.lowercase()] = reloadable
    }

    /**
     * Recarga una feature específica de forma asíncrona.
     */
    override suspend fun reloadFeature(featureId: String): Long {
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
    override suspend fun reloadAll(): Map<String, Long> {
        val metrics = mutableMapOf<String, Long>()
        
        // 1. Obtener el orden de dependencias de la base de datos de características activas
        val sortedFeatures = featureManager?.getEnabledFeaturesInOrder() ?: emptyList()
        val sortedFeatureIds = sortedFeatures.map { it.id.lowercase() }
        
        // 2. Identificar recargables a nivel de sistema/framework que no son features tradicionales (ej. core, regions)
        val systemReloadables = reloadables.keys.filter { it !in sortedFeatureIds }
        
        // 3. Recargar primero los módulos de sistema en el orden en que se registraron
        for (featureId in systemReloadables) {
            try {
                val duration = reloadFeature(featureId)
                metrics[featureId] = duration
            } catch (e: Exception) {
                logger.severe("❌ No se pudo completar la recarga del módulo de sistema '$featureId'.")
            }
        }

        // 4. Recargar los módulos de gameplay/features en el orden topológico derivado del grafo
        for (featureId in sortedFeatureIds) {
            if (reloadables.containsKey(featureId)) {
                try {
                    val duration = reloadFeature(featureId)
                    metrics[featureId] = duration
                } catch (e: Exception) {
                    logger.severe("❌ No se pudo completar la recarga del módulo '$featureId'.")
                }
            }
        }

        return metrics
    }
}
