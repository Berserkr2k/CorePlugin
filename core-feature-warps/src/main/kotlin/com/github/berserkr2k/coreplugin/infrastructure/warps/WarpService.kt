package com.github.berserkr2k.coreplugin.infrastructure.warps

import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuConfig
import com.github.berserkr2k.coreplugin.api.framework.menu.FillerConfig
import com.github.berserkr2k.coreplugin.api.config.ItemConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.filesystem.FeatureFolderProvider
import com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler
import com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler
import com.github.berserkr2k.coreplugin.api.core.scheduler.Task
import com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService
import com.github.berserkr2k.coreplugin.api.core.state.StateContainer
import com.github.berserkr2k.coreplugin.api.core.state.StateContainerType
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class WarpStateContainer(
    val cooldowns: ConcurrentHashMap<String, Long> = ConcurrentHashMap(),
    var activeWarmup: Task? = null
) : StateContainer

val WARP_STATE = StateContainerType { WarpStateContainer() }

class WarpService(
    private val plugin: Plugin,
    private val configManager: ModularConfigManager,
    private val messageService: MessageService,
    private val registry: ServiceRegistry
) : Listener, com.github.berserkr2k.coreplugin.api.feature.warps.WarpService, com.github.berserkr2k.coreplugin.api.core.lifecycle.Reloadable {

    private val taskScheduler = registry.get(TaskScheduler::class.java)
    private val regionTaskScheduler = registry.get(RegionTaskScheduler::class.java)
    private val stateService = registry.get(PlayerStateService::class.java)

    private val folderProvider = registry.get(FeatureFolderProvider::class.java)!!
    private val warpsDir = folderProvider.getFeatureFolder("warps").resolve("warps").toFile()
    private val warps = ConcurrentHashMap<String, WarpConfig>()

    private fun getWarpState(uuid: UUID): WarpStateContainer {
        return stateService.getContainer(uuid, WARP_STATE)
    }

    var menuConfig = MenuConfig(
        title = "<dark_gray>Puntos de Teletransporte</dark_gray>",
        size = 27,
        filler = FillerConfig(
            enabled = true,
            item = ItemConfig(material = "GRAY_STAINED_GLASS_PANE", displayName = " ")
        )
    )
        private set

    init {
        // Asegurar que la carpeta warps existe
        if (!warpsDir.exists()) {
            warpsDir.mkdirs()
        }
        
        loadAllWarps()

        configManager.loadModuleConfig("menus/warps-selector.conf", MenuConfig::class.java, menuConfig)
            .thenAccept { this.menuConfig = it }
    }

    fun loadAllWarps() {
        warps.clear()
        val files = warpsDir.listFiles { _, name -> name.endsWith(".conf") } ?: return
        for (file in files) {
            val name = file.nameWithoutExtension.lowercase()
            configManager.loadModuleConfig("warps/warps/${file.name}", WarpConfig::class.java, WarpConfig(name = file.nameWithoutExtension))
                .thenAccept { loadedWarp ->
                    warps[name] = loadedWarp
                }.join()
        }
    }

    override suspend fun reload() {
        loadAllWarps()
        configManager.loadModuleConfig("menus/warps-selector.conf", MenuConfig::class.java, menuConfig)
            .thenAccept { this.menuConfig = it }.join()
    }

    fun getWarpConfig(name: String): WarpConfig? {
        return warps[name.lowercase()]
    }

    fun getAllWarpConfigs(): Collection<WarpConfig> {
        return warps.values
    }

    override fun getWarp(name: String): com.github.berserkr2k.coreplugin.api.feature.warps.CompiledWarp? {
        val w = warps[name.lowercase()] ?: return null
        val loc = resolveLocation(w) ?: return null
        return com.github.berserkr2k.coreplugin.api.feature.warps.CompiledWarp(
            name = w.name,
            location = loc,
            permission = w.permission,
            warmupSeconds = w.warmupSeconds,
            cooldownSeconds = w.cooldownSeconds,
            guiSlot = w.guiSlot,
            displayName = w.item.displayName ?: w.name
        )
    }

    override fun getAllWarps(): List<com.github.berserkr2k.coreplugin.api.feature.warps.CompiledWarp> {
        return warps.values.mapNotNull { getWarp(it.name) }
    }

    override fun teleport(player: Player, warp: com.github.berserkr2k.coreplugin.api.feature.warps.CompiledWarp): java.util.concurrent.CompletableFuture<Boolean> {
        val future = java.util.concurrent.CompletableFuture<Boolean>()
        val wConfig = warps[warp.name.lowercase()]
        if (wConfig == null) {
            future.complete(false)
            return future
        }
        val loc = Location(Bukkit.getWorld(wConfig.world), wConfig.x, wConfig.y, wConfig.z, wConfig.yaw, wConfig.pitch)
        player.teleportAsync(loc).thenAccept { success ->
            if (success) {
                regionTaskScheduler.runAtLocation(loc, Runnable {
                    messageService.send(player, WarpMessages.SUCCESS, PlaceholderContext.of("name" to warp.name))
                    val soundName = wConfig.teleportSound
                    val sound = try { Sound.valueOf(soundName) } catch (e: Exception) { Sound.ENTITY_ENDERMAN_TELEPORT }
                    player.playSound(loc, sound, 1.0f, 1.0f)
                })
                if (wConfig.cooldownSeconds > 0) {
                    val expireTime = System.currentTimeMillis() + (wConfig.cooldownSeconds * 1000L)
                    val warpState = getWarpState(player.uniqueId)
                    warpState.cooldowns[wConfig.name.lowercase()] = expireTime
                }
            }
            future.complete(success)
        }
        return future
    }

    override fun setWarp(name: String, world: String, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
        val lowercaseName = name.lowercase()
        val existing = warps[lowercaseName]
        val warpConfig = WarpConfig(
            name = name,
            world = world,
            x = x,
            y = y,
            z = z,
            yaw = yaw,
            pitch = pitch,
            permission = existing?.permission ?: "core.warps.use.$lowercaseName",
            warmupSeconds = existing?.warmupSeconds ?: 0,
            cooldownSeconds = existing?.cooldownSeconds ?: 0,
            guiSlot = existing?.guiSlot ?: -1,
            item = existing?.item ?: ItemConfig(
                material = "ENDER_PEARL",
                displayName = "<green><bold>Warp $name</bold></green>",
                lore = listOf(
                    "<gray>Haz clic para viajar a este warp.</gray>",
                    " ",
                    "<yellow>▶ Click para viajar</yellow>"
                )
            )
        )

        warps[lowercaseName] = warpConfig
        // Guardar asíncronamente
        configManager.saveModuleConfig("warps/warps/$name.conf", WarpConfig::class.java, warpConfig)
    }

    override fun deleteWarp(name: String): Boolean {
        val lowercaseName = name.lowercase()
        val config = warps.remove(lowercaseName) ?: return false
        
        // Eliminar archivo físico
        val file = File(warpsDir, "${config.name}.conf")
        if (file.exists()) {
            file.delete()
        }
        return true
    }

    private fun resolveLocation(config: WarpConfig): Location? {
        val world = Bukkit.getWorld(config.world) ?: return null
        return Location(world, config.x, config.y, config.z, config.yaw, config.pitch)
    }

    fun handleTeleportRequest(player: Player, warp: WarpConfig) {
        // 1. Validar Permisos
        if (warp.permission.isNotEmpty() && !player.hasPermission(warp.permission)) {
            messageService.send(player, WarpMessages.NO_PERMISSION, PlaceholderContext.of("name" to warp.name))
            return
        }

        // 2. Validar Cooldown (salvo bypass)
        val now = System.currentTimeMillis()
        val warpState = getWarpState(player.uniqueId)
        if (!player.hasPermission("core.warps.bypass.cooldown")) {
            val userCooldowns = warpState.cooldowns
            val exp = userCooldowns[warp.name.lowercase()]
            if (exp != null && exp > now) {
                val remaining = (exp - now + 999) / 1000
                messageService.send(player, WarpMessages.COOLDOWN_ACTIVE, PlaceholderContext.of("time" to remaining.toString()))
                return
            }
        }

        // 3. Validar Warmup
        val warmup = warp.warmupSeconds
        if (warmup > 0 && !player.hasPermission("core.warps.bypass.warmup")) {
            // Cancelar warmup anterior si hay
            warpState.activeWarmup?.cancel()
            warpState.activeWarmup = null

            messageService.send(player, WarpMessages.WARMUP, PlaceholderContext.of("time" to warmup.toString()))
            
            // Iniciar tarea regional segura
            val task = taskScheduler.runSyncLater(Runnable {
                regionTaskScheduler.runAtLocation(player.location, Runnable {
                    val st = getWarpState(player.uniqueId)
                    st.activeWarmup = null
                    performTeleport(player, warp)
                })
            }, warmup * 20L)
            
            warpState.activeWarmup = task
        } else {
            performTeleport(player, warp)
        }
    }

    private fun performTeleport(player: Player, warp: WarpConfig) {
        val world = Bukkit.getWorld(warp.world)
        if (world == null) {
            messageService.send(player, WarpMessages.WORLD_NOT_LOADED, PlaceholderContext.of("world" to warp.world))
            return
        }

        val loc = Location(world, warp.x, warp.y, warp.z, warp.yaw, warp.pitch)
        
        player.teleportAsync(loc).thenAccept { success ->
            if (success) {
                messageService.send(player, WarpMessages.SUCCESS, PlaceholderContext.of("name" to warp.name))
                player.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f)
                
                // Aplicar Cooldown si es > 0
                if (warp.cooldownSeconds > 0) {
                    val expireTime = System.currentTimeMillis() + (warp.cooldownSeconds * 1000L)
                    val warpState = getWarpState(player.uniqueId)
                    warpState.cooldowns[warp.name.lowercase()] = expireTime
                }
            } else {
                messageService.send(player, WarpMessages.TELEPORT_FAILED)
            }
        }
    }

    private fun cancelWarmup(player: Player, messageKey: WarpMessages) {
        val warpState = getWarpState(player.uniqueId)
        val task = warpState.activeWarmup
        if (task != null) {
            task.cancel()
            warpState.activeWarmup = null
            messageService.send(player, messageKey)
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val warpState = getWarpState(player.uniqueId)
        if (warpState.activeWarmup != null) {
            // Verificar si hubo un cambio de bloque real
            val from = event.from
            val to = event.to
            if (from.blockX != to.blockX || from.blockY != to.blockY || from.blockZ != to.blockZ) {
                cancelWarmup(player, WarpMessages.CANCELLED_MOVEMENT)
            }
        }
    }

    @EventHandler
    fun onPlayerDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        val warpState = getWarpState(player.uniqueId)
        if (warpState.activeWarmup != null) {
            cancelWarmup(player, WarpMessages.CANCELLED_DAMAGE)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val warpState = getWarpState(event.player.uniqueId)
        warpState.activeWarmup?.cancel()
        warpState.activeWarmup = null
    }
}
