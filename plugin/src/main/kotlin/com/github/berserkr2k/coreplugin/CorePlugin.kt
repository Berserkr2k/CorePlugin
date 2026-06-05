package com.github.berserkr2k.coreplugin

import com.github.berserkr2k.coreplugin.common.ThreadCoordinator
import com.github.berserkr2k.coreplugin.common.LegacyPlaceholderBridge
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import com.github.berserkr2k.coreplugin.infrastructure.database.DatabaseService
import com.github.berserkr2k.coreplugin.infrastructure.chat.ChatModule
import com.github.berserkr2k.coreplugin.infrastructure.chat.PrivateMessageCommand
import com.github.berserkr2k.coreplugin.infrastructure.chat.ColorCommand
import com.github.berserkr2k.coreplugin.infrastructure.warps.WarpService
import com.github.berserkr2k.coreplugin.infrastructure.warps.WarpCommand
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
import com.github.berserkr2k.coreplugin.infrastructure.ui.TablistService
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
    private var tablistService: TablistService? = null
    private var economyService: EconomyService? = null
    private var utilityService: UtilityService? = null
    private var shopManager: com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop.ShopManager? = null
    private var warpService: WarpService? = null

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
        val mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
        val statuses = mutableMapOf<String, String>()
        val errors = mutableListOf<String>()

        fun initModule(name: String, block: () -> Unit) {
            try {
                block()
                statuses[name] = "<green>[ ACTIVO ]</green>"
            } catch (t: Throwable) {
                statuses[name] = "<red>[ ERROR ]</red>"
                var cause = t
                while (cause.cause != null && cause.cause != cause) {
                    cause = cause.cause!!
                }
                errors.add("❌ $name: ${cause.message ?: cause.toString()}")
                logger.log(java.util.logging.Level.WARNING, "Error al inicializar el módulo $name", t)
            }
        }

        fun initDbDependentModule(name: String, block: () -> Unit) {
            if (databaseService == null || profileRegistry == null) {
                statuses[name] = "<yellow>[ DESACTIVADO (No DB) ]</yellow>"
                return
            }
            initModule(name, block)
        }

        fun initEconomyDependentModule(name: String, block: () -> Unit) {
            if (economyService == null) {
                statuses[name] = "<yellow>[ DESACTIVADO (No Eco) ]</yellow>"
                return
            }
            initDbDependentModule(name, block)
        }

        // 1. Inicializar Base de Datos (Módulo SQL)
        initModule("Base de Datos") {
            val db = DatabaseService(this, configManager)
            databaseService = db
            val reg = com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry(db, logger)
            profileRegistry = reg
            server.pluginManager.registerEvents(
                com.github.berserkr2k.coreplugin.infrastructure.listeners.UserProfileListener(this, reg),
                this
            )
        }

        // 2. Inicializar Módulo de Interfaces (Tablist, Bossbars) sin NMS
        initModule("Tablist e Interfaces") {
            tablistService = TablistService(this, placeholderBridge, configManager)
        }

        // 3. Inicializar Módulo de Chat Enriquecido (DeluxeChat Equivalence)
        initDbDependentModule("Chat y Mensajería") {
            chatModule = ChatModule(this, configManager, placeholderBridge, profileRegistry!!)
            PrivateMessageCommand(this, commandManager, profileRegistry!!, messagesConfig)
            ColorCommand(this, commandManager, profileRegistry!!, configManager, messagesConfig)
        }

        // 4. Inicializar Módulo de Yunques (Moderación y Permisos de Color)
        initModule("Yunques y Bloqueos") {
            anvilModule = AnvilModule(this, configManager)
        }

        // 5. Inicializar Módulo de Hologramas Interactivos Modernos
        initModule("Hologramas Virtuales") {
            val holoService = HologramService(this, configManager, placeholderBridge)
            hologramService = holoService
            HologramCommand(this, commandManager, holoService)
        }

        // 6. Inicializar Módulo de Podios Físicos e Editor de ArmorStands
        initDbDependentModule("Podios y Clasificaciones") {
            val lService = LeaderboardService(this, configManager, messagesConfig, databaseService!!, placeholderBridge, profileRegistry!!)
            leaderboardService = lService
            server.pluginManager.registerEvents(lService, this)
            LeaderboardCommand(this, commandManager, lService)

            val editorConfig = configManager.loadModuleConfig("editor.conf", EditorConfig::class.java, EditorConfig()).join()
            ArmorStandEditorGui.init(this, configManager)
            ArmorStandEditorCommand(this, commandManager, editorConfig)
            server.pluginManager.registerEvents(
                ArmorStandEditorListener(this, lService, messagesConfig),
                this
            )
        }

        // 7. Inicializar Módulo "Misc" (Escaleras como Sillas)
        initModule("Mecánica Sillas") {
            val cListener = ChairListener(this, messagesConfig)
            chairListener = cListener
            server.pluginManager.registerEvents(cListener, this)
        }

        // 8. Inicializar Módulo de Economía Multi-Divisa Altamente Seguro
        initDbDependentModule("Economía Multi-Divisa") {
            val ecoService = EconomyService(this, configManager, databaseService!!, profileRegistry!!)
            economyService = ecoService

            val isVaultEnabled = server.pluginManager.isPluginEnabled("Vault")
            val isVaultUnlockedEnabled = server.pluginManager.isPluginEnabled("VaultUnlocked")

            if (isVaultEnabled || isVaultUnlockedEnabled) {
                var registeredLegacy = false
                var registeredVault2 = false

                try {
                    Class.forName("net.milkbowl.vault.economy.Economy")
                    com.github.berserkr2k.coreplugin.infrastructure.economy.LegacyVaultRegisterHelper.register(this, ecoService)
                    registeredLegacy = true
                } catch (e: Throwable) {}

                try {
                    Class.forName("net.milkbowl.vault2.economy.Economy")
                    com.github.berserkr2k.coreplugin.infrastructure.economy.Vault2RegisterHelper.register(this, ecoService)
                    registeredVault2 = true
                } catch (e: Throwable) {}

                if (registeredLegacy && registeredVault2) {
                    paperLogger.info("¡Wrappers seguros de Vault (Legacy) y Vault2 (VaultUnlocked) registrados con prioridad Máxima!")
                } else if (registeredLegacy) {
                    paperLogger.info("¡Wrapper seguro de Vault (Legacy) registrado con prioridad Máxima!")
                } else if (registeredVault2) {
                    paperLogger.info("¡Wrapper seguro de Vault2 (VaultUnlocked) registrado con prioridad Máxima!")
                }
            }

            WalletCommand(this, commandManager, ecoService, messagesConfig)

            if (server.pluginManager.isPluginEnabled("PlaceholderAPI")) {
                EconomyPlaceholderExpansion(ecoService).register()
                paperLogger.info("¡Expansión de economía de PlaceholderAPI registrada con éxito!")
            }

            // Programar purga automática de 90 días en segundo plano cada 24 horas (delay 1h)
            threadCoordinator.runTimerAsync(72000, 1728000) {
                ecoService.purgeInactiveRecords(90).thenAccept { deleted ->
                    if (deleted > 0) {
                        paperLogger.info("¡Purga de base de datos completada! ${deleted} cuentas inactivas eliminadas.")
                    }
                }
            }
        }

        // 9. Inicializar Módulo de Utilidades Modulares
        initModule("Utilidades Modulares") {
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
        }

        // 10. Inicializar Módulo Premium de Kits
        initEconomyDependentModule("Kits Premium") {
            val kService = com.github.berserkr2k.coreplugin.infrastructure.kits.KitService(this, configManager, databaseService!!, economyService!!, messagesConfig, profileRegistry!!)
            val kGuis = com.github.berserkr2k.coreplugin.infrastructure.kits.KitGuis(this, configManager, kService)
            com.github.berserkr2k.coreplugin.infrastructure.kits.KitCommand(this, commandManager, kService, kGuis)
        }

        // 11. Inicializar Módulo Premium de Estelas de Partículas (Projectile Trails)
        initDbDependentModule("Estelas de Proyectil") {
            val trailManager = com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails.ProjectileTrailManager(this, databaseService!!, configManager, profileRegistry!!)
            val trailGuis = com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails.TrailGuis(this, configManager, trailManager)
            server.pluginManager.registerEvents(com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails.ProjectileTrailListener(this, trailManager), this)
            com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails.ProjectileTrailCommand(this, commandManager, trailManager, trailGuis)
        }

        // 12. Inicializar Módulo Premium de Tiendas de Mercado Dinámico
        initEconomyDependentModule("Tiendas de Mercado") {
            val sManager = com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop.ShopManager(this, configManager, databaseService!!)
            shopManager = sManager
            val sGuis = com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop.ShopGuis(this, sManager, economyService!!, messagesConfig)
            com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop.ShopCommand(this, commandManager, sManager, sGuis, messagesConfig)
        }

        // 13. Inicializar Módulo de Teletransporte (Warps)
        initModule("Puntos de Teletransporte") {
            val wService = WarpService(this, configManager, messagesConfig)
            warpService = wService
            server.pluginManager.registerEvents(wService, this)
            WarpCommand(this, commandManager, wService, messagesConfig)
        }

        // Programar guardado por lotes cada 5 minutos asíncronamente si la DB / registry se cargó bien
        if (profileRegistry != null) {
            Bukkit.getAsyncScheduler().runAtFixedRate(this, { _ ->
                profileRegistry?.flushAllActive()
            }, 5, 5, java.util.concurrent.TimeUnit.MINUTES)
        }

        // Imprimir Dashboard Visual Premium de Estado de Módulos
        val logo = """
<gold>  ___ ___  ___  ___ ___ _   _   _ ___ ___ ___ 
 / __/ _ \| _ \/ __| _ \ | | | | | __|_ _|_ _|
| (_| (_) |   / (_ |  _/ |_| |_| | _| | | | | 
 \___\___/|_|_\\___|_| |____\___/|___|___|___|</gold>
        """.trimIndent()

        val sb = java.lang.StringBuilder()
        sb.append("\n").append(logo).append("\n")
        sb.append("<dark_gray>======================================================</dark_gray>\n")
        sb.append("<yellow><bold>          COREPLUGIN MODULES INITIALIZATION</bold></yellow>\n")
        sb.append("<dark_gray>======================================================</dark_gray>\n")

        val keys = listOf(
            "Base de Datos" to "Base de Datos",
            "Tablist e Interfaces" to "Tablist e Interfaces",
            "Chat y Mensajería" to "Chat y Mensajería",
            "Yunques y Bloqueos" to "Yunques y Bloqueos",
            "Hologramas Virtuales" to "Hologramas Virtuales",
            "Podios y Clasificaciones" to "Podios y Clasificaciones",
            "Mecánica Sillas" to "Mecánica Sillas",
            "Economía Multi-Divisa" to "Economía Multi-Divisa",
            "Utilidades Modulares" to "Utilidades Modulares",
            "Kits Premium" to "Kits Premium",
            "Estelas de Proyectil" to "Estelas de Proyectil",
            "Tiendas de Mercado" to "Tiendas de Mercado",
            "Puntos de Teletransporte" to "Puntos de Teletransporte"
        )

        for ((label, key) in keys) {
            val status = statuses[key] ?: "<yellow>[ INACTIVO ]</yellow>"
            val paddedLabel = label.padEnd(25, '.')
            sb.append(" <gray>⚡ <gold>$paddedLabel</gold>: $status</gray>\n")
        }

        sb.append("<dark_gray>======================================================</dark_gray>\n")

        if (errors.isEmpty()) {
            sb.append("<green>✔ ¡Todos los módulos de CorePlugin cargados con éxito!</green>\n")
        } else {
            sb.append("<red>⚠ ¡Se detectaron errores durante la inicialización!</red>\n")
            sb.append("<yellow>Detalles del diagnóstico:</yellow>\n")
            for (err in errors) {
                sb.append("  $err\n")
            }
            sb.append("<yellow>Consulte la consola para ver los stack traces completos.</yellow>\n")
        }
        sb.append("<dark_gray>======================================================</dark_gray>")

        paperLogger.info(mm.deserialize(sb.toString()))
    }

    override fun onDisable() {
        val mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
        val shutdownList = mutableListOf<String>()

        if (hologramService != null) {
            hologramService?.shutdown()
            shutdownList.add(" <gray>🔌 <red>Hologramas</red>      : <gold>[ DESACTIVADO ]</gold> (Cerrando visores...)</gray>")
        }
        if (leaderboardService != null) {
            leaderboardService?.shutdown()
            shutdownList.add(" <gray>🔌 <red>Podios</red>          : <gold>[ DESACTIVADO ]</gold> (Limpiando ArmorStands...)</gray>")
        }
        if (chairListener != null) {
            chairListener?.shutdown()
            shutdownList.add(" <gray>🔌 <red>Sillas</red>          : <gold>[ DESACTIVADO ]</gold> (Desmontando jugadores...)</gray>")
        }
        if (profileRegistry != null) {
            profileRegistry?.flushAllActive()?.join()
            shutdownList.add(" <gray>🔌 <red>Perfiles</red>        : <gold>[ PERSISTIDO ]</gold> (Guardando caché...)</gray>")
        }
        if (databaseService != null) {
            databaseService?.shutdown()
            shutdownList.add(" <gray>🔌 <red>Base de Datos</red>   : <gold>[ CERRADO ]</gold> (Pool Hikari...)</gray>")
        }
        configManager.shutdown()
        shutdownList.add(" <gray>🔌 <red>Configuraciones</red> : <gold>[ CERRADO ]</gold> (Deteniendo hilos...)</gray>")

        val sbShutdown = java.lang.StringBuilder()
        sbShutdown.append("\n<dark_gray>======================================================</dark_gray>\n")
        sbShutdown.append("<red><bold>          COREPLUGIN SHUTTING DOWN</bold></red>\n")
        sbShutdown.append("<dark_gray>======================================================</dark_gray>\n")
        for (line in shutdownList) {
            sbShutdown.append(line).append("\n")
        }
        sbShutdown.append("<dark_gray>======================================================</dark_gray>\n")
        sbShutdown.append("<red>¡CorePlugin desactivado y datos persistidos con éxito!</red>\n")
        sbShutdown.append("<dark_gray>======================================================</dark_gray>")

        paperLogger.info(mm.deserialize(sbShutdown.toString()))
    }
}