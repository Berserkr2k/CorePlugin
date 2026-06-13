package com.github.berserkr2k.coreplugin.infrastructure.economy

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.core.database.DatabaseService
import com.github.berserkr2k.coreplugin.api.core.user.ProfileRegistry
import com.github.berserkr2k.coreplugin.api.core.scheduler.Task

class EconomyFeature : Feature {
    override val id = "economy"
    override val dependencies = setOf("database")

    private var ecoService: EconomyService? = null
    private var purgeTask: Task? = null

    override fun onEnable(context: FeatureContext) {
        context.messageService.registerFeature("economy", EconomyMessages.defaults)

        val folderProvider = context.getService(com.github.berserkr2k.coreplugin.api.core.filesystem.FeatureFolderProvider::class.java)
        val dbService = context.getService(DatabaseService::class.java)
        val profileRegistry = context.getService(ProfileRegistry::class.java)

        // 1. Inicializar el servicio purificado de dependencias concretas
        val service = EconomyService(
            context._plugin,
            context.configService, // Pasamos la interfaz de la API en lugar del manager concreto
            dbService,
            profileRegistry,
            folderProvider
        )
        this.ecoService = service

        // 2. Exponer el contrato en el ServiceRegistry para que otras features (como la Shop) puedan consumirlo
        context.registry.register(com.github.berserkr2k.coreplugin.api.framework.economy.EconomyService::class.java, service)

        // 3. Absorber la lógica de hooks externos (Vault / Vault2 / PAPI) de forma aislada
        registerVaultHooks(context._plugin, service)
        registerPlaceholderAPI(service)

        // 4. Registrar comandos locales usando el gestor de comandos del contexto
        val commandService = context.getService(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java)
        WalletCommand(context._plugin, commandService.manager, service, context.messageService, context.registry)

        // 5. Encapsular la tarea asíncrona de purga automática de 90 días (Cada 24h, delay 1h)
        this.purgeTask = context.taskScheduler.runAsyncTimer(Runnable {
            service.purgeInactiveRecords(90).thenAccept { deleted ->
                if (deleted > 0) context.platform.logger.info("¡Purga de base de datos completada! $deleted cuentas inactivas eliminadas.")
            }
        }, 72000L, 1728000L)
    }

    override fun onDisable(context: FeatureContext) {
        // Cancelar la tarea programada para evitar hilos zombie en recargas
        purgeTask?.cancel()
    }

    private fun registerVaultHooks(plugin: org.bukkit.plugin.Plugin, service: EconomyService) {
        if (plugin.server.pluginManager.isPluginEnabled("Vault") || plugin.server.pluginManager.isPluginEnabled("VaultUnlocked")) {
            runCatching {
                Class.forName("net.milkbowl.vault.economy.Economy")
                LegacyVaultRegisterHelper.register(plugin, service)
            }
            runCatching {
                Class.forName("net.milkbowl.vault2.economy.Economy")
                Vault2RegisterHelper.register(plugin, service)
            }
        }
    }

    private fun registerPlaceholderAPI(service: EconomyService) {
        if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            EconomyPlaceholderExpansion(service).register()
        }
    }
}
