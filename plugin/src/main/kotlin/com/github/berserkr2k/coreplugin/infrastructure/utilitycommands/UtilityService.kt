package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import com.github.berserkr2k.coreplugin.infrastructure.config.UtilityConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import net.kyori.adventure.text.minimessage.MiniMessage
import java.util.concurrent.CompletableFuture

class UtilityService(
    private val plugin: Plugin,
    private val configManager: ModularConfigManager,
    private val messagesConfig: MessagesConfig
) : Listener {

    lateinit var config: UtilityConfig
        private set

    private val miniMessage = MiniMessage.miniMessage()

    init {
        // Cargar configuración de utilidades síncronamente al iniciar
        reloadConfig().join()
        
        // Registrar listener de control de vuelo
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    /**
     * Recarga el archivo de configuración utility.conf
     */
    fun reloadConfig(): CompletableFuture<Void> {
        return configManager.loadModuleConfig("utility.conf", UtilityConfig::class.java, UtilityConfig())
            .thenAccept { loadedConfig ->
                this.config = loadedConfig
                plugin.logger.info("¡Configuración de Utilidades recargada con éxito!")
            }
    }

    /**
     * Verifica si el vuelo está permitido para un jugador en su mundo actual.
     */
    fun isFlyAllowed(player: Player): Boolean {
        if (player.hasPermission("core.utility.fly.bypass")) return true
        val worldName = player.world.name
        return config.fly.allowedWorlds.contains(worldName)
    }

    /**
     * Comprobación de seguridad de vuelo al cambiar de mundo.
     */
    @EventHandler
    fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
        checkFlightSecurity(event.player)
    }

    /**
     * Comprobación de seguridad al teletransportarse.
     */
    @EventHandler
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        // Retrasamos la comprobación 1 tick para que el jugador se encuentre en la posición destino
        Bukkit.getRegionScheduler().runDelayed(plugin, event.to, { _ ->
            checkFlightSecurity(event.player)
        }, 1L)
    }

    /**
     * Comprobación al unirse al servidor.
     */
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        checkFlightSecurity(event.player)
    }

    /**
     * Resetea la velocidad al valor por defecto cuando el jugador se desconecta.
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        player.flySpeed = 0.1f
        player.walkSpeed = 0.2f
    }

    private fun checkFlightSecurity(player: Player) {
        if (!player.allowFlight) return
        if (isFlyAllowed(player)) return

        // Ejecutar de forma segura en la región del jugador
        Bukkit.getRegionScheduler().execute(plugin, player.location) {
            player.allowFlight = false
            player.isFlying = false
            
            val msg = messagesConfig.utility["fly-world-left"] ?: "<red>Has salido de un mundo permitido para volar. Modo de vuelo desactivado.</red>"
            player.sendMessage(miniMessage.deserialize(msg))
        }
    }
}
