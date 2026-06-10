package com.github.berserkr2k.coreplugin.infrastructure.spawn.service

import com.github.berserkr2k.coreplugin.api.scheduler.TaskScheduler
import com.github.berserkr2k.coreplugin.api.scheduler.RegionTaskScheduler
import com.github.berserkr2k.coreplugin.api.scheduler.Task
import com.github.berserkr2k.coreplugin.api.state.PlayerStateService
import com.github.berserkr2k.coreplugin.api.state.StateContainer
import com.github.berserkr2k.coreplugin.api.state.StateContainerType
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.getSpawn
import com.github.berserkr2k.coreplugin.infrastructure.spawn.SpawnConfig
import com.github.berserkr2k.coreplugin.infrastructure.spawn.PersistedLocation
import com.github.berserkr2k.coreplugin.common.ColorUtility
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
    private val configManager: ModularConfigManager,
    private val taskScheduler: TaskScheduler,
    private val regionTaskScheduler: RegionTaskScheduler,
    private val playerStateService: PlayerStateService,
    private val messagesConfig: MessagesConfig
) {
    lateinit var config: SpawnConfig
        private set

    init {
        configManager.loadModuleConfig("spawn.conf", SpawnConfig::class.java, SpawnConfig())
            .thenAccept { loaded ->
                this.config = loaded
            }
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
        return config.worlds[worldName] ?: com.github.berserkr2k.coreplugin.infrastructure.spawn.WorldSpawnSettings(
            voidTeleportEnabled = true,
            spawnLocation = PersistedLocation(world = worldName),
            voidThresholdY = -64,
            safeFallbackLocation = PersistedLocation(world = worldName)
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
        configManager.saveModuleConfig("spawn.conf", SpawnConfig::class.java, newConfig)
        
        // Update Bukkit world spawn coordinate physically
        loc.world.setSpawnLocation(loc)
    }

    fun teleportToSpawn(player: Player, bypassWarmup: Boolean = false) {
        val destination = getSpawnLocation(player.world.name)
        if (destination == null) {
            player.sendMessage(ColorUtility.parse(messagesConfig.getSpawn("not-configured")))
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
        player.sendMessage(ColorUtility.parse(messagesConfig.getSpawn("warmup", "time" to warmupTime)))

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
                player.sendMessage(ColorUtility.parse(messagesConfig.getSpawn("success")))
                player.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f)
            } else {
                player.sendMessage(ColorUtility.parse("<red>❌ No se pudo realizar la teletransportación.</red>"))
            }
        }
    }

    fun cancelWarmup(player: Player, messageKey: String) {
        val state = getSpawnState(player.uniqueId)
        val task = state.activeWarmup
        if (task != null) {
            task.cancel()
            state.activeWarmup = null
            player.sendMessage(ColorUtility.parse(messagesConfig.getSpawn(messageKey)))
        }
    }

    fun removeActiveWarmup(uuid: UUID) {
        val state = getSpawnState(uuid)
        state.activeWarmup?.cancel()
        state.activeWarmup = null
    }
}
