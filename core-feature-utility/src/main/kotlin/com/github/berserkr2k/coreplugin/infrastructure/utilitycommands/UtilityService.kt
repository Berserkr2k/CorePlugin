package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import java.util.concurrent.CompletableFuture

class UtilityService(
    private val plugin: Plugin,
    private val featureConfig: com.github.berserkr2k.coreplugin.api.core.config.FeatureConfig
) : Listener, com.github.berserkr2k.coreplugin.api.core.lifecycle.Reloadable {

    private val registry = org.bukkit.Bukkit.getServicesManager().load(com.github.berserkr2k.coreplugin.api.di.ServiceRegistry::class.java)
        ?: throw IllegalStateException("ServiceRegistry not found in ServicesManager")
    val taskScheduler = registry.get(com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler::class.java)
    val regionTaskScheduler = registry.get(com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler::class.java)
    private val messageService = registry.get(com.github.berserkr2k.coreplugin.api.core.message.MessageService::class.java)

    lateinit var config: UtilityConfig
        private set

    private val mapperFactory = org.spongepowered.configurate.objectmapping.ObjectMapper.factoryBuilder()
        .defaultNamingScheme(org.spongepowered.configurate.util.NamingSchemes.PASSTHROUGH)
        .build()

    private val mapper = mapperFactory.get(UtilityConfig::class.java)

    private fun getRootNode(): org.spongepowered.configurate.CommentedConfigurationNode {
        val field = featureConfig.javaClass.getDeclaredField("rootNode")
        field.isAccessible = true
        return field.get(featureConfig) as org.spongepowered.configurate.CommentedConfigurationNode
    }

    private fun loadConfig() {
        val rootNode = getRootNode()
        this.config = mapper.load(rootNode) ?: UtilityConfig()
    }

    init {
        loadConfig()
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    override suspend fun reload() {
        featureConfig.reload()
        loadConfig()
    }

    fun reloadConfig(): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            featureConfig.reload()
            loadConfig()
            plugin.logger.info("¡Configuración de Utilidades recargada con éxito!")
        }, { taskScheduler.runAsync(it) })
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
        taskScheduler.runSyncLater({
            regionTaskScheduler.runAtLocation(event.to) {
                checkFlightSecurity(event.player)
            }
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
        regionTaskScheduler.runAtLocation(player.location) {
            player.allowFlight = false
            player.isFlying = false
            
            messageService.send(player, UtilityMessages.FLY_WORLD_LEFT)
        }
    }
}
