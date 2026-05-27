package com.github.berserkr2k.coreplugin

import com.github.berserkr2k.coreplugin.common.ThreadCoordinator
import com.github.berserkr2k.coreplugin.common.LegacyPlaceholderBridge
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import com.github.berserkr2k.coreplugin.infrastructure.database.DatabaseService
import com.github.berserkr2k.coreplugin.infrastructure.chat.ChatModule
import com.github.berserkr2k.coreplugin.infrastructure.anvil.AnvilModule
import com.github.berserkr2k.coreplugin.infrastructure.hologram.HologramService
import com.github.berserkr2k.coreplugin.infrastructure.hologram.HologramCommand
import com.github.berserkr2k.coreplugin.infrastructure.leaderboard.LeaderboardService
import com.github.berserkr2k.coreplugin.infrastructure.leaderboard.ArmorStandEditorListener
import com.github.berserkr2k.coreplugin.infrastructure.leaderboard.ArmorStandEditorCommand
import com.github.berserkr2k.coreplugin.infrastructure.leaderboard.EditorConfig
import com.github.berserkr2k.coreplugin.infrastructure.ui.InterfaceService
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Path
import java.util.logging.Logger
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.incendo.cloud.execution.ExecutionCoordinator
import org.bukkit.command.CommandSender
import org.bukkit.Bukkit

class CorePlugin(
    private val paperLogger: net.kyori.adventure.text.logger.slf4j.ComponentLogger,
    private val dataFolderPath: Path
) : JavaPlugin() {

    lateinit var threadCoordinator: ThreadCoordinator
        private set
    lateinit var configManager: ModularConfigManager
        private set
    lateinit var placeholderBridge: LegacyPlaceholderBridge
        private set
    lateinit var commandManager: LegacyPaperCommandManager<CommandSender>
        private set

    private var databaseService: DatabaseService? = null
    private var chatModule: ChatModule? = null
    private var anvilModule: AnvilModule? = null
    private var hologramService: HologramService? = null
    private var leaderboardService: LeaderboardService? = null
    private var interfaceService: InterfaceService? = null

    override fun onEnable() {
        threadCoordinator = ThreadCoordinator(this)
        placeholderBridge = LegacyPlaceholderBridge()
        configManager = ModularConfigManager(this, dataFolderPath)

        // Inicializar el orquestador de comandos nativos de Brigadier en Cloud v2
        commandManager = LegacyPaperCommandManager.createNative(
            this,
            ExecutionCoordinator.simpleCoordinator()
        )

        // Wrapper de Executor para ejecutar CompletableFutures de forma segura en el planificador asíncrono de Paper
        val asyncExecutor = java.util.concurrent.Executor { command ->
            Bukkit.getAsyncScheduler().runNow(this) { _ -> command.run() }
        }

        // Inicialización reactiva asíncrona de configuraciones y módulos
        configManager.loadModuleConfig("messages.conf", MessagesConfig::class.java, MessagesConfig())
           .thenAcceptAsync({ messagesConfig ->
                initializeModules(messagesConfig)
            }, asyncExecutor)
    }

    private fun initializeModules(messagesConfig: MessagesConfig) {
        // 1. Inicializar Base de Datos (Módulo SQL)
        databaseService = DatabaseService(this, configManager)
        
        // 2. Inicializar Módulo de Interfaces (Tablist, Bossbars) sin NMS
        interfaceService = InterfaceService(this, placeholderBridge, configManager)
        
        // 3. Inicializar Módulo de Chat Enriquecido (DeluxeChat Equivalence)
        chatModule = ChatModule(this, configManager, placeholderBridge)
        
        // 4. Inicializar Módulo de Yunques (Moderación y Permisos de Color)
        anvilModule = AnvilModule(this, configManager)
        
        // 5. Inicializar Módulo de Hologramas Interactivos Modernos
        val holoService = HologramService(this, configManager)
        hologramService = holoService
        HologramCommand(this, commandManager, holoService)
        
        // 6. Inicializar Módulo de Podios Físicos e Editor de ArmorStands
        leaderboardService = LeaderboardService(this, configManager, messagesConfig)
        
        // Cargar configuración de editor y registrar comandos/listeners
        configManager.loadModuleConfig("editor.conf", EditorConfig::class.java, EditorConfig())
            .thenAccept { editorConfig ->
                ArmorStandEditorCommand(this, commandManager, editorConfig)
            }

        // Registrar listener del editor de ArmorStands
        server.pluginManager.registerEvents(
            ArmorStandEditorListener(this, leaderboardService!!, messagesConfig), 
            this
        )
        
        paperLogger.info("¡Todos los módulos del plugin Core se han cargado de forma aislada y asíncrona!")
    }

    override fun onDisable() {
        hologramService?.shutdown()
        leaderboardService?.shutdown()
        databaseService?.shutdown()
        configManager.shutdown()
    }
}