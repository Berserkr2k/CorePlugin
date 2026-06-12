package com.github.berserkr2k.coreplugin

import com.github.berserkr2k.coreplugin.common.LegacyPlaceholderBridge
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.infra.di.SimpleServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler
import com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler
import com.github.berserkr2k.coreplugin.api.core.scheduler.ThreadAssertion
import com.github.berserkr2k.coreplugin.platform.paper.PaperTaskScheduler
import com.github.berserkr2k.coreplugin.platform.paper.PaperRegionTaskScheduler
import com.github.berserkr2k.coreplugin.platform.paper.PaperThreadAssertion
import com.github.berserkr2k.coreplugin.api.core.event.CoreEventBus
import com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService
import com.github.berserkr2k.coreplugin.infra.state.SimplePlayerStateService
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.filesystem.FeatureFolderProvider
import com.github.berserkr2k.coreplugin.api.core.config.ConfigService
import com.github.berserkr2k.coreplugin.api.core.database.DatabaseService
import com.github.berserkr2k.coreplugin.infrastructure.database.DatabaseServiceImpl
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
import com.github.berserkr2k.coreplugin.infrastructure.scoreboard.ScoreboardService
import com.github.berserkr2k.coreplugin.infrastructure.scoreboard.ScoreboardCommand
import com.github.berserkr2k.coreplugin.infrastructure.regions.service.RegionManager
import com.github.berserkr2k.coreplugin.infrastructure.regions.command.PlayerSelectionSession
import com.github.berserkr2k.coreplugin.infrastructure.regions.command.RegionCommand
import com.github.berserkr2k.coreplugin.infrastructure.regions.resolver.RegionRuleResolver
import com.github.berserkr2k.coreplugin.infrastructure.regions.listener.SelectionListener
import com.github.berserkr2k.coreplugin.infrastructure.regions.listener.ProtectionListener
import com.github.berserkr2k.coreplugin.infrastructure.regions.listener.RegionTrackingListener
import com.github.berserkr2k.coreplugin.infrastructure.spawn.service.SpawnService
import com.github.berserkr2k.coreplugin.infrastructure.spawn.command.SpawnCommand
import com.github.berserkr2k.coreplugin.infrastructure.spawn.listener.SpawnListener
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
import com.github.berserkr2k.coreplugin.infrastructure.commands.DebugCommand

