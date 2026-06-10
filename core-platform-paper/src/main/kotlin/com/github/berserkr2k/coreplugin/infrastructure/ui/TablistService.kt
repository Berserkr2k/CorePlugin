package com.github.berserkr2k.coreplugin.infrastructure.ui

import com.github.berserkr2k.coreplugin.common.LegacyPlaceholderBridge
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.bossbar.BossBar
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.UUID
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.scheduler.TaskScheduler
import com.github.berserkr2k.coreplugin.api.scheduler.Task

class TablistService(
    private val plugin: Plugin,
    private val papiBridge: LegacyPlaceholderBridge,
    private val configManager: ModularConfigManager,
    private val registry: ServiceRegistry
) {
    private val taskScheduler = registry.get(TaskScheduler::class.java)
    private val activeBossBars = ConcurrentHashMap<UUID, BossBar>()
    private var tablistConfig = TablistConfig()

    init {
        // Carga dinámica HOCON mapeada directamente a clases de Kotlin
        configManager.loadModuleConfig("tablist.conf", TablistConfig::class.java, TablistConfig())
           .thenAccept { loadedConfig ->
                this.tablistConfig = loadedConfig
                startTablistScheduler()
            }
    }

    private fun startTablistScheduler() {
        // Programación asíncrona robusta exenta de hilos bloqueantes (Paper/Folia compatible)
        taskScheduler.runAsyncTimer({
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
        }, 0L, 20L)
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
