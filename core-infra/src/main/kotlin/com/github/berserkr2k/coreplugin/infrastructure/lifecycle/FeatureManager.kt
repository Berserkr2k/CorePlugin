package com.github.berserkr2k.coreplugin.infrastructure.lifecycle

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.di.InternalService
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit

@InternalService
class FeatureManager(private val context: FeatureContext) {
    private val features = mutableMapOf<String, Feature>()
    private val enabledFeatures = mutableListOf<Feature>()
    private val statuses = mutableMapOf<String, com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureStatus>()

    fun register(feature: Feature) {
        features[feature.id.lowercase()] = feature
        statuses[feature.id.lowercase()] = com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureStatus(
            feature.id,
            com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureState.DISCOVERED
        )
    }

    fun getEnabledFeaturesInOrder(): List<Feature> {
        return enabledFeatures.toList()
    }

    fun getFeatureStatus(featureId: String): com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureStatus? {
        return statuses[featureId.lowercase()]
    }

    fun getFeatureStatuses(): Map<String, com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureStatus> {
        return statuses.toMap()
    }

    fun enableAll() {
        val featuresConfFile = context.platform.dataFolder.resolve("features.conf")
        if (!featuresConfFile.exists()) {
            featuresConfFile.parentFile.mkdirs()
            featuresConfFile.writeText("""
                # CorePlugin Feature Toggles
                # Enable or disable features below.

                spawn { enabled = true }
                warps { enabled = true }
                kits { enabled = true }
                chat { enabled = true }
                economy { enabled = true }
                projectile-trails { enabled = true }
                holograms { enabled = true }
                scoreboard { enabled = true }
                utility-commands { enabled = true }
                leaderboard { enabled = true }
                shop { enabled = true }
            """.trimIndent())
        }

        val loader = HoconConfigurationLoader.builder()
            .path(featuresConfFile.toPath())
            .build()
        val rootNode = loader.load()

        // 1. Register dependencies of all features into DependencyVerifier
        for (feature in features.values) {
            DependencyVerifier.registerFeatureDependencies(
                feature.id,
                feature.descriptor.dependencies,
                feature.descriptor.optionalDependencies
            )
            val isEnabled = rootNode.node(feature.id, "enabled").getBoolean(true)
            if (!isEnabled) {
                statuses[feature.id.lowercase()] = com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureStatus(
                    feature.id,
                    com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureState.DISABLED
                )
            }
        }

        val enabledInConfig = features.values.filter { rootNode.node(it.id, "enabled").getBoolean(true) }
        val fatalErrors = mutableListOf<String>()

        try {
            val sortedFeatures = try {
                enabledInConfig.forEach {
                    statuses[it.id.lowercase()] = com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureStatus(
                        it.id,
                        com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureState.RESOLVING_DEPENDENCIES
                    )
                }
                resolveStartupOrder(enabledInConfig)
            } catch (e: Exception) {
                val errMsg = e.message ?: "Dependency resolution failed"
                context.platform.logger.severe("❌ Error al resolver dependencias de módulos: $errMsg")
                enabledInConfig.forEach { feature ->
                    val statusObj = statuses[feature.id.lowercase()]
                    if (statusObj?.state == com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureState.RESOLVING_DEPENDENCIES) {
                        statuses[feature.id.lowercase()] = com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureStatus(
                            feature.id,
                            com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureState.FAILED,
                            error = e
                        )
                    }
                }
                throw e
            }

            // 2. Service Registration Phase
            for (feature in sortedFeatures) {
                statuses[feature.id.lowercase()] = com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureStatus(
                    feature.id,
                    com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureState.REGISTERING_SERVICES
                )
                val scopedRegistry = com.github.berserkr2k.coreplugin.infra.di.ScopedServiceRegistry(feature.id, context.registry)
                try {
                    feature.registerServices(scopedRegistry)
                    
                    // Assert registered services match provides metadata exactly
                    val expectedProvides = feature.descriptor.provides.toSet()
                    val actualProvides = DependencyVerifier.getProvidedServices(feature.id)
                    if (expectedProvides != actualProvides) {
                        throw IllegalStateException(
                            "Feature '${feature.id}' registered services $actualProvides, " +
                            "but its descriptor declared $expectedProvides"
                        )
                    }
                } catch (t: Throwable) {
                    statuses[feature.id.lowercase()] = com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureStatus(
                        feature.id,
                        com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureState.FAILED,
                        error = t
                    )
                    context.platform.logger.severe("❌ Error crítico en registro de servicios del módulo [${feature.id}]: ${t.message}")
                    throw t
                }
            }

            // 3. Feature Enable/Load Phase
            val validationRegistry = context.registry.getOptional(com.github.berserkr2k.coreplugin.api.core.validation.ValidationRegistry::class.java)
            if (validationRegistry != null) {
                for (feature in sortedFeatures) {
                    feature.registerValidators(validationRegistry)
                }
            }

            for (feature in sortedFeatures) {
                // Verify dependencies
                var canLoad = true
                var missingDepReason = ""
                for (depId in feature.dependencies) {
                    if (depId.equals("database", ignoreCase = true)) {
                        if (context.databaseService == null) {
                            canLoad = false
                            missingDepReason = "Database service not active"
                            break
                        }
                    } else {
                        val depFeature = features[depId.lowercase()]
                        if (depFeature == null) {
                            canLoad = false
                            missingDepReason = "Dependency '$depId' not registered"
                            break
                        }
                        val depStatus = statuses[depFeature.id.lowercase()]
                        if (depStatus?.state != com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureState.ENABLED) {
                            canLoad = false
                            missingDepReason = "Dependency '$depId' is not ENABLED"
                            break
                        }
                    }
                }

                if (!canLoad) {
                    statuses[feature.id.lowercase()] = com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureStatus(
                        feature.id,
                        com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureState.FAILED,
                        error = IllegalStateException("Unsatisfied dependency: $missingDepReason")
                    )
                    context.platform.logger.warning("⚠️ Módulo [${feature.id}] DESACTIVADO: $missingDepReason.")
                    throw IllegalStateException("Unsatisfied dependency for feature '${feature.id}': $missingDepReason")
                }

                val start = System.currentTimeMillis()
                try {
                    statuses[feature.id.lowercase()] = com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureStatus(
                        feature.id,
                        com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureState.LOADING_CONFIG
                    )
                    // (Future config loading goes here)

                    statuses[feature.id.lowercase()] = com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureStatus(
                        feature.id,
                        com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureState.VALIDATING
                    )
                    // (Future validation goes here)

                    val scopedRegistry = com.github.berserkr2k.coreplugin.infra.di.ScopedServiceRegistry(feature.id, context.registry)
                    val scopedContext = com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext(
                        platform = context.platform,
                        registry = scopedRegistry,
                        taskScheduler = context.taskScheduler,
                        regionTaskScheduler = context.regionTaskScheduler,
                        messageService = context.messageService,
                        configService = context.configService,
                        databaseService = context.databaseService,
                        _plugin = context._plugin
                    )

                    feature.onLoad(scopedContext)
                    feature.onEnable(scopedContext)

                    val duration = System.currentTimeMillis() - start
                    enabledFeatures.add(feature)
                    statuses[feature.id.lowercase()] = com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureStatus(
                        id = feature.id,
                        state = com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureState.ENABLED,
                        startupTimeMs = duration
                    )
                    context.platform.logger.info("⚡ Módulo [${feature.id}] cargado exitosamente.")
                } catch (t: Throwable) {
                    val duration = System.currentTimeMillis() - start
                    statuses[feature.id.lowercase()] = com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureStatus(
                        id = feature.id,
                        state = com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureState.FAILED,
                        startupTimeMs = duration,
                        error = t
                    )
                    context.platform.logger.severe("❌ Error crítico al inicializar el módulo [${feature.id}]: ${t.message}")
                    t.printStackTrace()
                    throw t
                }
            }

            // Generate report on success
            val validationEngine = context.registry.getOptional(com.github.berserkr2k.coreplugin.api.core.validation.ValidationRegistry::class.java)
                as? com.github.berserkr2k.coreplugin.infrastructure.validation.ValidationEngine
            BootReporter.generateAndReport(
                dataFolder = context.platform.dataFolder,
                features = features.values,
                statuses = statuses,
                validationEngine = validationEngine,
                fatalErrors = emptyList()
            )

        } catch (t: Throwable) {
            fatalErrors.add(t.message ?: t.javaClass.simpleName)
            val validationEngine = context.registry.getOptional(com.github.berserkr2k.coreplugin.api.core.validation.ValidationRegistry::class.java)
                as? com.github.berserkr2k.coreplugin.infrastructure.validation.ValidationEngine
            BootReporter.generateAndReport(
                dataFolder = context.platform.dataFolder,
                features = features.values,
                statuses = statuses,
                validationEngine = validationEngine,
                fatalErrors = fatalErrors
            )
            throw t
        }
    }

