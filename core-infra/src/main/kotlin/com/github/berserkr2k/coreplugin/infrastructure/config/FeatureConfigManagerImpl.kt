package com.github.berserkr2k.coreplugin.infrastructure.config

import com.github.berserkr2k.coreplugin.api.config.FeatureConfigManager
import java.util.concurrent.atomic.AtomicReference

class FeatureConfigManagerImpl<T : Any>(
    private val configManager: ModularConfigManager,
    private val relPath: String,
    private val configClass: Class<T>,
    private val defaultInstance: T
) : FeatureConfigManager<T> {

    private val instanceRef = AtomicReference<T>(defaultInstance)

    init {
        reload()
    }

    override fun get(): T = instanceRef.get()

    override fun reload() {
        try {
            val loaded = configManager.loadModuleConfig(relPath, configClass, defaultInstance).join()
            instanceRef.set(loaded)
        } catch (e: Exception) {
            throw RuntimeException("Error al cargar la configuración en $relPath: ${e.message}", e)
        }
    }
}
