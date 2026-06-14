package com.github.berserkr2k.coreplugin.infra.di

import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.infrastructure.lifecycle.DependencyVerifier

class ScopedServiceRegistry(
    private val featureId: String,
    private val delegate: ServiceRegistry
) : ServiceRegistry {

    override fun <T : Any> register(type: Class<T>, instance: T) {
        DependencyVerifier.registerProvider(type, featureId)
        delegate.register(type, instance)
    }

    override fun <T : Any> registerProvider(type: Class<T>, provider: () -> T) {
        DependencyVerifier.registerProvider(type, featureId)
        delegate.registerProvider(type, provider)
    }

    override fun <T : Any> get(type: Class<T>): T {
        DependencyVerifier.verify(featureId, type)
        return delegate.get(type)
    }

    override fun <T : Any> getOptional(type: Class<T>): T? {
        DependencyVerifier.verify(featureId, type)
        return delegate.getOptional(type)
    }
}
