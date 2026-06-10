package com.github.berserkr2k.coreplugin.infrastructure.scoreboard

import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.scheduler.TaskScheduler
import com.github.berserkr2k.coreplugin.api.scheduler.RegionTaskScheduler
import com.github.berserkr2k.coreplugin.api.scheduler.Task
import com.github.berserkr2k.coreplugin.api.state.PlayerStateService
import com.github.berserkr2k.coreplugin.api.state.StateContainer
import com.github.berserkr2k.coreplugin.api.state.StateContainerType
import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.common.LegacyPlaceholderBridge
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import io.papermc.paper.scoreboard.numbers.NumberFormat
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Criteria
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture

class ScoreboardStateContainer(
    var visible: Boolean = true
) : StateContainer

val SCOREBOARD_STATE_TYPE = StateContainerType { ScoreboardStateContainer() }

class ScoreboardService(
    private val plugin: Plugin,
    private val configManager: ModularConfigManager,
    private val placeholderBridge: LegacyPlaceholderBridge,
    private val serviceRegistry: ServiceRegistry
) : Listener {

    private val taskScheduler = serviceRegistry.get(TaskScheduler::class.java)
    private val regionTaskScheduler = serviceRegistry.get(RegionTaskScheduler::class.java)
    private val stateService = serviceRegistry.get(PlayerStateService::class.java)

    lateinit var config: ScoreboardModuleConfig
        private set

    private val playerLineCounts = ConcurrentHashMap<UUID, Int>()
    private val titleFrameIndexes = ConcurrentHashMap<UUID, Int>()
    private var updateTask: Task? = null

    init {
        reloadConfig().join()
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    fun reloadConfig(): CompletableFuture<Void> {
        return configManager.loadModuleConfig("scoreboard.conf", ScoreboardModuleConfig::class.java, ScoreboardModuleConfig())
            .thenAccept { loadedConfig ->
                this.config = loadedConfig
                if (loadedConfig.enabled) {
                    startUpdateTask()
                    plugin.logger.info("¡Configuración de Scoreboard cargada y actualizada con éxito!")
                } else {
                    stopUpdateTask()
                    plugin.logger.info("Módulo de Scoreboard desactivado en configuración.")
                }
            }
    }

    private fun startUpdateTask() {
        updateTask?.cancel()
        val interval = config.updateIntervalTicks
        updateTask = taskScheduler.runSyncTimer({
            if (!config.enabled) return@runSyncTimer
            for (player in Bukkit.getOnlinePlayers()) {
                regionTaskScheduler.runAtEntity(player) {
                    updatePlayerScoreboard(player)
                }
            }
        }, interval, interval)
    }

    private fun stopUpdateTask() {
        updateTask?.cancel()
        updateTask = null
        for (player in Bukkit.getOnlinePlayers()) {
            try {
                regionTaskScheduler.runAtEntity(player) {
                    player.scoreboard = Bukkit.getScoreboardManager().mainScoreboard
                }
            } catch (e: org.bukkit.plugin.IllegalPluginAccessException) {
                // Si el plugin ya está desactivado (ej. durante onDisable), no podemos registrar tareas.
                // Intentamos revertir directamente en el hilo actual de forma safe, ignorando fallas de hilo en Folia.
                try {
                    player.scoreboard = Bukkit.getScoreboardManager().mainScoreboard
                } catch (t: Throwable) {
                    // Ignorar
                }
            }
        }
        playerLineCounts.clear()
        titleFrameIndexes.clear()
    }

    fun getScoreboardState(uuid: UUID): ScoreboardStateContainer {
        return stateService.getContainer(uuid, SCOREBOARD_STATE_TYPE)
    }

    private fun getLayoutForPlayer(player: Player): ScoreboardLayoutConfig {
        val worldName = player.world.name
        return config.worlds[worldName] ?: config.defaultLayout
    }

    private fun getLineEntryId(index: Int): String {
        val hexChar = index.toString(16)
        return "§$hexChar§r"
    }

    private fun setupScoreboard(player: Player) {
        val scoreboard = Bukkit.getScoreboardManager().newScoreboard
        val layout = getLayoutForPlayer(player)

        val objective = scoreboard.registerNewObjective(
            "sidebar",
            Criteria.DUMMY,
            ColorUtility.parse(layout.title.firstOrNull() ?: "")
        )
        objective.displaySlot = DisplaySlot.SIDEBAR
        objective.numberFormat(NumberFormat.blank())

        player.scoreboard = scoreboard
        updatePlayerScoreboard(player)
    }

    private fun getCharWidth(c: Char, isBold: Boolean): Int {
        val baseWidth = when (c) {
            'i', '!', '|', 'l', '.', ':', ';', ',', '\'' -> 2
            'I', 't', '[', ']', ' ', '(', ')', '*', '{', '}' -> 4
            'f', 'k', '"', '<', '>', 'I' -> 5
            '@', '~' -> 7
            else -> 6
        }
        return if (isBold) baseWidth + 1 else baseWidth
    }

    private fun getComponentPixelWidth(component: net.kyori.adventure.text.Component, parentBold: Boolean = false): Int {
        var width = 0
        val isBold = when (component.decoration(net.kyori.adventure.text.format.TextDecoration.BOLD)) {
            net.kyori.adventure.text.format.TextDecoration.State.TRUE -> true
            net.kyori.adventure.text.format.TextDecoration.State.FALSE -> false
            else -> parentBold
        }

        if (component is net.kyori.adventure.text.TextComponent) {
            for (char in component.content()) {
                width += getCharWidth(char, isBold)
            }
        }

        for (child in component.children()) {
            width += getComponentPixelWidth(child, isBold)
        }

        return width
    }

    private fun updatePlayerScoreboard(player: Player) {
        if (!config.enabled) return
        val state = getScoreboardState(player.uniqueId)
        if (!state.visible) {
            if (player.scoreboard != Bukkit.getScoreboardManager().mainScoreboard) {
                player.scoreboard = Bukkit.getScoreboardManager().mainScoreboard
            }
            return
        }

        var scoreboard = player.scoreboard
        if (scoreboard == Bukkit.getScoreboardManager().mainScoreboard) {
            setupScoreboard(player)
            return
        }

        val objective = scoreboard.getObjective("sidebar") ?: return
        val layout = getLayoutForPlayer(player)

        // 1. Actualizar Título Animado
        var titleComponent: net.kyori.adventure.text.Component = net.kyori.adventure.text.Component.empty()
        val frameIndex = titleFrameIndexes.getOrDefault(player.uniqueId, 0)
        val frames = layout.title
        if (frames.isNotEmpty()) {
            val currentFrame = frames[frameIndex % frames.size]
            val resolvedTitle = placeholderBridge.parsePlaceholder(player, currentFrame)
            titleComponent = ColorUtility.parse(resolvedTitle)
            objective.displayName(titleComponent)
            titleFrameIndexes[player.uniqueId] = frameIndex + 1
        }

        // 2. Resolver Líneas Normales con Placeholders
        val normalLines = layout.lines
        val resolvedNormalComponents = normalLines.map { lineText ->
            val resolvedLine = placeholderBridge.parsePlaceholder(player, lineText)
            ColorUtility.parse(resolvedLine)
        }

        // 3. Resolver y Centrar Footer
        val footerLines = layout.footer
        val resolvedFooterComponents = footerLines.map { lineText ->
            val resolvedLine = placeholderBridge.parsePlaceholder(player, lineText)
            var footerComp: net.kyori.adventure.text.Component = ColorUtility.parse(resolvedLine)

            if (layout.centerFooter) {
                val titleWidth = getComponentPixelWidth(titleComponent)
                val maxNormalWidth = resolvedNormalComponents.maxOfOrNull { getComponentPixelWidth(it) } ?: 0
                val maxWidth = maxOf(titleWidth, maxNormalWidth)

                val footerWidth = getComponentPixelWidth(footerComp)
                if (maxWidth > footerWidth) {
                    val paddingPixels = (maxWidth - footerWidth) / 2
                    var currentPadding = 0
                    val sb = StringBuilder()
                    while (currentPadding < paddingPixels) {
                        sb.append(" ")
                        currentPadding += 4
                    }
                    footerComp = net.kyori.adventure.text.Component.text(sb.toString()).append(footerComp)
                }
            }
            footerComp
        }

        // 4. Combinar y Establecer Líneas en el Scoreboard
        val combinedComponents = resolvedNormalComponents + resolvedFooterComponents
        val maxLines = minOf(combinedComponents.size, 15)

        for (i in 0 until maxLines) {
            val lineComponent = combinedComponents[i]
            val entryId = getLineEntryId(i)
            val score = objective.getScore(entryId)
            score.score = maxLines - i
            score.customName(lineComponent)
        }

        // 5. Remover líneas que sobraron de la anterior visualización
        val oldLineCount = playerLineCounts.getOrDefault(player.uniqueId, 0)
        if (oldLineCount > maxLines) {
            for (i in maxLines until oldLineCount) {
                val entryId = getLineEntryId(i)
                scoreboard.resetScores(entryId)
            }
        }
        playerLineCounts[player.uniqueId] = maxLines
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!config.enabled) return
        val player = event.player
        regionTaskScheduler.runAtEntity(player) {
            setupScoreboard(player)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        playerLineCounts.remove(uuid)
        titleFrameIndexes.remove(uuid)
    }

    @EventHandler
    fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
        if (!config.enabled) return
        val player = event.player
        regionTaskScheduler.runAtEntity(player) {
            // Re-instanciar o re-popular el scoreboard del jugador para el nuevo layout del mundo
            setupScoreboard(player)
        }
    }

    fun shutdown() {
        stopUpdateTask()
    }
}
