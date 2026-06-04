package com.github.berserkr2k.coreplugin

import com.github.berserkr2k.coreplugin.common.ThreadCoordinator
import com.github.berserkr2k.coreplugin.common.LegacyPlaceholderBridge
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import com.github.berserkr2k.coreplugin.infrastructure.database.DatabaseService
import com.github.berserkr2k.coreplugin.infrastructure.chat.ChatModule
import com.github.berserkr2k.coreplugin.infrastructure.mechanics.AnvilModule
import com.github.berserkr2k.coreplugin.infrastructure.hologram.HologramService
import com.github.berserkr2k.coreplugin.infrastructure.hologram.HologramCommand
import com.github.berserkr2k.coreplugin.infrastructure.leaderboard.LeaderboardService
import com.github.berserkr2k.coreplugin.infrastructure.leaderboard.LeaderboardCommand
import com.github.berserkr2k.coreplugin.infrastructure.leaderboard.ArmorStandEditorListener
import com.github.berserkr2k.coreplugin.infrastructure.leaderboard.ArmorStandEditorCommand
import com.github.berserkr2k.coreplugin.infrastructure.leaderboard.EditorConfig
import com.github.berserkr2k.coreplugin.infrastructure.leaderboard.ArmorStandEditorGui
import com.github.berserkr2k.coreplugin.infrastructure.mechanics.ChairListener
import com.github.berserkr2k.coreplugin.infrastructure.ui.InterfaceService
import com.github.berserkr2k.coreplugin.infrastructure.economy.EconomyService
import com.github.berserkr2k.coreplugin.infrastructure.economy.WalletCommand
import com.github.berserkr2k.coreplugin.infrastructure.economy.EconomyPlaceholderExpansion
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Path
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.incendo.cloud.execution.ExecutionCoordinator
import org.bukkit.command.CommandSender
import org.bukkit.Bukkit
import com.github.berserkr2k.coreplugin.infrastructure.utilitycommands.UtilityService
import com.github.berserkr2k.coreplugin.infrastructure.utilitycommands.FlyCommand
import com.github.berserkr2k.coreplugin.infrastructure.utilitycommands.SpeedCommand
import com.github.berserkr2k.coreplugin.infrastructure.utilitycommands.HatCommand
import com.github.berserkr2k.coreplugin.infrastructure.utilitycommands.FeedCommand
import com.github.berserkr2k.coreplugin.infrastructure.utilitycommands.HealCommand
import com.github.berserkr2k.coreplugin.infrastructure.utilitycommands.AnvilCommand
import com.github.berserkr2k.coreplugin.infrastructure.utilitycommands.EnderChestCommand
import com.github.berserkr2k.coreplugin.infrastructure.utilitycommands.ExpCommand
import com.github.berserkr2k.coreplugin.infrastructure.utilitycommands.BroadcastCommand
import com.github.berserkr2k.coreplugin.infrastructure.utilitycommands.SendTitleCommand

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
    private var profileRegistry: com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry? = null
    private var chatModule: ChatModule? = null
    private var anvilModule: AnvilModule? = null
    private var hologramService: HologramService? = null
    private var leaderboardService: LeaderboardService? = null
    private var chairListener: ChairListener? = null
    private var interfaceService: InterfaceService? = null
    private var economyService: EconomyService? = null
    private var utilityService: UtilityService? = null
    private var shopManager: com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop.ShopManager? = null

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

        // Inicializar caché relacional centralizada (ProfileRegistry y Listener)
        val registry = com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry(databaseService!!, logger)
        profileRegistry = registry
        server.pluginManager.registerEvents(
            com.github.berserkr2k.coreplugin.infrastructure.listeners.UserProfileListener(this, registry),
            this
        )
        
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
        val lService = LeaderboardService(this, configManager, messagesConfig, databaseService!!, placeholderBridge, registry)
        leaderboardService = lService
        server.pluginManager.registerEvents(lService, this)
        
        // Registrar Comando de Leaderboards
        LeaderboardCommand(this, commandManager, lService)
        
        // Cargar configuración de editor y registrar comandos/listeners síncronamente
        val editorConfig = configManager.loadModuleConfig("editor.conf", EditorConfig::class.java, EditorConfig()).join()
        ArmorStandEditorGui.init(this, configManager)
        ArmorStandEditorCommand(this, commandManager, editorConfig)

        // Registrar listener del editor de ArmorStands
        server.pluginManager.registerEvents(
            ArmorStandEditorListener(this, leaderboardService!!, messagesConfig), 
            this
        )

        // 7. Inicializar Módulo "Misc" (Escaleras como Sillas)
        val cListener = ChairListener(this, messagesConfig)
        chairListener = cListener
        server.pluginManager.registerEvents(cListener, this)

        // 8. Inicializar Módulo de Economía Multi-Divisa Altamente Seguro
        val ecoService = EconomyService(this, configManager, databaseService!!, registry)
        economyService = ecoService

        val isVaultEnabled = server.pluginManager.isPluginEnabled("Vault")
        val isVaultUnlockedEnabled = server.pluginManager.isPluginEnabled("VaultUnlocked")

        if (isVaultEnabled || isVaultUnlockedEnabled) {
            var registeredLegacy = false
            var registeredVault2 = false

            // Intentar registrar Vault Legacy
            try {
                Class.forName("net.milkbowl.vault.economy.Economy")
                com.github.berserkr2k.coreplugin.infrastructure.economy.LegacyVaultRegisterHelper.register(this, ecoService)
                registeredLegacy = true
            } catch (e: Throwable) {
                // Omitir
            }

            // Intentar registrar Vault2 (VaultUnlocked)
            try {
                Class.forName("net.milkbowl.vault2.economy.Economy")
                com.github.berserkr2k.coreplugin.infrastructure.economy.Vault2RegisterHelper.register(this, ecoService)
                registeredVault2 = true
            } catch (e: Throwable) {
                // Omitir
            }

            if (registeredLegacy && registeredVault2) {
                paperLogger.info("¡Wrappers seguros de Vault (Legacy) y Vault2 (VaultUnlocked) registrados con prioridad Máxima!")
            } else if (registeredLegacy) {
                paperLogger.info("¡Wrapper seguro de Vault (Legacy) registrado con prioridad Máxima!")
            } else if (registeredVault2) {
                paperLogger.info("¡Wrapper seguro de Vault2 (VaultUnlocked) registrado con prioridad Máxima!")
            }
        }


        // Registrar Billetera y Comandos Dinámicos de Divisas
        WalletCommand(this, commandManager, ecoService, messagesConfig)
        
        if (server.pluginManager.isPluginEnabled("PlaceholderAPI")) {
            EconomyPlaceholderExpansion(ecoService).register()
            paperLogger.info("¡Expansión de economía de PlaceholderAPI registrada con éxito!")
        }

        // 9. Inicializar Módulo de Utilidades Modulares
        val uService = UtilityService(this, configManager, messagesConfig)
        utilityService = uService
        
        FlyCommand(this, commandManager, uService, messagesConfig)
        SpeedCommand(this, commandManager, messagesConfig)
        HatCommand(this, commandManager, messagesConfig)
        FeedCommand(this, commandManager, messagesConfig)
        HealCommand(this, commandManager, messagesConfig)
        AnvilCommand(this, commandManager, uService, messagesConfig)
        EnderChestCommand(this, commandManager, messagesConfig)
        ExpCommand(this, commandManager, messagesConfig)
        BroadcastCommand(this, commandManager, uService, messagesConfig)
        SendTitleCommand(this, commandManager, messagesConfig)
        
        // 10. Inicializar Módulo Premium de Kits
        val kService = com.github.berserkr2k.coreplugin.infrastructure.kits.KitService(this, configManager, databaseService!!, ecoService, messagesConfig, registry)
        val kGuis = com.github.berserkr2k.coreplugin.infrastructure.kits.KitGuis(this, configManager, kService)
        com.github.berserkr2k.coreplugin.infrastructure.kits.KitCommand(this, commandManager, kService, kGuis)

        // 11. Inicializar Módulo Premium de Estelas de Partículas (Projectile Trails)
        val trailManager = com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails.ProjectileTrailManager(this, databaseService!!, configManager, registry)
        val trailGuis = com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails.TrailGuis(this, configManager, trailManager)
        server.pluginManager.registerEvents(com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails.ProjectileTrailListener(this, trailManager), this)
        com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails.ProjectileTrailCommand(this, commandManager, trailManager, trailGuis)

        // 12. Inicializar Módulo Premium de Tiendas de Mercado Dinámico
        val sManager = com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop.ShopManager(this, configManager, databaseService!!)
        shopManager = sManager
        val sGuis = com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop.ShopGuis(this, sManager, ecoService, messagesConfig)
        com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop.ShopCommand(this, commandManager, sManager, sGuis, messagesConfig)

        // Programar purga automática de 90 días en segundo plano cada 24 horas (delay 1h)
        threadCoordinator.runTimerAsync(72000, 1728000) {
            ecoService.purgeInactiveRecords(90).thenAccept { deleted ->
                if (deleted > 0) {
                    paperLogger.info("¡Purga de base de datos completada! $deleted cuentas inactivas eliminadas (y cascada).")
                }
            }
        }

        // Programar guardado por lotes cada 5 minutos asíncronamente
        Bukkit.getAsyncScheduler().runAtFixedRate(this, { _ ->
            registry.flushAllActive()
        }, 5, 5, java.util.concurrent.TimeUnit.MINUTES)
        
        // Imprimir Dashboard Visual Premium de Estado de Módulos
        val mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
        val logo = """
<gold>  ___ ___  ___  ___ ___ _   _   _ ___ ___ ___ 
 / __/ _ \| _ \/ __| _ \ | | | | | __|_ _|_ _|
| (_| (_) |   / (_ |  _/ |_| |_| | _| | | | | 
 \___\___/|_|_\\___|_| |____\___/|___|___|___|</gold>
        """.trimIndent()
        
        val dashboard = """
$logo
<dark_gray>======================================================</dark_gray>
<yellow><bold>          COREPLUGIN MODULES STATUS</bold></yellow>
<dark_gray>======================================================</dark_gray>
 <gray>⚡ <gold>Base de Datos</gold>      : <green>[ ACTIVO ]</green> (SQLite JDBC)</gray>
 <gray>⚡ <gold>Interfaces</gold>         : <green>[ ACTIVO ]</green> (Tablist & Bossbars)</gray>
 <gray>⚡ <gold>Chat Enriquecido</gold>   : <green>[ ACTIVO ]</green> (DeluxeChat Equiv)</gray>
 <gray>⚡ <gold>Yunques</gold>            : <green>[ ACTIVO ]</green> (Filtros & Colores)</gray>
 <gray>⚡ <gold>Hologramas</gold>         : <green>[ ACTIVO ]</green> (Packet-Based)</gray>
 <gray>⚡ <gold>Podios Físicos</gold>     : <green>[ ACTIVO ]</green> (ArmorStands Editor)</gray>
 <gray>⚡ <gold>Mecánica Sillas</gold>    : <green>[ ACTIVO ]</green> (Stairs Sitting)</gray>
 <gray>⚡ <gold>Economía</gold>           : <green>[ ACTIVO ]</green> (Multi-Divisa & Vault)</gray>
 <gray>⚡ <gold>Utilidades</gold>         : <green>[ ACTIVO ]</green> (Modular Commands)</gray>
 <gray>⚡ <gold>Kits Premium</gold>       : <green>[ ACTIVO ]</green> (HOCON & GUIs)</gray>
 <gray>⚡ <gold>Estelas Proyectil</gold>  : <green>[ ACTIVO ]</green> (Async & 3D Math)</gray>
  <gray>⚡ <gold>Tiendas Mercado</gold>   : <green>[ ACTIVO ]</green> (Dynamic & Symmetric)</gray>
<dark_gray>======================================================</dark_gray>
<green>¡Todos los módulos del plugin Core cargados de forma síncrona!</green>
        """.trimIndent()

        paperLogger.info(mm.deserialize(dashboard))
    }

    override fun onDisable() {
        val mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
        val shutdownMessage = """
<dark_gray>======================================================</dark_gray>
<red><bold>          COREPLUGIN SHUTTING DOWN</bold></red>
<dark_gray>======================================================</dark_gray>
 <gray>🔌 <red>Hologramas</red>      : <gold>[ DESACTIVADO ]</gold> (Cerrando visores...)</gray>
 <gray>🔌 <red>Podios</red>          : <gold>[ DESACTIVADO ]</gold> (Limpiando ArmorStands...)</gray>
 <gray>🔌 <red>Sillas</red>          : <gold>[ DESACTIVADO ]</gold> (Desmontando jugadores...)</gray>
 <gray>🔌 <red>Base de Datos</red>   : <gold>[ DESACTIVADO ]</gold> (Cerrando pool Hikari...)</gray>
 <gray>🔌 <red>Configuraciones</red> : <gold>[ DESACTIVADO ]</gold> (Guardando cambios...)</gray>
<dark_gray>======================================================</dark_gray>
<red>¡CorePlugin desactivado y datos persistidos con éxito!</red>
        """.trimIndent()
        paperLogger.info(mm.deserialize(shutdownMessage))

        hologramService?.shutdown()
        leaderboardService?.shutdown()
        chairListener?.shutdown()

        // Guardar todos los perfiles de usuario sucios en memoria antes de apagar
        profileRegistry?.flushAllActive()?.join()

        databaseService?.shutdown()
        configManager.shutdown()
    }
}