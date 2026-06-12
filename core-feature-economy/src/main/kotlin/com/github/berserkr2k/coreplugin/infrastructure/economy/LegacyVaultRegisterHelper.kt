package com.github.berserkr2k.coreplugin.infrastructure.economy

import com.github.berserkr2k.coreplugin.api.feature.economy.EconomyService
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.ServicePriority

object LegacyVaultRegisterHelper {
    fun register(plugin: Plugin, ecoService: EconomyService) {
        plugin.server.servicesManager.register(
            net.milkbowl.vault.economy.Economy::class.java,
            VaultEconomyHook(ecoService),
            plugin,
            ServicePriority.Highest
        )
    }
}
