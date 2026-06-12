package com.github.berserkr2k.coreplugin.infrastructure.config

import com.github.berserkr2k.coreplugin.api.core.config.ConfigService
import com.github.berserkr2k.coreplugin.api.core.config.FeatureConfig
import com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import org.bukkit.plugin.Plugin
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

class ConfigServiceImpl(
    private val basePath: Path,
    private val plugin: Plugin = org.bukkit.Bukkit.getServicesManager().load(ServiceRegistry::class.java)?.get(Plugin::class.java)
        ?: throw IllegalStateException("Plugin instance not found in ServiceRegistry"),
    private val taskScheduler: TaskScheduler = org.bukkit.Bukkit.getServicesManager().load(ServiceRegistry::class.java)?.get(TaskScheduler::class.java)
        ?: throw IllegalStateException("TaskScheduler instance not found in ServiceRegistry")
) : ConfigService {
    private val configs = ConcurrentHashMap<String, FeatureConfig>()
    private val executor = Executor { command -> taskScheduler.runAsync(command) }

    override fun getConfig(featureId: String): FeatureConfig {
        return getCustomConfig(featureId, "config.conf")
    }

    override fun getCustomConfig(featureId: String, fileName: String): FeatureConfig {
        val cacheKey = "${featureId.lowercase()}:$fileName"
        return configs.computeIfAbsent(cacheKey) {
            val path = basePath.resolve("features").resolve(featureId.lowercase()).resolve(fileName)
            
            if (Files.notExists(path)) {
                val parentDir = path.parent
                if (parentDir != null && Files.notExists(parentDir)) {
                    Files.createDirectories(parentDir)
                }
                
                // Intentar buscar e inyectar el recurso desde el .jar
                val resourcePath = "features/${featureId.lowercase()}/$fileName"
                var resourceStream: InputStream? = plugin.getResource(resourcePath)
                if (resourceStream == null) {
                    resourceStream = plugin.getResource("${featureId.lowercase()}/$fileName")
                }
                if (resourceStream == null) {
                    resourceStream = plugin.getResource(fileName)
                }
                if (resourceStream == null) {
                    // Cargar vía ClassLoader del plugin como fallback definitivo
                    resourceStream = plugin::class.java.classLoader.getResourceAsStream(resourcePath)
                        ?: plugin::class.java.classLoader.getResourceAsStream("${featureId.lowercase()}/$fileName")
                        ?: plugin::class.java.classLoader.getResourceAsStream(fileName)
                }
                
                if (resourceStream != null) {
                    resourceStream.use { input ->
                        Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING)
                    }
                } else {
                    if (Files.notExists(path)) {
                        Files.createFile(path)
                    }
                }
            }
            
            HoconFeatureConfig(path, executor)
        }
    }
}
