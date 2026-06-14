package com.github.berserkr2k.coreplugin.api.core.lifecycle

import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.validation.ValidationRegistry
import com.github.berserkr2k.coreplugin.api.core.health.HealthCheckRegistry

interface Feature {
    val descriptor: FeatureDescriptor

    val id: String get() = descriptor.id
    val dependencies: Set<String> get() = descriptor.dependencies

    fun registerServices(registry: ServiceRegistry) {}
    fun registerValidators(registry: ValidationRegistry) {}
    fun registerHealthChecks(registry: HealthCheckRegistry) {}

    fun onLoad(context: FeatureContext) {}
    fun onEnable(context: FeatureContext) {}
    fun onDisable(context: FeatureContext) {}
}
