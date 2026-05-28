package com.github.berserkr2k.coreplugin.infrastructure.economy

import com.github.berserkr2k.coreplugin.CorePlugin
import org.bukkit.plugin.ServicePriority

object Vault2RegisterHelper {
    fun register(plugin: CorePlugin, ecoService: EconomyService) {
        plugin.server.servicesManager.register(
            net.milkbowl.vault2.economy.Economy::class.java,
            Vault2EconomyHook(ecoService),
            plugin,
            ServicePriority.Highest
        )
    }
}
