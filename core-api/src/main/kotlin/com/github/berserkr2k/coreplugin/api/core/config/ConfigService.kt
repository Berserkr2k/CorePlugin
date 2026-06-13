package com.github.berserkr2k.coreplugin.api.core.config

import java.io.File

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

    /**
     * Carga y mapea un archivo HOCON a una clase específica de manera segura.
     */
    fun <T : Any> loadConfig(file: File, configClass: Class<T>, defaultInstance: T): T

    /**
     * Guarda una instancia de configuración a un archivo HOCON de manera segura.
     */
    fun <T : Any> saveConfig(file: File, configClass: Class<T>, instance: T)
}
