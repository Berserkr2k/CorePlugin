package com.github.berserkr2k.coreplugin.api.config

interface FeatureConfigManager<T : Any> {
    /**
     * Obtiene la instancia de configuración actual.
     */
    fun get(): T

    /**
     * Carga o recarga la configuración desde el disco.
     */
    fun reload()
}
