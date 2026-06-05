package com.github.berserkr2k.coreplugin.infrastructure.warps

import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.common.gui.MenuConfig
import com.github.berserkr2k.coreplugin.common.gui.FillerConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.ItemConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.getWarps
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
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

class WarpService(
    private val plugin: Plugin,
    private val configManager: ModularConfigManager,
    private val messagesConfig: MessagesConfig
) : Listener {

    private val warpsDir = plugin.dataFolder.resolve("warps")
    private val warps = ConcurrentHashMap<String, WarpConfig>()
    
    // UUID -> Warp Name -> Cooldown Expiration Timestamp (ms)
    private val cooldowns = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>>()
    
    // UUID -> Active Warmup Task
    private val activeWarmups = ConcurrentHashMap<UUID, ScheduledTask>()

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
            configManager.loadModuleConfig("warps/${file.name}", WarpConfig::class.java, WarpConfig(name = file.nameWithoutExtension))
                .thenAccept { loadedWarp ->
                    warps[name] = loadedWarp
                }.join()
        }
    }

    fun reload() {
        loadAllWarps()
        configManager.loadModuleConfig("menus/warps-selector.conf", MenuConfig::class.java, menuConfig)
            .thenAccept { this.menuConfig = it }.join()
    }

    fun getWarp(name: String): WarpConfig? {
        return warps[name.lowercase()]
    }

    fun getAllWarps(): Collection<WarpConfig> {
        return warps.values
    }

    fun setWarp(name: String, world: String, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
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
        configManager.saveModuleConfig("warps/$name.conf", WarpConfig::class.java, warpConfig)
    }

    fun deleteWarp(name: String): Boolean {
        val lowercaseName = name.lowercase()
        val config = warps.remove(lowercaseName) ?: return false
        
        // Eliminar archivo físico
        val file = File(warpsDir, "${config.name}.conf")
        if (file.exists()) {
            file.delete()
        }
        return true
    }

    fun handleTeleportRequest(player: Player, warp: WarpConfig) {
        // 1. Validar Permisos
        if (warp.permission.isNotEmpty() && !player.hasPermission(warp.permission)) {
            player.sendMessage(ColorUtility.parse(messagesConfig.getWarps("no-permission", "name" to warp.name)))
            return
        }

        // 2. Validar Cooldown (salvo bypass)
        val now = System.currentTimeMillis()
        if (!player.hasPermission("core.warps.bypass.cooldown")) {
            val userCooldowns = cooldowns[player.uniqueId]
            if (userCooldowns != null) {
                val exp = userCooldowns[warp.name.lowercase()]
                if (exp != null && exp > now) {
                    val remaining = (exp - now + 999) / 1000
                    player.sendMessage(ColorUtility.parse(messagesConfig.getWarps("cooldown-active", "time" to remaining)))
                    return
                }
            }
        }

        // 3. Validar Warmup
        val warmup = warp.warmupSeconds
        if (warmup > 0 && !player.hasPermission("core.warps.bypass.warmup")) {
            // Cancelar warmup anterior si hay
            activeWarmups.remove(player.uniqueId)?.cancel()

            player.sendMessage(ColorUtility.parse(messagesConfig.getWarps("warmup", "time" to warmup)))
            
            // Iniciar tarea regional segura
            val task = Bukkit.getRegionScheduler().runDelayed(plugin, player.location, { _ ->
                activeWarmups.remove(player.uniqueId)
                performTeleport(player, warp)
            }, warmup * 20L)
            
            activeWarmups[player.uniqueId] = task
        } else {
            performTeleport(player, warp)
        }
    }

    private fun performTeleport(player: Player, warp: WarpConfig) {
        val world = Bukkit.getWorld(warp.world)
        if (world == null) {
            player.sendMessage(ColorUtility.parse("<red>El mundo de destino '${warp.world}' no está cargado.</red>"))
            return
        }

        val loc = Location(world, warp.x, warp.y, warp.z, warp.yaw, warp.pitch)
        
        player.teleportAsync(loc).thenAccept { success ->
            if (success) {
                player.sendMessage(ColorUtility.parse(messagesConfig.getWarps("success", "name" to warp.name)))
                player.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f)
                
                // Aplicar Cooldown si es > 0
                if (warp.cooldownSeconds > 0) {
                    val expireTime = System.currentTimeMillis() + (warp.cooldownSeconds * 1000L)
                    cooldowns.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }[warp.name.lowercase()] = expireTime
                }
            } else {
                player.sendMessage(ColorUtility.parse("<red>No se pudo realizar la teletransportación.</red>"))
            }
        }
    }

    private fun cancelWarmup(player: Player, messageKey: String) {
        val task = activeWarmups.remove(player.uniqueId)
        if (task != null) {
            task.cancel()
            player.sendMessage(ColorUtility.parse(messagesConfig.getWarps(messageKey)))
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        if (activeWarmups.containsKey(player.uniqueId)) {
            // Verificar si hubo un cambio de bloque real
            val from = event.from
            val to = event.to
            if (from.blockX != to.blockX || from.blockY != to.blockY || from.blockZ != to.blockZ) {
                cancelWarmup(player, "cancelled-movement")
            }
        }
    }

    @EventHandler
    fun onPlayerDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (activeWarmups.containsKey(player.uniqueId)) {
            cancelWarmup(player, "cancelled-damage")
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        activeWarmups.remove(event.player.uniqueId)?.cancel()
        cooldowns.remove(event.player.uniqueId)
    }
}
