package com.github.berserkr2k.coreplugin.infrastructure.config

import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.ConfigurateException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

class ModularConfigManager(private val plugin: Plugin, private val configDirectory: Path) {
    private val loadedConfigs = ConcurrentHashMap<String, Any>()
    private val loaders = ConcurrentHashMap<String, ConfigurationLoader<CommentedConfigurationNode>>()

    init {
        if (Files.notExists(configDirectory)) {
            Files.createDirectories(configDirectory)
        }
    }

    /**
     * Carga de forma asíncrona un archivo de configuración HOCON mapeándolo a un tipo específico.
     */
    fun <T : Any> loadModuleConfig(fileName: String, configClass: Class<T>, defaultInstance: T): CompletableFuture<T> {
        return CompletableFuture.supplyAsync({
            val file = configDirectory.resolve(fileName)
            if (Files.notExists(file)) {
                Files.createFile(file)
            }

            val loader = HoconConfigurationLoader.builder().path(file).build()
            loaders[fileName] = loader

            try {
                val root = loader.load()
                val mapper = ObjectMapper.factory().get(configClass)
                val mappedInstance: T
                
                if (root.empty()) {
                    mapper.save(defaultInstance, root)
                    loader.save(root)
                    mappedInstance = defaultInstance
                } else {
                    mappedInstance = mapper.load(root) ?: defaultInstance
                }

                loadedConfigs[fileName] = mappedInstance
                mappedInstance
            } catch (e: ConfigurateException) {
                plugin.logger.severe("Fallo de sintaxis crítico en HOCON: $fileName")
                throw RuntimeException(e)
            }
        })
    }

    /**
     * Guarda de forma asíncrona una instancia de configuración en su archivo HOCON correspondiente.
     */
    fun <T : Any> saveModuleConfig(fileName: String, configClass: Class<T>, instance: T): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            val file = configDirectory.resolve(fileName)
            val loader = loaders[fileName] ?: HoconConfigurationLoader.builder().path(file).build()
            try {
                val root = loader.load() // Carga el nodo existente para mantener comentarios
                val mapper = ObjectMapper.factory().get(configClass)
                mapper.save(instance, root)
                loader.save(root)
                loadedConfigs[fileName] = instance
            } catch (e: ConfigurateException) {
                plugin.logger.severe("Fallo al guardar HOCON: $fileName")
                throw RuntimeException(e)
            }
        })
    }

    fun shutdown() {
        loadedConfigs.clear()
        loaders.clear()
    }
}
