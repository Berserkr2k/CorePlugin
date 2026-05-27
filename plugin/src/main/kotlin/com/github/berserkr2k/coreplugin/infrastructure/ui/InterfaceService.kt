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

class InterfaceService(
    private val plugin: Plugin,
    private val papiBridge: LegacyPlaceholderBridge,
    private val configManager: ModularConfigManager
) {
    private val activeBossBars = ConcurrentHashMap<UUID, BossBar>()
    private var uiConfig = UiConfig()

    init {
        // Carga dinámica HOCON mapeada directamente a clases de Kotlin
        configManager.loadModuleConfig("ui.conf", UiConfig::class.java, UiConfig())
           .thenAccept { loadedConfig ->
                this.uiConfig = loadedConfig
                startTablistScheduler()
            }
    }

    private fun startTablistScheduler() {
        // Programación asíncrona robusta exenta de hilos bloqueantes (Paper/Folia compatible)
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _ ->
            for (player in Bukkit.getOnlinePlayers()) {
                val priorityOrder = resolvePlayerPriority(player)
                player.playerListOrder = priorityOrder // API nativa de Paper

                val header = papiBridge.parseLegacyStringSecurely(player, uiConfig.tablistHeader)
                val footer = papiBridge.parseLegacyStringSecurely(player, uiConfig.tablistFooter)

                player.sendPlayerListHeaderAndFooter(header, footer)
            }
        }, 0, 1, TimeUnit.SECONDS)
    }

    private fun resolvePlayerPriority(player: Player): Int {
        // Recorrido dinámico por permisos inyectados desde la configuración (Zero Hardcoding)
        return uiConfig.tablistPriorities.asSequence()
           .filter { player.hasPermission(it.permission) }
           .minByOrNull { it.priority }?.priority ?: 100
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
