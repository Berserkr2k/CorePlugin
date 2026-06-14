package com.github.berserkr2k.coreplugin.infrastructure.config

import com.github.berserkr2k.coreplugin.api.core.config.ConfigService
import com.github.berserkr2k.coreplugin.api.core.config.FeatureConfig
import com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import org.bukkit.plugin.Plugin
import java.io.File
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
        val path = basePath.resolve("features").resolve(featureId).resolve("config").resolve("config.conf")
        java.nio.file.Files.createDirectories(path.parent)
        return configs.computeIfAbsent("${featureId.lowercase()}:config.conf") { HoconFeatureConfig(path, executor) }
    }

    override fun getCustomConfig(featureId: String, fileName: String): FeatureConfig {
        val path = basePath.resolve("features").resolve(featureId).resolve("config").resolve(fileName)
        java.nio.file.Files.createDirectories(path.parent)
        return configs.computeIfAbsent("${featureId.lowercase()}:$fileName") { HoconFeatureConfig(path, executor) }
    }

    override fun <T : Any> loadConfig(file: File, configClass: Class<T>, defaultInstance: T): T {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }
        val loader = HoconLoaderFactory.create(file)
        val root = loader.load()
        val mapper = HoconLoaderFactory.mapperFactory.get(configClass)
        val instance = if (root.empty()) {
            mapper.save(defaultInstance, root)
            loader.save(root)
            defaultInstance
        } else {
            mapper.load(root) ?: defaultInstance
        }

        // 1. Check schemaVersion compatibility if property exists
        val schemaVersionField = try {
            configClass.getDeclaredField("schemaVersion").apply { isAccessible = true }
        } catch (e: Exception) {
            null
        }
        if (schemaVersionField != null) {
            val expectedVersion = schemaVersionField.get(defaultInstance) as? Int ?: 1
            val loadedVersion = schemaVersionField.get(instance) as? Int ?: 0
            if (loadedVersion != expectedVersion) {
                throw IllegalStateException("Configuration file '${file.name}' schema version '$loadedVersion' does not match expected version '$expectedVersion'!")
            }
        }

        // 2. Perform ValidationEngine validation
        val serviceRegistry = org.bukkit.Bukkit.getServicesManager().load(ServiceRegistry::class.java)
        val validationRegistry = serviceRegistry?.getOptional(com.github.berserkr2k.coreplugin.api.core.validation.ValidationRegistry::class.java) 
            as? com.github.berserkr2k.coreplugin.infrastructure.validation.ValidationEngine
        if (validationRegistry != null) {
            val errors = validationRegistry.validate(instance)
            if (errors.isNotEmpty()) {
                val warnings = errors.filter { it.startsWith("[WARNING]") || it.startsWith("[WARN]") }
                val fatals = errors.filterNot { it.startsWith("[WARNING]") || it.startsWith("[WARN]") }
                
                warnings.forEach { warn ->
                    plugin.logger.warning("Configuration warning in ${file.name}: $warn")
                    validationRegistry.recordWarning(file.name, warn)
                }
                
                if (fatals.isNotEmpty()) {
                    throw IllegalStateException("Configuration validation failed for ${file.name}:\n" + fatals.joinToString("\n"))
                }
            }
        }

        return instance
    }

    override fun <T : Any> saveConfig(file: File, configClass: Class<T>, instance: T) {
        val parent = file.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        if (!file.exists()) {
            file.createNewFile()
        }
        val loader = HoconLoaderFactory.create(file)
        val root = loader.createNode()
        val mapper = HoconLoaderFactory.mapperFactory.get(configClass)
        mapper.save(instance, root)
        loader.save(root)
    }

    fun getLoadedConfigs(): Set<String> {
        return configs.keys
    }
}
