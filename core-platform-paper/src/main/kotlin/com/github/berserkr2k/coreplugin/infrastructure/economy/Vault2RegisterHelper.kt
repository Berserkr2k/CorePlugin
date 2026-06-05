package com.github.berserkr2k.coreplugin.infrastructure.economy

import com.github.berserkr2k.coreplugin.api.economy.EconomyService
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.ServicePriority

object Vault2RegisterHelper {
    fun register(plugin: Plugin, ecoService: EconomyService) {
        plugin.server.servicesManager.register(
            net.milkbowl.vault2.economy.Economy::class.java,
            Vault2EconomyHook(ecoService),
            plugin,
            ServicePriority.Highest
        )
    }
}
