package com.github.berserkr2k.coreplugin.api.core.config

interface ConfigService {
    /**
     * Obtiene o crea la configuración aislada para una característica.
     * Debe mapearse automáticamente a: plugins/CorePlugin/features/<featureId>/config.conf
     */
    fun getConfig(featureId: String): FeatureConfig

    /**
     * Permite cargar archivos específicos adicionales (ej. messages.conf, data.conf)
     */
    fun getCustomConfig(featureId: String, fileName: String): FeatureConfig
}
