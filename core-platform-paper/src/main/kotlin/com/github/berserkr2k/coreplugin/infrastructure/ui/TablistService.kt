package com.github.berserkr2k.coreplugin.infrastructure.ui

import com.github.berserkr2k.coreplugin.common.LegacyPlaceholderBridge
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.util.NamingSchemes
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.bossbar.BossBar
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.UUID
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler
import com.github.berserkr2k.coreplugin.api.core.scheduler.Task

class TablistService(
    private val plugin: Plugin,
    private val papiBridge: LegacyPlaceholderBridge,
    private val registry: ServiceRegistry
) : com.github.berserkr2k.coreplugin.api.core.lifecycle.Reloadable {
    private val taskScheduler = registry.get(TaskScheduler::class.java)
    private val activeBossBars = ConcurrentHashMap<UUID, BossBar>()
    private var tablistConfig = TablistConfig()
    private var updateTask: Task? = null

    init {
        this.tablistConfig = loadConfig()
        startTablistScheduler()
    }

    private fun loadConfig(): TablistConfig {
        val file = plugin.dataFolder.toPath().resolve("core/tablist.conf")
        if (java.nio.file.Files.notExists(file)) {
            java.nio.file.Files.createDirectories(file.parent)
            java.nio.file.Files.createFile(file)
        }
        val mapperFactory = ObjectMapper.factoryBuilder()
            .defaultNamingScheme(NamingSchemes.PASSTHROUGH)
            .build()
        val loader = HoconConfigurationLoader.builder()
            .path(file)
            .defaultOptions { options ->
                options.serializers { builder ->
                    builder.registerAnnotatedObjects(mapperFactory)
                }
            }
            .build()
        val root = loader.load()
        val mapper = mapperFactory.get(TablistConfig::class.java)
        return if (root.empty()) {
            val defaultInstance = TablistConfig()
            mapper.save(defaultInstance, root)
            loader.save(root)
            defaultInstance
        } else {
            mapper.load(root) ?: TablistConfig()
        }
    }

    override suspend fun reload() {
        try {
            this.tablistConfig = loadConfig()
            startTablistScheduler()
        } catch (e: Exception) {
            plugin.logger.severe("Error al recargar tablist.conf: ${e.message}")
        }
    }

    private fun startTablistScheduler() {
        updateTask?.cancel()
        // Programación asíncrona robusta exenta de hilos bloqueantes (Paper/Folia compatible)
        updateTask = taskScheduler.runAsyncTimer({
            for (player in Bukkit.getOnlinePlayers()) {
                val group = resolvePlayerPriorityGroup(player)
                val priority = group?.priority ?: 100
                player.playerListOrder = priority
 
                // Aplicar el color permanente en la tablist
                val color = group?.color ?: "<white>"
                val coloredName = com.github.berserkr2k.coreplugin.common.ColorUtility.parse("$color${player.name}")
                player.playerListName(coloredName)
 
                val header = papiBridge.parseLegacyStringSecurely(player, tablistConfig.tablistHeader)
                val footer = papiBridge.parseLegacyStringSecurely(player, tablistConfig.tablistFooter)
 
                player.sendPlayerListHeaderAndFooter(header, footer)
            }
        }, 0L, tablistConfig.updateIntervalTicks)
    }

    private fun resolvePlayerPriorityGroup(player: Player): TablistConfig.TablistPriorityGroup? {
        // Recorrido dinámico por permisos inyectados desde la configuración (Zero Hardcoding)
        return tablistConfig.tablistPriorities.asSequence()
           .filter { player.hasPermission(it.permission) }
           .minByOrNull { it.priority }
    }

    /**
     * Muestra una barra de jefe (BossBar) a un jugador utilizando la API nativa de Adventure.
     */
    fun displayBossBar(player: Player, titleTemplate: String, progress: Float, color: BossBar.Color, overlay: BossBar.Overlay) {
        val resolvedTitle = papiBridge.parseLegacyStringSecurely(player, titleTemplate)
        val progressClamped = progress.coerceIn(0.0f, 1.0f)
        val bossBar = BossBar.bossBar(resolvedTitle, progressClamped, color, overlay)
        
        activeBossBars[player.uniqueId] = bossBar
        player.showBossBar(bossBar)
    }

    /**
     * Actualiza el progreso de una barra de jefe activa de forma segura.
     */
    fun updateBossBarProgress(player: Player, newProgress: Float) {
        val bar = activeBossBars[player.uniqueId]
        if (bar != null) {
            bar.progress(newProgress.coerceIn(0.0f, 1.0f))
        }
    }

    /**
     * Oculta y remueve la barra de jefe de un jugador.
     */
    fun removeBossBar(player: Player) {
        val bar = activeBossBars.remove(player.uniqueId)
        if (bar != null) {
            player.hideBossBar(bar)
        }
    }
}
