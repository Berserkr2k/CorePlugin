package com.github.berserkr2k.coreplugin.infra.di

import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import java.util.concurrent.ConcurrentHashMap

class SimpleServiceRegistry : ServiceRegistry {
    private val instances = ConcurrentHashMap<Class<*>, Any>()
    private val providers = ConcurrentHashMap<Class<*>, () -> Any>()

    override fun <T : Any> register(type: Class<T>, instance: T) {
        instances[type] = instance
    }

    override fun <T : Any> registerProvider(type: Class<T>, provider: () -> T) {
        providers[type] = provider
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(type: Class<T>): T {
        return getOptional(type) ?: throw IllegalArgumentException("No service registered of type: ${type.name}")
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getOptional(type: Class<T>): T? {
        val direct = instances[type] as? T
        if (direct != null) return direct

        val provider = providers[type]
        if (provider != null) {
            val resolved = provider() as T
            instances[type] = resolved
            return resolved
        }
        return null
    }
}
