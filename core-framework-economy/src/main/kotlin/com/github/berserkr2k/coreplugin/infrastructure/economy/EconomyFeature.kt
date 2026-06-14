package com.github.berserkr2k.coreplugin.infrastructure.economy

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureContext
import com.github.berserkr2k.coreplugin.api.core.lifecycle.FeatureDescriptor
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.database.DatabaseService
import com.github.berserkr2k.coreplugin.api.core.user.ProfileRegistry
import com.github.berserkr2k.coreplugin.api.core.scheduler.Task

class EconomyFeature : Feature {
    override val descriptor = FeatureDescriptor(
        id = "economy",
        dependencies = setOf("database"),
        provides = setOf(com.github.berserkr2k.coreplugin.api.framework.economy.EconomyService::class.java)
    )

    private var ecoService: EconomyService? = null
    private var purgeTask: Task? = null

    override fun registerServices(registry: ServiceRegistry) {
        val plugin = registry.get(org.bukkit.plugin.Plugin::class.java)
        val configService = registry.get(com.github.berserkr2k.coreplugin.api.core.config.ConfigService::class.java)
        val dbService = registry.get(DatabaseService::class.java)
        val profileRegistry = registry.get(ProfileRegistry::class.java)
        val folderProvider = registry.get(com.github.berserkr2k.coreplugin.api.core.filesystem.FeatureFolderProvider::class.java)

        val service = EconomyService(
            plugin,
            configService,
            dbService,
            profileRegistry,
            folderProvider
        )
        this.ecoService = service

        registry.register(com.github.berserkr2k.coreplugin.api.framework.economy.EconomyService::class.java, service)
    }

    override fun onEnable(context: FeatureContext) {
        context.messageService.registerFeature("economy", EconomyMessages.defaults)

        val service = ecoService ?: context.getService(com.github.berserkr2k.coreplugin.api.framework.economy.EconomyService::class.java) as EconomyService

        registerVaultHooks(context._plugin, service)
        registerPlaceholderAPI(service)

        val commandService = context.getService(com.github.berserkr2k.coreplugin.api.framework.command.CommandService::class.java)
        WalletCommand(context._plugin, commandService.manager, service, context.messageService, context.registry)

        this.purgeTask = context.taskScheduler.runAsyncTimer(Runnable {
            service.purgeInactiveRecords(90).thenAccept { deleted ->
                if (deleted > 0) context.platform.logger.info("¡Purga de base de datos completada! $deleted cuentas inactivas eliminadas.")
            }
        }, 72000L, 1728000L)
    }

    override fun onDisable(context: FeatureContext) {
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
