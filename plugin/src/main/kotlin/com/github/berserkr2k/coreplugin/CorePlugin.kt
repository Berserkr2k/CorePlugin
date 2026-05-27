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
import com.github.berserkr2k.coreplugin.infrastructure.leaderboard.LeaderboardCommand
import com.github.berserkr2k.coreplugin.infrastructure.leaderboard.ArmorStandEditorListener
import com.github.berserkr2k.coreplugin.infrastructure.leaderboard.ArmorStandEditorCommand
import com.github.berserkr2k.coreplugin.infrastructure.leaderboard.EditorConfig
import com.github.berserkr2k.coreplugin.infrastructure.misc.ChairListener
import com.github.berserkr2k.coreplugin.infrastructure.ui.InterfaceService
import com.github.berserkr2k.coreplugin.infrastructure.economy.EconomyService
import com.github.berserkr2k.coreplugin.infrastructure.economy.EconomyListener
import com.github.berserkr2k.coreplugin.infrastructure.economy.VaultEconomyHook
import com.github.berserkr2k.coreplugin.infrastructure.economy.Vault2EconomyHook
import com.github.berserkr2k.coreplugin.infrastructure.economy.WalletCommand
import com.github.berserkr2k.coreplugin.infrastructure.economy.EconomyPlaceholderExpansion
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Path
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
    private var chairListener: ChairListener? = null
    private var interfaceService: InterfaceService? = null
    private var economyService: EconomyService? = null

    override fun onEnable() {
        threadCoordinator = ThreadCoordinator(this)
        placeholderBridge = LegacyPlaceholderBridge()
        configManager = ModularConfigManager(this, dataFolderPath)

        // Inicializar el MenuManager para prevención de robos y duplicados
        com.github.berserkr2k.coreplugin.common.gui.MenuManager.init(this)

        // Inicializar el orquestador de comandos nativos de Brigadier en Cloud v2
        commandManager = LegacyPaperCommandManager.createNative(
            this,
            ExecutionCoordinator.simpleCoordinator()
        )

        // Wrapper de Executor para ejecutar CompletableFutures de forma segura en el planificador asíncrono de Paper
        try {
            val messagesConfig = configManager.loadModuleConfig("messages.conf", MessagesConfig::class.java, MessagesConfig()).join()
            initializeModules(messagesConfig)
        } catch (e: Exception) {
            paperLogger.error("Error crítico al inicializar el CorePlugin: ${e.message}", e)
            server.pluginManager.disablePlugin(this)
        }
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
        val holoService = HologramService(this, configManager, placeholderBridge)
        hologramService = holoService
        HologramCommand(this, commandManager, holoService)
        
        // 6. Inicializar Módulo de Podios Físicos e Editor de ArmorStands
        val lService = LeaderboardService(this, configManager, messagesConfig, databaseService!!, placeholderBridge)
        leaderboardService = lService
        server.pluginManager.registerEvents(lService, this)
        
        // Registrar Comando de Leaderboards
        LeaderboardCommand(this, commandManager, lService)
        
        // Cargar configuración de editor y registrar comandos/listeners síncronamente
        val editorConfig = configManager.loadModuleConfig("editor.conf", EditorConfig::class.java, EditorConfig()).join()
        ArmorStandEditorCommand(this, commandManager, editorConfig)

        // Registrar listener del editor de ArmorStands
        server.pluginManager.registerEvents(
            ArmorStandEditorListener(this, leaderboardService!!, messagesConfig), 
            this
        )

        // 7. Inicializar Módulo "Misc" (Escaleras como Sillas)
        val cListener = ChairListener(this)
        chairListener = cListener
        server.pluginManager.registerEvents(cListener, this)

        // 8. Inicializar Módulo de Economía Multi-Divisa Altamente Seguro
        val ecoService = EconomyService(this, configManager, databaseService!!)
        economyService = ecoService
        
        server.pluginManager.registerEvents(EconomyListener(this, ecoService), this)

        if (server.pluginManager.isPluginEnabled("Vault")) {
            server.servicesManager.register(
                net.milkbowl.vault.economy.Economy::class.java,
                VaultEconomyHook(ecoService),
                this,
                org.bukkit.plugin.ServicePriority.Highest
            )
            server.servicesManager.register(
                net.milkbowl.vault2.economy.Economy::class.java,
                Vault2EconomyHook(ecoService),
                this,
                org.bukkit.plugin.ServicePriority.Highest
            )
            paperLogger.info("¡Wrappers seguros de Vault (Legacy) y Vault2 (VaultUnlocked) registrados con prioridad Máxima!")
        }

        // Registrar Billetera y Comandos Dinámicos de Divisas
        WalletCommand(this, commandManager, ecoService, messagesConfig)
        
        if (server.pluginManager.isPluginEnabled("PlaceholderAPI")) {
            EconomyPlaceholderExpansion(ecoService).register()
            paperLogger.info("¡Expansión de economía de PlaceholderAPI registrada con éxito!")
        }

        // Programar purga automática de 90 días en segundo plano cada 24 horas (delay 1h)
        threadCoordinator.runTimerAsync(72000, 1728000) {
            ecoService.purgeInactiveRecords(90).thenAccept { deleted ->
                if (deleted > 0) {
                    paperLogger.info("¡Purga de base de datos completada! $deleted cuentas inactivas eliminadas.")
                }
            }
        }
        
        paperLogger.info("¡Todos los módulos del plugin Core se han cargado de forma síncrona y robusta!")
    }

    override fun onDisable() {
        hologramService?.shutdown()
        leaderboardService?.shutdown()
        chairListener?.shutdown()
        databaseService?.shutdown()
        configManager.shutdown()
    }
}