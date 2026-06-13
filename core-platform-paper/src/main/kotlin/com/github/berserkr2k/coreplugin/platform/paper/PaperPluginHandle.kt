package com.github.berserkr2k.coreplugin.platform.paper

import com.github.berserkr2k.coreplugin.api.platform.PluginHandle
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.logging.Logger

/**
 * Paper/Bukkit implementation of [PluginHandle].
 * Wraps a [JavaPlugin] instance so that all framework and feature code
 * can depend solely on the abstract [PluginHandle] contract.
 */
class PaperPluginHandle(private val plugin: JavaPlugin) : PluginHandle {
    override val name: String get() = plugin.name
    override val dataFolder: File get() = plugin.dataFolder
    override val logger: Logger get() = plugin.logger
    override fun isEnabled(): Boolean = plugin.isEnabled
}
