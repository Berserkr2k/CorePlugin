package com.github.berserkr2k.coreplugin.infrastructure.regions.service

import com.github.berserkr2k.coreplugin.infrastructure.regions.CompiledRegion
import com.github.berserkr2k.coreplugin.infrastructure.regions.GlobalRegionConfig
import com.github.berserkr2k.coreplugin.infrastructure.regions.RegionConfig
import com.github.berserkr2k.coreplugin.infrastructure.regions.compiler.RegionCompiler
import com.github.berserkr2k.coreplugin.infrastructure.regions.spatial.SpatialRegionIndex
import com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class RegionManager(
    private val plugin: Plugin,
    private val configService: com.github.berserkr2k.coreplugin.api.core.config.ConfigService,
    private val taskScheduler: com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler
) : com.github.berserkr2k.coreplugin.api.core.lifecycle.Reloadable,
    com.github.berserkr2k.coreplugin.api.framework.regions.RegionService {

    private val registry = org.bukkit.Bukkit.getServicesManager().load(com.github.berserkr2k.coreplugin.api.di.ServiceRegistry::class.java)
        ?: throw IllegalStateException("ServiceRegistry not found in ServicesManager")

    val resolver = com.github.berserkr2k.coreplugin.infrastructure.regions.resolver.RegionRuleResolver(this)

    val selectionSession by lazy {
        val playerStateService = registry.get(PlayerStateService::class.java)!!
        com.github.berserkr2k.coreplugin.infrastructure.regions.command.PlayerSelectionSession(playerStateService)
    }

    override fun hasFlag(location: org.bukkit.Location, flag: com.github.berserkr2k.coreplugin.api.framework.regions.RegionFlag): Boolean {
        val worldIdx = com.github.berserkr2k.coreplugin.infrastructure.regions.WorldIndexRegistry.getIndex(location.world.uid)
        val x = location.blockX
        val y = location.blockY
        val z = location.blockZ
        val candidates = getCurrentIndex().getRegionsInChunk(worldIdx, x shr 4, z shr 4) ?: return flag.mask != com.github.berserkr2k.coreplugin.api.framework.regions.RegionFlags.USE_WITHOUT_BREAK
        
        for (i in candidates.indices.reversed()) {
            val region = candidates[i]
            if (region.worldIndex == worldIdx && region.contains(x, y, z) && region.hasFlag(flag)) {
                return region.isAllowed(flag)
            }
        }
        
        val fallbackMask = when (flag.mask) {
            com.github.berserkr2k.coreplugin.api.framework.regions.RegionFlags.BLOCK_BREAK,
            com.github.berserkr2k.coreplugin.api.framework.regions.RegionFlags.BLOCK_PLACE -> com.github.berserkr2k.coreplugin.api.framework.regions.RegionFlags.BUILD
            else -> 0L
        }
        
        if (fallbackMask != 0L) {
            val fallbackFlag = com.github.berserkr2k.coreplugin.api.framework.regions.RegionFlags.getFlagByMask(fallbackMask)
            if (fallbackFlag != null) {
                for (i in candidates.indices.reversed()) {
                    val region = candidates[i]
                    if (region.worldIndex == worldIdx && region.contains(x, y, z) && region.hasFlag(fallbackFlag)) {
                        return region.isAllowed(fallbackFlag)
                    }
                }
            }
        }
        
        return flag.mask != com.github.berserkr2k.coreplugin.api.framework.regions.RegionFlags.USE_WITHOUT_BREAK
    }

    override fun getRegion(location: org.bukkit.Location): com.github.berserkr2k.coreplugin.api.framework.regions.Region? {
        val worldIdx = com.github.berserkr2k.coreplugin.infrastructure.regions.WorldIndexRegistry.getIndex(location.world.uid)
        val x = location.blockX
        val y = location.blockY
        val z = location.blockZ
        val candidates = getCurrentIndex().getRegionsInChunk(worldIdx, x shr 4, z shr 4) ?: return null
        
        for (i in candidates.indices.reversed()) {
            val region = candidates[i]
            if (region.worldIndex == worldIdx && region.contains(x, y, z)) {
                return region
            }
        }
        return null
    }

    override fun getRegions(location: org.bukkit.Location): List<com.github.berserkr2k.coreplugin.api.framework.regions.Region> {
        val worldIdx = com.github.berserkr2k.coreplugin.infrastructure.regions.WorldIndexRegistry.getIndex(location.world.uid)
        val x = location.blockX
        val y = location.blockY
        val z = location.blockZ
        val candidates = getCurrentIndex().getRegionsInChunk(worldIdx, x shr 4, z shr 4) ?: return emptyList()
        
        val active = mutableListOf<com.github.berserkr2k.coreplugin.api.framework.regions.Region>()
        for (region in candidates) {
            if (region.worldIndex == worldIdx && region.contains(x, y, z)) {
                active.add(region)
            }
        }
        return active
    }

    override fun getHighestPriorityRegion(location: org.bukkit.Location): com.github.berserkr2k.coreplugin.api.framework.regions.Region? {
        val regions = getRegions(location)
        return regions.maxByOrNull { it.priority }
    }

    lateinit var config: GlobalRegionConfig
        private set

    private val currentIndex = AtomicReference(SpatialRegionIndex())
    private val regionsMap = ConcurrentHashMap<String, RegionConfig>()

    private fun loadConfig() {
        try {
            val file = File(plugin.dataFolder, "config/regions.conf")
            this.config = configService.loadConfig(file, GlobalRegionConfig::class.java, GlobalRegionConfig())
        } catch (e: Exception) {
            plugin.logger.severe("Error al cargar la configuración global de regiones: ${e.message}")
            this.config = GlobalRegionConfig()
        }
    }

    init {
        taskScheduler.runAsync {
            loadConfig()
            loadAllRegions()
        }
    }

    override suspend fun reload() {
        loadConfig()
        loadAllRegions()
    }

    fun getCurrentIndex(): SpatialRegionIndex = currentIndex.get()

    fun swapIndex(newIndex: SpatialRegionIndex) {
        currentIndex.set(newIndex)
    }

    fun getRegionDTO(id: String): RegionConfig? {
        return regionsMap[id.lowercase()]
    }

    fun getRegionsDTOs(): Collection<RegionConfig> {
        return regionsMap.values
    }

    fun createRegion(region: RegionConfig): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            regionsMap[region.id.lowercase()] = region
            saveRegionToFile(region)
            rebuildIndex()
        }, { taskScheduler.runAsync(it) })
    }

    override fun createRegion(
        id: String,
        worldName: String,
        priority: Int,
        minX: Int,
        minY: Int,
        minZ: Int,
        maxX: Int,
        maxY: Int,
        maxZ: Int,
        allowFlags: List<String>,
        denyFlags: List<String>,
        tags: Map<String, String>,
        type: com.github.berserkr2k.coreplugin.api.framework.regions.RegionType
    ): CompletableFuture<Void> {
        val region = RegionConfig(
            id = id,
            world = worldName,
            priority = priority,
            minX = minX, minY = minY, minZ = minZ,
            maxX = maxX, maxY = maxY, maxZ = maxZ,
            allowFlags = allowFlags,
            denyFlags = denyFlags,
            tags = tags,
            type = type
        )
        return createRegion(region)
    }

    override fun removeRegion(id: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            val removed = regionsMap.remove(id.lowercase()) != null
            if (removed) {
                deleteRegionFile(id)
                rebuildIndex()
            }
            removed
        }, { taskScheduler.runAsync(it) })
    }

    fun rebuildIndex() {
        val newIndex = SpatialRegionIndex()
        val compiledRegions = ArrayList<CompiledRegion>()
        for (dto in regionsMap.values) {
            try {
                val compiled = RegionCompiler.compile(dto)
                compiledRegions.add(compiled)
            } catch (e: Exception) {
                plugin.logger.warning("No se pudo compilar la región '${dto.id}': ${e.message}")
            }
        }
        newIndex.buildFrom(compiledRegions)
        swapIndex(newIndex)
    }

    private fun getRegionsDirectory(): File {
        val folderProvider = registry.get(com.github.berserkr2k.coreplugin.api.core.filesystem.FeatureFolderProvider::class.java)!!
        return folderProvider.getFeatureDataFolder("regions").toFile()
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
                val region = configService.loadConfig(file, RegionConfig::class.java, RegionConfig())
                if (region.id.isNotEmpty()) {
                    regionsMap[region.id.lowercase()] = region
                }
            } catch (e: Exception) {
                plugin.logger.severe("Error al cargar la región desde el archivo ${file.name}: ${e.message}")
            }
        }
        rebuildIndex()
        plugin.logger.info("¡Se cargaron con éxito ${regionsMap.size} regiones desde la carpeta data/regions!")
    }

    private fun saveRegionToFile(region: RegionConfig) {
        val file = getRegionFile(region.id)
        try {
            configService.saveConfig(file, RegionConfig::class.java, region)
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
