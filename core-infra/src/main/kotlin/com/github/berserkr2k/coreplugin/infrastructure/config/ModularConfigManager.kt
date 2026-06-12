package com.github.berserkr2k.coreplugin.infrastructure.config

import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.util.NamingSchemes
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture
import org.bukkit.plugin.Plugin

class ModularConfigManager(private val plugin: Plugin, private val configDirectory: Path) {
    private val loadedConfigs = ConcurrentHashMap<String, Any>()
    private val loaders = ConcurrentHashMap<String, ConfigurationLoader<CommentedConfigurationNode>>()

    fun getLoadedConfigs(): Set<String> = loadedConfigs.keys

    private val mapperFactory = ObjectMapper.factoryBuilder()
        .defaultNamingScheme(NamingSchemes.PASSTHROUGH)
        .build()

    init {
        if (Files.notExists(configDirectory)) {
            Files.createDirectories(configDirectory)
        }
    }

    /**
     * Carga de forma asíncrona un archivo de configuración HOCON mapeándolo a un tipo específico.
     * Guarda automáticamente los nuevos campos predeterminados en el disco.
     */
    fun <T : Any> loadModuleConfig(fileName: String, configClass: Class<T>, defaultInstance: T): CompletableFuture<T> {
        return CompletableFuture.supplyAsync({
            val file = configDirectory.resolve(fileName)
            if (Files.notExists(file)) {
                val parent = file.parent
                if (parent != null && Files.notExists(parent)) {
                    Files.createDirectories(parent)
                }
                Files.createFile(file)
            }

            val loader = HoconConfigurationLoader.builder()
                .path(file)
                .defaultOptions { options ->
                    options.serializers { builder ->
                        builder.registerAnnotatedObjects(mapperFactory)
                    }
                }
                .build()
            loaders[fileName] = loader

            try {
                val root = loader.load()
                val mapper = mapperFactory.get(configClass)
                val mappedInstance: T
                
                if (root.empty()) {
                    mapper.save(defaultInstance, root)
                    loader.save(root)
                    mappedInstance = defaultInstance
                } else {
                    mappedInstance = mapper.load(root) ?: defaultInstance
                    // Limpiar y ordenar el archivo físico eliminando claves inválidas/obsoletas
                    val tempRoot = loader.createNode()
                    mapper.save(mappedInstance, tempRoot)
                    
                    val orderedRoot = loader.createNode()
                    reorderAndClean(root, tempRoot, orderedRoot)
                    
                    loader.save(orderedRoot)
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
            val loader = loaders[fileName] ?: HoconConfigurationLoader.builder()
                .path(file)
                .defaultOptions { options ->
                    options.serializers { builder ->
                        builder.registerAnnotatedObjects(mapperFactory)
                    }
                }
                .build()
            try {
                val root = loader.load() // Carga el nodo existente para mantener comentarios
                val mapper = mapperFactory.get(configClass)
                
                val tempRoot = loader.createNode()
                mapper.save(instance, tempRoot)
                
                val orderedRoot = loader.createNode()
                reorderAndClean(root, tempRoot, orderedRoot)
                
                loader.save(orderedRoot)
                loadedConfigs[fileName] = instance
            } catch (e: ConfigurateException) {
                plugin.logger.severe("Fallo al guardar HOCON: $fileName")
                throw RuntimeException(e)
            }
        })
    }

    private fun reorderAndClean(
        original: CommentedConfigurationNode,
        schema: CommentedConfigurationNode,
        target: CommentedConfigurationNode
    ) {
        original.comment()?.let { target.comment(it) }

        if (schema.isMap) {
            val schemaKeys = schema.childrenMap().keys
            
            // Procesar llaves de esquema en el orden correcto
            for (key in schemaKeys) {
                val schemaChild = schema.node(key)
                val originalChild = original.node(key)
                val targetChild = target.node(key)
                reorderAndClean(originalChild, schemaChild, targetChild)
            }
        } else {
            target.raw(schema.raw())
        }
    }

    fun shutdown() {
        loadedConfigs.clear()
        loaders.clear()
    }
}
