package com.github.berserkr2k.coreplugin.infra.di

import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import java.util.concurrent.ConcurrentHashMap

class SimpleServiceRegistry : ServiceRegistry {
    private val instances = ConcurrentHashMap<Class<*>, Any>()
    private val providers = ConcurrentHashMap<Class<*>, () -> Any>()

    override fun <T : Any> register(type: Class<T>, instance: T) {
        if (type.isAnnotationPresent(com.github.berserkr2k.coreplugin.api.di.InternalService::class.java) ||
            instance.javaClass.isAnnotationPresent(com.github.berserkr2k.coreplugin.api.di.InternalService::class.java)) {
            throw IllegalArgumentException("Cannot register internal service ${type.name} (implementation ${instance.javaClass.name}) in the public ServiceRegistry.")
        }
        instances[type] = instance
    }

    override fun <T : Any> registerProvider(type: Class<T>, provider: () -> T) {
        if (type.isAnnotationPresent(com.github.berserkr2k.coreplugin.api.di.InternalService::class.java)) {
            throw IllegalArgumentException("Cannot register internal service provider for ${type.name} in the public ServiceRegistry.")
        }
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
