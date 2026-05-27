package com.github.berserkr2k.coreplugin

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.bootstrap.PluginProviderContext
import org.bukkit.plugin.java.JavaPlugin

class PluginBootstrap : PluginBootstrap {
    
    override fun bootstrap(context: BootstrapContext) {
        // Registro temprano de clases u otra configuración inicial
    }

    override fun createPlugin(context: PluginProviderContext): JavaPlugin {
        return CorePlugin(context.logger, context.dataDirectory)
    }
}
