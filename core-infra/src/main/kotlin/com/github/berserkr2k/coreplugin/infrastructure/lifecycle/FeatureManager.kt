package com.github.berserkr2k.coreplugin.infrastructure.lifecycle

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext

class FeatureManager(private val context: FeatureContext) {
    private val features = mutableMapOf<String, Feature>()
    private val enabledFeatures = mutableListOf<Feature>()

    fun register(feature: Feature) {
        features[feature.id] = feature
    }

    fun enableAll() {
        for (feature in features.values) {
            // Validación Enterprise de dependencias críticas
            if (feature.requiresDatabase && context.databaseService == null) {
                context.platform.logger.warning("⚠️ Módulo [${feature.id}] DESACTIVADO: Requiere una Base de Datos activa.")
                continue
            }
            if (feature.requiresEconomy && !isEconomyPresent()) {
                context.platform.logger.warning("⚠️ Módulo [${feature.id}] DESACTIVADO: Requiere el módulo de Economía habilitado.")
                continue
            }

            runCatching {
                feature.onLoad(context)
                feature.onEnable(context)
                enabledFeatures.add(feature)
                context.platform.logger.info("⚡ Módulo [${feature.id}] cargado exitosamente.")
            }.onFailure { t ->
                context.platform.logger.severe("❌ Error crítico al inicializar el módulo [${feature.id}]: ${t.message}")
                t.printStackTrace()
            }
        }
    }

    fun disableAll() {
        for (feature in enabledFeatures.reversed()) {
            runCatching { feature.onDisable(context) }
        }
    }

    private fun isEconomyPresent(): Boolean {
        return context.registry.getOptional(com.github.berserkr2k.coreplugin.api.framework.economy.EconomyService::class.java) != null
    }
}

