package com.github.berserkr2k.coreplugin.infrastructure.spawn.service

import com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler
import com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler
import com.github.berserkr2k.coreplugin.api.core.scheduler.Task
import com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService
import com.github.berserkr2k.coreplugin.api.core.state.StateContainer
import com.github.berserkr2k.coreplugin.api.core.state.StateContainerType
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import com.github.berserkr2k.coreplugin.infrastructure.spawn.SpawnMessages
import com.github.berserkr2k.coreplugin.infrastructure.spawn.SpawnConfig
import com.github.berserkr2k.coreplugin.infrastructure.spawn.PersistedLocation
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.UUID

class SpawnStateContainer(
    var activeWarmup: Task? = null
) : StateContainer

val SPAWN_STATE = StateContainerType { SpawnStateContainer() }

class SpawnService(
    private val plugin: Plugin,
    private val configService: com.github.berserkr2k.coreplugin.api.core.config.ConfigService,
    private val taskScheduler: TaskScheduler,
    private val regionTaskScheduler: RegionTaskScheduler,
    private val playerStateService: PlayerStateService,
    private val messageService: MessageService
) : com.github.berserkr2k.coreplugin.api.core.lifecycle.Reloadable {
    lateinit var config: SpawnConfig
        private set

    private val configManager: ModularConfigManager by lazy {
        Bukkit.getServicesManager().load(com.github.berserkr2k.coreplugin.api.di.ServiceRegistry::class.java)
            ?.get(ModularConfigManager::class.java)
            ?: throw IllegalStateException("ModularConfigManager not registered in ServiceRegistry")
    }

    init {
        configManager.loadModuleConfig("spawn/spawn.conf", SpawnConfig::class.java, SpawnConfig())
            .thenAccept { loaded ->
                this.config = loaded
            }
    }

    override suspend fun reload() {
        configManager.loadModuleConfig("spawn/spawn.conf", SpawnConfig::class.java, SpawnConfig())
            .thenAccept { loaded ->
                this.config = loaded
            }.join()
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
        config = newConfig
        configManager.saveModuleConfig("spawn/spawn.conf", SpawnConfig::class.java, newConfig)
        
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
