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
import com.github.berserkr2k.coreplugin.infrastructure.mechanics.AnvilModule
import com.github.berserkr2k.coreplugin.infrastructure.mechanics.ChairListener
import com.github.berserkr2k.coreplugin.infrastructure.ui.TablistService
import com.github.berserkr2k.coreplugin.infrastructure.regions.service.RegionManager
import com.github.berserkr2k.coreplugin.infrastructure.regions.command.RegionCommand
import com.github.berserkr2k.coreplugin.infrastructure.regions.listener.SelectionListener
import com.github.berserkr2k.coreplugin.infrastructure.regions.listener.ProtectionListener
import com.github.berserkr2k.coreplugin.infrastructure.regions.listener.RegionTrackingListener
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Path
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.incendo.cloud.execution.ExecutionCoordinator
import org.bukkit.command.CommandSender
import org.bukkit.Bukkit
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
    private var chairListener: ChairListener? = null
    private var tablistService: TablistService? = null
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
        registry.register(com.github.berserkr2k.coreplugin.api.core.placeholder.PlaceholderService::class.java, placeholderBridge)
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

        // 3. Inicializar Módulo de Regiones y Protecciones (Framework Layer)
        val regionManagerImpl = RegionManager(this, configService, taskScheduler)
        registry.register(com.github.berserkr2k.coreplugin.api.framework.regions.RegionService::class.java, regionManagerImpl)
        reloadCoordinator.register("regions", regionManagerImpl)

        server.pluginManager.registerEvents(SelectionListener(regionManagerImpl), this)
        server.pluginManager.registerEvents(ProtectionListener(regionManagerImpl), this)
        server.pluginManager.registerEvents(RegionTrackingListener(regionManagerImpl), this)

        RegionCommand(this, commandManager, regionManagerImpl, messageRegistry)

        // 4. Inicializar Base de Datos (Módulo SQL)
        val db = DatabaseServiceImpl(this, configManager, taskScheduler)
        databaseService = db
        val reg = com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry(db, logger)
        profileRegistry = reg
        
        registry.register(com.github.berserkr2k.coreplugin.api.core.database.DatabaseService::class.java, db)
        registry.register(com.github.berserkr2k.coreplugin.api.core.user.ProfileRegistry::class.java, reg)
        registry.register(com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry::class.java, reg)

        server.pluginManager.registerEvents(
            com.github.berserkr2k.coreplugin.infrastructure.listeners.UserProfileListener(this, reg, stateService),
            this
        )

        // 5. Inicializar Módulo de Interfaces (Tablist, Bossbars)
        val tablist = TablistService(this, placeholderBridge, configManager, registry)
        tablistService = tablist

        // 6. Inicializar Módulo de Yunques
        val anvil = AnvilModule(this, configManager, registry)
        anvilModule = anvil

        // 7. Inicializar Módulo "Misc" (Escaleras como Sillas)
        val cListener = ChairListener(this, messageRegistry, registry)
        chairListener = cListener
        server.pluginManager.registerEvents(cListener, this)

        // 8. Registrar reloadables en reloadCoordinator
        val coreReloadable = object : com.github.berserkr2k.coreplugin.api.core.lifecycle.Reloadable {
            override suspend fun reload() {
                val fRegistry = messageRegistry as? com.github.berserkr2k.coreplugin.infrastructure.message.FeatureMessageRegistry
                if (fRegistry != null) {
                    for (featureId in fRegistry.getRegisteredFeatures()) {
                        fRegistry.registerFeature(featureId)
                    }
                }
                anvilModule?.reload()
                tablist.reload()
            }
        }
        reloadCoordinator.register("core", coreReloadable)

        // 9. Registrar DebugCommand
        val fRegistry = messageRegistry as? com.github.berserkr2k.coreplugin.infrastructure.message.FeatureMessageRegistry
        if (fRegistry != null) {
            DebugCommand(this, commandManager, reloadCoordinator, fRegistry, configManager, messageRegistry)
        }

        // 10. Programar guardado por lotes cada 5 minutos asíncronamente
        taskScheduler.runAsyncTimer({
            profileRegistry?.flushAllActive()
        }, 6000L, 6000L)

        try {
            // Inicializar Motor de Ciclo de Vida Enterprise para Características Modernas
            val context = com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext(
                plugin = this,
                registry = serviceRegistry,
                commandManager = commandManager,
                taskScheduler = taskScheduler,
                regionTaskScheduler = regionTaskScheduler,
                messageService = messageRegistry,
                configService = configService,
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
            manager.register(com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails.ProjectileTrailFeature())
            manager.register(com.github.berserkr2k.coreplugin.infrastructure.hologram.HologramFeature())
            manager.register(com.github.berserkr2k.coreplugin.infrastructure.scoreboard.ScoreboardFeature())
            manager.register(com.github.berserkr2k.coreplugin.infrastructure.utilitycommands.UtilityFeature())
            manager.register(com.github.berserkr2k.coreplugin.infrastructure.leaderboard.LeaderboardFeature())
            manager.register(com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop.ShopFeature())

            // Habilitación masiva
            manager.enableAll()
        } catch (e: Exception) {
            paperLogger.error("Error crítico al inicializar el CorePlugin: ${e.message}", e)
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {
        val mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
        val shutdownList = mutableListOf<String>()

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