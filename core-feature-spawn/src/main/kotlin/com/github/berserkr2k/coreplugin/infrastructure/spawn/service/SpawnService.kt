package com.github.berserkr2k.coreplugin.infrastructure.spawn.service

import com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler
import com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler
import com.github.berserkr2k.coreplugin.api.core.scheduler.Task
import com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService
import com.github.berserkr2k.coreplugin.api.core.state.StateContainer
import com.github.berserkr2k.coreplugin.api.core.state.StateContainerType
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import com.github.berserkr2k.coreplugin.api.core.config.FeatureConfig
import com.github.berserkr2k.coreplugin.infrastructure.spawn.SpawnMessages
import com.github.berserkr2k.coreplugin.infrastructure.spawn.SpawnConfig
import com.github.berserkr2k.coreplugin.infrastructure.spawn.PersistedLocation
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.util.NamingSchemes
import java.util.UUID

class SpawnStateContainer(
    var activeWarmup: Task? = null
) : StateContainer

val SPAWN_STATE = StateContainerType { SpawnStateContainer() }

class SpawnService(
    private val plugin: Plugin,
    private val featureConfig: FeatureConfig,
    private val taskScheduler: TaskScheduler,
    private val regionTaskScheduler: RegionTaskScheduler,
    private val playerStateService: PlayerStateService,
    private val messageService: MessageService
) : com.github.berserkr2k.coreplugin.api.core.lifecycle.Reloadable {
    lateinit var config: SpawnConfig
        private set

    private val mapperFactory = ObjectMapper.factoryBuilder()
        .defaultNamingScheme(NamingSchemes.PASSTHROUGH)
        .build()

    private val mapper = mapperFactory.get(SpawnConfig::class.java)

    private fun getRootNode(): CommentedConfigurationNode {
        val field = featureConfig.javaClass.getDeclaredField("rootNode")
        field.isAccessible = true
        return field.get(featureConfig) as CommentedConfigurationNode
    }

    private fun loadConfig() {
        val rootNode = getRootNode()
        this.config = mapper.load(rootNode) ?: SpawnConfig()
    }

    init {
        loadConfig()
    }

    override suspend fun reload() {
        featureConfig.reload()
        loadConfig()
    }

    private fun getSpawnState(uuid: UUID): SpawnStateContainer {
        return playerStateService.getContainer(uuid, SPAWN_STATE)
    }

    fun getSpawnLocation(worldName: String? = null): Location? {
        val targetWorld = worldName ?: Bukkit.getWorlds().firstOrNull()?.name ?: "world"
        val settings = getWorldSettings(targetWorld)
        return resolveLocation(settings.spawnLocation)
    }

    fun resolveLocation(pLoc: PersistedLocation): Location? {
        val world = Bukkit.getWorld(pLoc.world) ?: Bukkit.getWorlds().firstOrNull() ?: return null
        return Location(world, pLoc.x, pLoc.y, pLoc.z, pLoc.yaw, pLoc.pitch)
    }

    fun getWorldSettings(worldName: String): com.github.berserkr2k.coreplugin.infrastructure.spawn.WorldSpawnSettings {
        config.worlds[worldName]?.let { return it }

        // Fallback: search for first configured world in the map, or fallback to server's first world, or "world"
        val fallbackWorld = config.worlds.keys.firstOrNull() ?: Bukkit.getWorlds().firstOrNull()?.name ?: "world"
        config.worlds[fallbackWorld]?.let { return it }

        return com.github.berserkr2k.coreplugin.infrastructure.spawn.WorldSpawnSettings(
            voidTeleportEnabled = true,
            spawnLocation = PersistedLocation(world = fallbackWorld),
            voidThresholdY = -64,
            safeFallbackLocation = PersistedLocation(world = fallbackWorld)
        )
    }

    fun setSpawnLocation(loc: Location) {
        val pLoc = PersistedLocation(loc.world.name, loc.x, loc.y, loc.z, loc.yaw, loc.pitch)
        
        val updatedWorlds = config.worlds.toMutableMap()
        val currentWorldSettings = updatedWorlds[loc.world.name] ?: com.github.berserkr2k.coreplugin.infrastructure.spawn.WorldSpawnSettings(
            voidTeleportEnabled = true,
            spawnLocation = pLoc,
            voidThresholdY = -64,
            safeFallbackLocation = pLoc
        )
        updatedWorlds[loc.world.name] = currentWorldSettings.copy(spawnLocation = pLoc)

        val newConfig = config.copy(worlds = updatedWorlds)
        this.config = newConfig
        
        val rootNode = getRootNode()
        mapper.save(newConfig, rootNode)
        featureConfig.save().join()
        
        // Update Bukkit world spawn coordinate physically
        loc.world.setSpawnLocation(loc)
    }

    fun teleportToSpawn(player: Player, bypassWarmup: Boolean = false) {
        val destination = getSpawnLocation(player.world.name)
        if (destination == null) {
            messageService.send(player, SpawnMessages.NOT_CONFIGURED)
            return
        }

        if (bypassWarmup || config.warmupSeconds <= 0 || player.hasPermission("core.spawn.bypass.warmup")) {
            executeTeleport(player, destination)
            return
        }

        val state = getSpawnState(player.uniqueId)
        state.activeWarmup?.cancel()
        state.activeWarmup = null

        val warmupTime = config.warmupSeconds
        messageService.send(player, SpawnMessages.WARMUP, PlaceholderContext.of("time" to warmupTime.toString()))

        val task = taskScheduler.runSyncLater(Runnable {
            regionTaskScheduler.runAtLocation(player.location, Runnable {
                val st = getSpawnState(player.uniqueId)
                st.activeWarmup = null
                executeTeleport(player, destination)
            })
        }, warmupTime * 20L)

        state.activeWarmup = task
    }

    private fun executeTeleport(player: Player, destination: Location) {
        player.teleportAsync(destination).thenAccept { success ->
            if (success) {
                regionTaskScheduler.runAtLocation(destination, Runnable {
                    messageService.send(player, SpawnMessages.SUCCESS)
                    val soundName = config.teleportSound
                    val sound = try { Sound.valueOf(soundName.uppercase()) } catch (e: Exception) { Sound.ENTITY_ENDERMAN_TELEPORT }
                    player.playSound(destination, sound, 1.0f, 1.0f)
                })
            } else {
                regionTaskScheduler.runAtLocation(player.location, Runnable {
                    messageService.send(player, SpawnMessages.FAILURE)
                })
            }
        }
    }

    fun cancelWarmup(player: Player, key: SpawnMessages) {
        val state = getSpawnState(player.uniqueId)
        val task = state.activeWarmup
        if (task != null) {
            task.cancel()
            state.activeWarmup = null
            messageService.send(player, key)
        }
    }

    fun removeActiveWarmup(uuid: UUID) {
        val state = getSpawnState(uuid)
        state.activeWarmup?.cancel()
        state.activeWarmup = null
    }
}
