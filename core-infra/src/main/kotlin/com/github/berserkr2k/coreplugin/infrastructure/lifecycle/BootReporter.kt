package com.github.berserkr2k.coreplugin.infrastructure.lifecycle

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureState
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureStatus
import com.github.berserkr2k.coreplugin.api.di.InternalService
import com.github.berserkr2k.coreplugin.infrastructure.validation.ValidationEngine
import com.google.gson.GsonBuilder
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter

@InternalService
object BootReporter {

    fun generateAndReport(
        dataFolder: File,
        features: Collection<Feature>,
        statuses: Map<String, FeatureStatus>,
        validationEngine: ValidationEngine?,
        fatalErrors: List<String> = emptyList()
    ) {
        val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        
        // 1. Build JSON report model
        val featureReports = features.map { feature ->
            val status = statuses[feature.id.lowercase()]
            val stateStr = status?.state?.name ?: FeatureState.DISCOVERED.name
            val timeMs = status?.startupTimeMs ?: 0L
            val errorStr = status?.error?.let { "${it.javaClass.name}: ${it.message}" }
            
            FeatureReportJson(
                id = feature.id,
                state = stateStr,
                startupTimeMs = timeMs,
                dependencies = feature.descriptor.dependencies.toList(),
                optionalDependencies = feature.descriptor.optionalDependencies.toList(),
                provides = feature.descriptor.provides.map { it.name },
                error = errorStr
            )
        }
        
        val warnings = validationEngine?.getWarnings()?.map {
            ValidationWarningJson(it.fileName, it.message)
        } ?: emptyList()
        
        val success = fatalErrors.isEmpty() && featureReports.none { it.state == "FAILED" }
        
        val reportJson = BootReportJson(
            timestamp = timestamp,
            success = success,
            features = featureReports,
            providesTopology = features.associate { feature ->
                feature.id to feature.descriptor.provides.map { it.name }
            },
            validation = ValidationTelemetryJson(
                warnings = warnings,
                fatals = fatalErrors
            )
        )
        
        // 2. Serialize and save to plugins/CorePlugin/reports/boot-report.json
        try {
            val reportsDir = dataFolder.resolve("reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }
            val reportFile = reportsDir.resolve("boot-report.json")
            val gson = GsonBuilder().setPrettyPrinting().create()
            reportFile.writeText(gson.toJson(reportJson))
        } catch (e: Exception) {
            Bukkit.getLogger().severe("Failed to write boot-report.json: ${e.message}")
        }
        
        // 3. Print console report
        printConsoleReport(featureReports, warnings, fatalErrors)
    }

    private fun printConsoleReport(
        featureReports: List<FeatureReportJson>,
        warnings: List<ValidationWarningJson>,
        fatalErrors: List<String>
    ) {
        val mm = MiniMessage.miniMessage()
        val builder = StringBuilder()
        builder.append("\n<dark_gray>======================================================</dark_gray>\n")
        builder.append("<gold><bold>            COREPLUGIN FEATURE BOOT REPORT</bold></gold>\n")
        builder.append("<dark_gray>======================================================</dark_gray>\n")
        builder.append(String.format("  %-18s %-10s %-8s %-20s\n", "Feature", "Status", "Time", "Exposed Services"))
        builder.append("<dark_gray>------------------------------------------------------</dark_gray>\n")
        
        for (report in featureReports) {
            val statusColor = when (report.state) {
                "ENABLED" -> "<green>"
                "DISABLED" -> "<gray>"
                "FAILED" -> "<red>"
                else -> "<yellow>"
            }
            
            val servicesStr = if (report.provides.isEmpty()) {
                "-"
            } else {
                report.provides.joinToString(", ") { it.substringAfterLast('.') }
            }
            
            val line = String.format(
                "  <white>%-18s</white> %s%-10s</color> <gold>%-8s</gold> <gray>%s</gray>\n",
                report.id,
                statusColor,
                report.state,
                "${report.startupTimeMs}ms",
                servicesStr
            )
            builder.append(line)
        }
        
        if (warnings.isNotEmpty() || fatalErrors.isNotEmpty()) {
            builder.append("<dark_gray>------------------------------------------------------</dark_gray>\n")
            builder.append("<gold><bold>             VALIDATION TELEMETRY</bold></gold>\n")
            builder.append("<dark_gray>------------------------------------------------------</dark_gray>\n")
            
            for (fatal in fatalErrors) {
                builder.append("  <red>[FATAL] $fatal</red>\n")
            }
            
            for (warn in warnings) {
                builder.append("  <yellow>[WARN] (${warn.file}): ${warn.message}</yellow>\n")
            }
        }
        
        builder.append("<dark_gray>======================================================</dark_gray>")
        
        Bukkit.getConsoleSender().sendMessage(mm.deserialize(builder.toString()))
    }

    private data class BootReportJson(
        val timestamp: String,
        val success: Boolean,
        val features: List<FeatureReportJson>,
        val providesTopology: Map<String, List<String>>,
        val validation: ValidationTelemetryJson
    )

    private data class FeatureReportJson(
        val id: String,
        val state: String,
        val startupTimeMs: Long,
        val dependencies: List<String>,
        val optionalDependencies: List<String>,
        val provides: List<String>,
        val error: String?
    )

    private data class ValidationWarningJson(
        val file: String,
        val message: String
    )

    private data class ValidationTelemetryJson(
        val warnings: List<ValidationWarningJson>,
        val fatals: List<String>
    )
}