class CorePlugin(
    private val paperLogger: net.kyori.adventure.text.logger.slf4j.ComponentLogger,
    private val dataFolderPath: Path
) : JavaPlugin() {

    lateinit var serviceRegistry: ServiceRegistry
        private set
    lateinit var configManager: ModularConfigManager
        private set
    lateinit var placeholderBridge: LegacyPlaceholderBridge
        private set
    lateinit var commandManager: LegacyPaperCommandManager<CommandSender>
        private set
    lateinit var reloadCoordinator: com.github.berserkr2k.coreplugin.infrastructure.lifecycle.ReloadCoordinator
        private set

    private var databaseService: DatabaseServiceImpl? = null
    private var profileRegistry: com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry? = null
    private var anvilModule: AnvilModule? = null
    private var hologramService: HologramService? = null
    private var leaderboardService: LeaderboardService? = null
    private var chairListener: ChairListener? = null
    private var tablistService: TablistService? = null
    private var utilityService: UtilityService? = null
    private var shopManager: com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop.ShopManager? = null
    private var scoreboardService: ScoreboardService? = null
    private var regionManager: RegionManager? = null
    private var spawnService: SpawnService? = null
    private var featureManager: com.github.berserkr2k.coreplugin.infrastructure.lifecycle.FeatureManager? = null

    override fun onEnable() {
        val registry = SimpleServiceRegistry()
        serviceRegistry = registry

        val taskScheduler = PaperTaskScheduler(this)
        val regionTaskScheduler = PaperRegionTaskScheduler(this)
        val threadAssertion = PaperThreadAssertion()
        val eventBus = CoreEventBus()
        val stateService = SimplePlayerStateService()

        registry.register(ServiceRegistry::class.java, registry)
        server.servicesManager.register(
            ServiceRegistry::class.java,
            registry,
            this,
            org.bukkit.plugin.ServicePriority.Normal
        )
        registry.register(TaskScheduler::class.java, taskScheduler)
        registry.register(RegionTaskScheduler::class.java, regionTaskScheduler)
        registry.register(ThreadAssertion::class.java, threadAssertion)
        registry.register(CoreEventBus::class.java, eventBus)
        registry.register(PlayerStateService::class.java, stateService)
        registry.register(org.bukkit.plugin.Plugin::class.java, this)
        registry.register(org.bukkit.plugin.java.JavaPlugin::class.java, this)
        registry.register(CorePlugin::class.java, this)

        // 1. Inicializar estructura modular de carpetas y migraciones
        val bootstrapper = com.github.berserkr2k.coreplugin.infrastructure.filesystem.RuntimeFolderBootstrapper(dataFolderPath, logger)
        registry.register(com.github.berserkr2k.coreplugin.api.core.filesystem.FeatureFolderProvider::class.java, bootstrapper)

        // 2. Inicializar sistema modular de mensajes y registrar features
        val messageRegistry = com.github.berserkr2k.coreplugin.infrastructure.message.FeatureMessageRegistry(bootstrapper)
        registry.register(com.github.berserkr2k.coreplugin.api.core.message.MessageService::class.java, messageRegistry)

        // 2b. Inicializar ReloadCoordinator
        reloadCoordinator = com.github.berserkr2k.coreplugin.infrastructure.lifecycle.ReloadCoordinator(logger)
        registry.register(com.github.berserkr2k.coreplugin.infrastructure.lifecycle.ReloadCoordinator::class.java, reloadCoordinator)

        messageRegistry.registerFeature("core", com.github.berserkr2k.coreplugin.api.core.message.CoreMessages.defaults)
        messageRegistry.registerFeature("spawn", com.github.berserkr2k.coreplugin.infrastructure.spawn.SpawnMessages.defaults)
        messageRegistry.registerFeature("regions", com.github.berserkr2k.coreplugin.infrastructure.regions.RegionMessages.defaults)
        messageRegistry.registerFeature("economy", com.github.berserkr2k.coreplugin.infrastructure.economy.EconomyMessages.defaults)
        messageRegistry.registerFeature("utility", com.github.berserkr2k.coreplugin.infrastructure.utilitycommands.UtilityMessages.defaults)
        messageRegistry.registerFeature("shops", com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop.ShopMessages.defaults)
        messageRegistry.registerFeature("warps", com.github.berserkr2k.coreplugin.infrastructure.warps.WarpMessages.defaults)
        messageRegistry.registerFeature("kits", com.github.berserkr2k.coreplugin.infrastructure.kits.KitMessages.defaults)
        messageRegistry.registerFeature("holograms", com.github.berserkr2k.coreplugin.infrastructure.hologram.HologramMessages.defaults)
        messageRegistry.registerFeature("leaderboards", com.github.berserkr2k.coreplugin.infrastructure.leaderboard.LeaderboardMessages.defaults)

        placeholderBridge = LegacyPlaceholderBridge()
        registry.register(LegacyPlaceholderBridge::class.java, placeholderBridge)
        configManager = ModularConfigManager(this, dataFolderPath)
        registry.register(ModularConfigManager::class.java, configManager)

        // Inicializar el MenuManager para prevención de robos y duplicados
        com.github.berserkr2k.coreplugin.common.gui.MenuManager.init(this)

        // Inicializar el orquestador de comandos nativos de Brigadier en Cloud v2
        commandManager = LegacyPaperCommandManager.createNative(
            this,
            ExecutionCoordinator.simpleCoordinator()
        )

        val configService = com.github.berserkr2k.coreplugin.infrastructure.config.ConfigServiceImpl(this.dataFolder.toPath())
        val menuServiceImpl = com.github.berserkr2k.coreplugin.common.gui.MenuServiceImpl(this)
        val commandServiceImpl = com.github.berserkr2k.coreplugin.infrastructure.commands.CommandServiceImpl(commandManager)
        val itemBuilderFactoryImpl = com.github.berserkr2k.coreplugin.platform.paper.ItemBuilderFactoryImpl()

        registry.register(ConfigService::class.java, configService)
        registry.register(com.github.berserkr2k.coreplugin.api.framework.menu.MenuService::class.java, menuServiceImpl)
        registry.register(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java, commandServiceImpl)
        registry.register(com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilderFactory::class.java, itemBuilderFactoryImpl)

        try {
            // Inicializar módulos heredados provisionales
            initializeModules()

            // Inicializar Motor de Ciclo de Vida Enterprise para Características Modernas
            val context = com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext(
                plugin = this,
                registry = serviceRegistry,
                commandManager = commandManager,
                taskScheduler = taskScheduler,
                regionTaskScheduler = regionTaskScheduler,
                messageService = messageRegistry,
                configService = serviceRegistry.get(ConfigService::class.java),
                databaseService = databaseService
            )

            val manager = com.github.berserkr2k.coreplugin.infrastructure.lifecycle.FeatureManager(context)
            this.featureManager = manager

            // Registro centralizado de las nuevas features modulares
            manager.register(com.github.berserkr2k.coreplugin.infrastructure.spawn.SpawnFeature())
            manager.register(com.github.berserkr2k.coreplugin.infrastructure.warps.WarpFeature())
            manager.register(com.github.berserkr2k.coreplugin.infrastructure.kits.KitFeature())
            manager.register(com.github.berserkr2k.coreplugin.infrastructure.chat.ChatFeature())
            manager.register(com.github.berserkr2k.coreplugin.infrastructure.economy.EconomyFeature())

            // Habilitación masiva
            manager.enableAll()
        } catch (e: Exception) {
            paperLogger.error("Error crítico al inicializar el CorePlugin: ${e.message}", e)
            server.pluginManager.disablePlugin(this)
        }
    }

    private fun initializeModules() {
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
            initDbDependentModule(name, block)
        }

        val stateService = serviceRegistry.get(PlayerStateService::class.java)
        val taskScheduler = serviceRegistry.get(TaskScheduler::class.java)
        val regionTaskScheduler = serviceRegistry.get(RegionTaskScheduler::class.java)
        val messageRegistry = serviceRegistry.get(MessageService::class.java)!!
        val folderProvider = serviceRegistry.get(com.github.berserkr2k.coreplugin.api.core.filesystem.FeatureFolderProvider::class.java)!!
        val configService = serviceRegistry.get(ConfigService::class.java)

        // 1. Inicializar Base de Datos (Módulo SQL)
        initModule("Base de Datos") {
            val db = DatabaseServiceImpl(this, configManager, taskScheduler)
            databaseService = db
            val reg = com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry(db, logger)
            profileRegistry = reg
            
            serviceRegistry.register(com.github.berserkr2k.coreplugin.api.core.database.DatabaseService::class.java, db)
            serviceRegistry.register(com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry::class.java, reg)

            server.pluginManager.registerEvents(
                com.github.berserkr2k.coreplugin.infrastructure.listeners.UserProfileListener(this, reg, stateService),
                this
            )
        }

        // 2. Inicializar Módulo de Interfaces (Tablist, Bossbars) sin NMS
        initModule("Tablist e Interfaces") {
            val service = TablistService(this, placeholderBridge, configManager, serviceRegistry)
            tablistService = service
            // Eliminado del ServiceRegistry público por ser feature/infraestructura interna
        }

        // 3. Módulo de Chat Enriquecido migrado a ChatFeature

        // 4. Inicializar Módulo de Yunques (Moderación y Permisos de Color)
        initModule("Yunques y Bloqueos") {
            val service = AnvilModule(this, configManager, serviceRegistry)
            anvilModule = service
            // Eliminado del ServiceRegistry público por ser feature interna
        }

        // 5. Inicializar Módulo de Hologramas Interactivos Modernos
        initModule("Hologramas Virtuales") {
            val holoService = HologramService(this, configManager, placeholderBridge, serviceRegistry)
            hologramService = holoService
            serviceRegistry.register(com.github.berserkr2k.coreplugin.api.feature.holograms.HologramService::class.java, holoService)
            HologramCommand(this, commandManager, holoService, messageRegistry)
        }

        // 6. Inicializar Módulo de Podios Físicos e Editor de ArmorStands
        initDbDependentModule("Podios y Clasificaciones") {
            val lService = LeaderboardService(this, configManager, messageRegistry, databaseService!!, placeholderBridge, profileRegistry!!, serviceRegistry)
            leaderboardService = lService
            serviceRegistry.register(com.github.berserkr2k.coreplugin.api.feature.leaderboard.LeaderboardService::class.java, lService)
            server.pluginManager.registerEvents(lService, this)
            LeaderboardCommand(this, commandManager, lService, serviceRegistry, messageRegistry)

            val editorConfig = configManager.loadModuleConfig("core/editor.conf", EditorConfig::class.java, EditorConfig()).join()
            // Eliminado del ServiceRegistry público
            ArmorStandEditorGui.init(this, configManager, serviceRegistry)
            ArmorStandEditorCommand(this, commandManager, editorConfig, messageRegistry)
            server.pluginManager.registerEvents(
                ArmorStandEditorListener(this, lService, messageRegistry, serviceRegistry),
                this
            )
        }

        // 7. Inicializar Módulo "Misc" (Escaleras como Sillas)
        initModule("Mecánica Sillas") {
            val cListener = ChairListener(this, messageRegistry, serviceRegistry)
            chairListener = cListener
            server.pluginManager.registerEvents(cListener, this)
        }

        // 8. Módulo de Economía Multi-Divisa migrado a EconomyFeature

        // 9. Inicializar Módulo de Utilidades Modulares
        initModule("Utilidades Modulares") {
            val uService = UtilityService(this, configManager, messageRegistry, serviceRegistry)
            utilityService = uService
            serviceRegistry.register(UtilityService::class.java, uService)
            FlyCommand(this, commandManager, uService, messageRegistry)
            SpeedCommand(this, commandManager, messageRegistry)
            HatCommand(this, commandManager, messageRegistry)
            FeedCommand(this, commandManager, messageRegistry)
            HealCommand(this, commandManager, messageRegistry)
            AnvilCommand(this, commandManager, uService, messageRegistry)
            EnderChestCommand(this, commandManager, messageRegistry)
            ExpCommand(this, commandManager, messageRegistry)
            BroadcastCommand(this, commandManager, uService, messageRegistry)
            SendTitleCommand(this, commandManager, uService, messageRegistry)
        }

        // 10. Inicializar Módulo Premium de Kits
        // Lógica migrada al Kernel central de ciclo de vida en onEnable()

        // 11. Inicializar Módulo Premium de Estelas de Partículas (Projectile Trails)
        initDbDependentModule("Estelas de Proyectil") {
            val folderProvider = serviceRegistry.get(com.github.berserkr2k.coreplugin.api.core.filesystem.FeatureFolderProvider::class.java)!!
            val trailManager = com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails.ProjectileTrailManager(this, databaseService!!, configManager, profileRegistry!!, folderProvider)
            serviceRegistry.register(com.github.berserkr2k.coreplugin.api.feature.trails.ProjectileTrailService::class.java, trailManager)
            val trailGuis = com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails.TrailGuis(this, configManager, trailManager, serviceRegistry)
            server.pluginManager.registerEvents(com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails.ProjectileTrailListener(this, trailManager, serviceRegistry), this)
            com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails.ProjectileTrailCommand(this, commandManager, trailManager, trailGuis)
        }

        // 12. Inicializar Módulo Premium de Tiendas de Mercado Dinámico
        initEconomyDependentModule("Tiendas de Mercado") {
            val sManager = com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop.ShopManager(this, configManager, databaseService!!, serviceRegistry)
            shopManager = sManager
            // Eliminado del ServiceRegistry público por ser feature interna
            val sGuis = com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop.ShopGuis(this, sManager, messageRegistry, serviceRegistry)
            com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop.ShopCommand(this, commandManager, sManager, sGuis, messageRegistry)
        }

        // 13. Inicializar Módulo de Teletransporte (Warps)
        // Lógica migrada al Kernel central de ciclo de vida en onEnable()

        // 14. Inicializar Módulo de Scoreboard Modular
        initModule("Scoreboard Modular") {
            val sService = ScoreboardService(this, configManager, placeholderBridge, serviceRegistry)
            scoreboardService = sService
            // Eliminado del ServiceRegistry público por ser feature interna
            ScoreboardCommand(this, commandManager, sService, messageRegistry)
        }

        // 15. Inicializar Módulo de Regiones y Protecciones
        initModule("Regiones y Protecciones") {
            val rManager = RegionManager(this, configManager)
            regionManager = rManager
            serviceRegistry.register(com.github.berserkr2k.coreplugin.api.framework.regions.RegionService::class.java, rManager)

            val stateService = serviceRegistry.get(PlayerStateService::class.java)!!
            val eventBus = serviceRegistry.get(com.github.berserkr2k.coreplugin.api.core.event.CoreEventBus::class.java)!!
            val session = PlayerSelectionSession(stateService)
            val resolver = RegionRuleResolver(rManager)

            server.pluginManager.registerEvents(SelectionListener(session, rManager), this)
            server.pluginManager.registerEvents(ProtectionListener(resolver, rManager, messageRegistry), this)
            server.pluginManager.registerEvents(RegionTrackingListener(resolver, stateService, eventBus), this)

            RegionCommand(this, commandManager, session, rManager, resolver, messageRegistry, serviceRegistry)
        }

        // 16. Inicializar Módulo de Punto de Aparición (Spawn)
        // Lógica de inicialización migrada al Kernel central de ciclo de vida en onEnable()

        // Register reloadables in reloadCoordinator
        val coreReloadable = object : com.github.berserkr2k.coreplugin.api.core.lifecycle.Reloadable {
            override suspend fun reload() {
                val fRegistry = messageRegistry as? com.github.berserkr2k.coreplugin.infrastructure.message.FeatureMessageRegistry
                if (fRegistry != null) {
                    for (featureId in fRegistry.getRegisteredFeatures()) {
                        fRegistry.registerFeature(featureId)
                    }
                }
                anvilModule?.reload()
                tablistService?.reload()
                scoreboardService?.reload()
            }
        }
        reloadCoordinator.register("core", coreReloadable)

        val leaderboardReloadable = object : com.github.berserkr2k.coreplugin.api.core.lifecycle.Reloadable {
            override suspend fun reload() {
                leaderboardService?.reload()
            }
        }
        reloadCoordinator.register("leaderboards", leaderboardReloadable)

        regionManager?.let { reloadCoordinator.register("regions", it) }
        hologramService?.let { reloadCoordinator.register("holograms", it) }
        shopManager?.let { reloadCoordinator.register("shops", it) }
        utilityService?.let { reloadCoordinator.register("utility", it) }

        val trailService = serviceRegistry.getOptional(com.github.berserkr2k.coreplugin.api.feature.trails.ProjectileTrailService::class.java)
        if (trailService != null && trailService is com.github.berserkr2k.coreplugin.api.core.lifecycle.Reloadable) {
            reloadCoordinator.register("trails", trailService)
        }

        // Register DebugCommand
        val fRegistry = messageRegistry as? com.github.berserkr2k.coreplugin.infrastructure.message.FeatureMessageRegistry
        if (fRegistry != null) {
            DebugCommand(this, commandManager, reloadCoordinator, fRegistry, configManager, messageRegistry)
        }

        // Programar guardado por lotes cada 5 minutos asíncronamente si la DB / registry se cargó bien
        if (profileRegistry != null) {
            taskScheduler.runAsyncTimer({
                profileRegistry?.flushAllActive()
            }, 6000L, 6000L)
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
            "Puntos de Teletransporte" to "Puntos de Teletransporte",
            "Scoreboard Modular" to "Scoreboard Modular",
            "Regiones y Protecciones" to "Regiones y Protecciones",
            "Punto de Aparición (Spawn)" to "Punto de Aparición (Spawn)"
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
        if (scoreboardService != null) {
            scoreboardService?.shutdown()
            shutdownList.add(" <gray>🔌 <red>Scoreboards</red>     : <gold>[ DESACTIVADO ]</gold> (Limpiando sidebars...)</gray>")
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
        featureManager?.let {
            it.disableAll()
            shutdownList.add(" <gray>🔌 <red>Features</red>        : <gold>[ DESACTIVADO ]</gold> (Lifecycle shutdown...)</gray>")
        }

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