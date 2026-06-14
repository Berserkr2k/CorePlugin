package com.github.berserkr2k.coreplugin.infrastructure.lifecycle

import java.util.concurrent.ConcurrentHashMap

object DependencyVerifier {
    private val serviceProviders = ConcurrentHashMap<Class<*>, String>()
    private val featureDependencies = ConcurrentHashMap<String, Set<String>>()

    fun registerProvider(serviceType: Class<*>, featureId: String) {
        serviceProviders[serviceType] = featureId.lowercase()
    }

    fun getProvidedServices(featureId: String): Set<Class<*>> {
        val fid = featureId.lowercase()
        return serviceProviders.filterValues { it == fid }.keys
    }

    fun registerFeatureDependencies(featureId: String, dependencies: Set<String>, optionalDependencies: Set<String>) {
        featureDependencies[featureId.lowercase()] = (dependencies + optionalDependencies).map { it.lowercase() }.toSet()
    }

    fun verify(callerFeatureId: String, serviceType: Class<*>) {
        val providerFeatureId = serviceProviders[serviceType] ?: return // Not a feature-provided service
        val caller = callerFeatureId.lowercase()
        
        if (caller == providerFeatureId) return
        
        val allowedDeps = featureDependencies[caller] ?: emptySet()
        if (providerFeatureId !in allowedDeps) {
            throw IllegalStateException(
                "Dependency violation: Feature '$callerFeatureId' attempted to resolve service '${serviceType.name}' " +
                "which is provided by feature '$providerFeatureId', but '$providerFeatureId' is not declared in " +
                "dependencies or optionalDependencies for '$callerFeatureId'!"
            )
        }
    }
}