    fun disableAll() {
        for (feature in enabledFeatures.reversed()) {
            runCatching {
                val scopedRegistry = com.github.berserkr2k.coreplugin.infra.di.ScopedServiceRegistry(feature.id, context.registry)
                val scopedContext = com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext(
                    platform = context.platform,
                    registry = scopedRegistry,
                    taskScheduler = context.taskScheduler,
                    regionTaskScheduler = context.regionTaskScheduler,
                    messageService = context.messageService,
                    configService = context.configService,
                    databaseService = context.databaseService,
                    _plugin = context._plugin
                )
                feature.onDisable(scopedContext)
                statuses[feature.id.lowercase()] = com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureStatus(
                    feature.id,
                    com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureState.DISABLED
                )
            }
        }
        enabledFeatures.clear()
    }

    private fun resolveStartupOrder(featuresList: Collection<Feature>): List<Feature> {
        val result = mutableListOf<Feature>()
        val visited = mutableMapOf<String, Boolean>() // true = visiting, false = visited
        val featuresMap = featuresList.associateBy { it.id.lowercase() }

        fun visit(feature: Feature) {
            val featureIdLower = feature.id.lowercase()
            val state = visited[featureIdLower]
            if (state == true) {
                throw IllegalStateException("Circular dependency detected involving feature: ${feature.id}")
            }
            if (state == null) {
                visited[featureIdLower] = true
                for (depId in feature.dependencies) {
                    if (depId.equals("database", ignoreCase = true)) continue
                    val depFeature = featuresMap[depId.lowercase()] 
                        ?: throw IllegalStateException("Missing dependency: '$depId' required by '${feature.id}'")
                    visit(depFeature)
                }
                visited[featureIdLower] = false
                result.add(feature)
            }
        }

        for (feature in featuresList) {
            visit(feature)
        }

        return result
    }
}
