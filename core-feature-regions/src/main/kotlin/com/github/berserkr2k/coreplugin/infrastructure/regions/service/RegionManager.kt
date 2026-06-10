package com.github.berserkr2k.coreplugin.infrastructure.regions.service

import com.github.berserkr2k.coreplugin.api.regions.CompiledRegion
import com.github.berserkr2k.coreplugin.infrastructure.regions.RegionConfig
import com.github.berserkr2k.coreplugin.infrastructure.regions.spatial.SpatialRegionIndex
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.util.NamingSchemes
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class RegionManager(
    private val plugin: Plugin,
    private val configManager: ModularConfigManager
) {
    lateinit var config: RegionConfig
        private set

    private val currentIndex = AtomicReference(SpatialRegionIndex())
    private val regionsMap = ConcurrentHashMap<String, CompiledRegion>()

    private val mapperFactory = ObjectMapper.factoryBuilder()
        .defaultNamingScheme(NamingSchemes.PASSTHROUGH)
        .build()

    init {
        // Cargar la configuración principal del módulo de regiones
        configManager.loadModuleConfig("regions.conf", RegionConfig::class.java, RegionConfig())
            .thenAccept { loadedConfig ->
                this.config = loadedConfig
                loadAllRegions()
            }
    }

    fun getCurrentIndex(): SpatialRegionIndex = currentIndex.get()

    fun swapIndex(newIndex: SpatialRegionIndex) {
        currentIndex.set(newIndex)
    }

    fun getRegion(id: String): CompiledRegion? {
        return regionsMap[id.lowercase()]
    }

    fun getRegions(): Collection<CompiledRegion> {
        return regionsMap.values
    }

    fun createRegion(region: CompiledRegion): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            regionsMap[region.id.lowercase()] = region
            saveRegionToFile(region)
            rebuildIndex()
        }
    }

    fun removeRegion(id: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            val removed = regionsMap.remove(id.lowercase()) != null
            if (removed) {
                deleteRegionFile(id)
                rebuildIndex()
            }
            removed
        }
    }

    private fun rebuildIndex() {
        val newIndex = SpatialRegionIndex()
        newIndex.buildFrom(regionsMap.values)
        swapIndex(newIndex)
    }

    private fun getRegionsDirectory(): File {
        val dir = File(plugin.dataFolder, "regions")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getRegionFile(id: String): File {
        return File(getRegionsDirectory(), "${id.lowercase()}.conf")
    }

    fun loadAllRegions() {
        val dir = getRegionsDirectory()
        val files = dir.listFiles { _, name -> name.endsWith(".conf") } ?: return
        
        regionsMap.clear()
        for (file in files) {
            try {
                val loader = HoconConfigurationLoader.builder()
                    .path(file.toPath())
                    .defaultOptions { options ->
                        options.serializers { builder ->
                            builder.registerAnnotatedObjects(mapperFactory)
                        }
                    }
                    .build()
                val root = loader.load()
                val mapper = mapperFactory.get(CompiledRegion::class.java)
                val region = mapper.load(root)
                if (region != null && region.id.isNotEmpty()) {
                    regionsMap[region.id.lowercase()] = region
                }
            } catch (e: Exception) {
                plugin.logger.severe("Error al cargar la región desde el archivo ${file.name}: ${e.message}")
            }
        }
        rebuildIndex()
        plugin.logger.info("¡Se cargaron con éxito ${regionsMap.size} regiones desde la carpeta regions/!")
    }

    private fun saveRegionToFile(region: CompiledRegion) {
        val file = getRegionFile(region.id)
        try {
            val loader = HoconConfigurationLoader.builder()
                .path(file.toPath())
                .defaultOptions { options ->
                    options.serializers { builder ->
                        builder.registerAnnotatedObjects(mapperFactory)
                    }
                }
                .build()
            val root = loader.load()
            val mapper = mapperFactory.get(CompiledRegion::class.java)
            mapper.save(region, root)
            loader.save(root)
        } catch (e: Exception) {
            plugin.logger.severe("Error al guardar la región ${region.id} en el archivo: ${e.message}")
        }
    }

    private fun deleteRegionFile(id: String) {
        val file = getRegionFile(id)
        if (file.exists()) {
            file.delete()
        }
    }
}
