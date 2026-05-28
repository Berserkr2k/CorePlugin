package com.github.berserkr2k.coreplugin.infrastructure.economy

import com.github.berserkr2k.coreplugin.CorePlugin
import org.bukkit.plugin.ServicePriority

object LegacyVaultRegisterHelper {
    fun register(plugin: CorePlugin, ecoService: EconomyService) {
        plugin.server.servicesManager.register(
            net.milkbowl.vault.economy.Economy::class.java,
            VaultEconomyHook(ecoService),
            plugin,
            ServicePriority.Highest
        )
    }
}
