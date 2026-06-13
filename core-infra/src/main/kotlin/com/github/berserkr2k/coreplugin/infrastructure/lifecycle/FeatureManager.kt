package com.github.berserkr2k.coreplugin.infrastructure.lifecycle

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit

class FeatureManager(private val context: FeatureContext) {
    private val features = mutableMapOf<String, Feature>()
    private val enabledFeatures = mutableListOf<Feature>()

    fun register(feature: Feature) {
        features[feature.id.lowercase()] = feature
    }

    fun getEnabledFeaturesInOrder(): List<Feature> {
        return enabledFeatures.toList()
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

        val report = features.values.associate { it.id to BootReportEntry(it.id, "PENDING") }

        val enabledInConfig = mutableListOf<Feature>()
        for (feature in features.values) {
            val entry = report[feature.id]!!
            val isEnabled = rootNode.node(feature.id, "enabled").getBoolean(true)
            if (isEnabled) {
                enabledInConfig.add(feature)
            } else {
                entry.status = "DISABLED"
            }
        }

        val sortedFeatures = try {
            resolveStartupOrder(enabledInConfig)
        } catch (e: Exception) {
            val errMsg = e.message ?: "Dependency resolution failed"
            context.platform.logger.severe("❌ Error al resolver dependencias de módulos: $errMsg")
            enabledInConfig.forEach { feature ->
                val entry = report[feature.id]!!
                if (entry.status == "PENDING") {
                    entry.status = "FAILED"
                    entry.error = errMsg
                }
            }
            emptyList()
        }

        for (feature in sortedFeatures) {
            val entry = report[feature.id]!!

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
                    val depEntry = report[depFeature.id]!!
                    if (depEntry.status != "ACTIVE") {
                        canLoad = false
                        missingDepReason = "Dependency '$depId' is ${depEntry.status}"
                        break
                    }
                }
            }

            if (!canLoad) {
                entry.status = "UNSATISFIED"
                entry.error = missingDepReason
                context.platform.logger.warning("⚠️ Módulo [${feature.id}] DESACTIVADO: $missingDepReason.")
                continue
            }

            val start = System.currentTimeMillis()
            runCatching {
                feature.onLoad(context)
                feature.onEnable(context)
            }.onSuccess {
                val duration = System.currentTimeMillis() - start
                enabledFeatures.add(feature)
                entry.status = "ACTIVE"
                entry.timeMs = "${duration}ms"
                context.platform.logger.info("⚡ Módulo [${feature.id}] cargado exitosamente.")
            }.onFailure { t ->
                entry.status = "FAILED"
                entry.error = t.message ?: t.javaClass.simpleName
                context.platform.logger.severe("❌ Error crítico al inicializar el módulo [${feature.id}]: ${t.message}")
                t.printStackTrace()
            }
        }

        // Print Boot Report
        printBootReport(report.values)
    }

    fun disableAll() {
        for (feature in enabledFeatures.reversed()) {
            runCatching { feature.onDisable(context) }
        }
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

    private fun printBootReport(entries: Collection<BootReportEntry>) {
        val mm = MiniMessage.miniMessage()
        val builder = java.lang.StringBuilder()
        builder.append("\n<dark_gray>======================================================</dark_gray>\n")
        builder.append("<gold><bold>            COREPLUGIN FEATURE BOOT REPORT</bold></gold>\n")
        builder.append("<dark_gray>======================================================</dark_gray>\n")
        builder.append(String.format("  %-20s %-12s %-8s %-20s\n", "Feature", "Status", "Time", "Details"))
        builder.append("<dark_gray>------------------------------------------------------</dark_gray>\n")
        
        for (entry in entries) {
            val statusColor = when (entry.status) {
                "ACTIVE" -> "<green>"
                "DISABLED" -> "<gray>"
                "FAILED" -> "<red>"
                "UNSATISFIED" -> "<yellow>"
                else -> "<white>"
            }
            val line = String.format(
                "  <white>%-20s</white> %s%-12s</color> <gold>%-8s</gold> <red>%s</red>\n",
                entry.featureId,
                statusColor,
                entry.status,
                entry.timeMs,
                if (entry.error == "-") "" else entry.error
            )
            builder.append(line)
        }
        builder.append("<dark_gray>======================================================</dark_gray>")
        
        Bukkit.getConsoleSender().sendMessage(mm.deserialize(builder.toString()))
    }

    private class BootReportEntry(
        val featureId: String,
        var status: String,
        var timeMs: String = "-",
        var error: String = "-"
    )
}
