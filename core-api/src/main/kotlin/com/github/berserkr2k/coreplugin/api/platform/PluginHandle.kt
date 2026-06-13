package com.github.berserkr2k.coreplugin.api.platform

import java.io.File
import java.util.logging.Logger

/**
 * Platform-agnostic handle to the hosting plugin bootstrap.
 *
 * Features and framework services must depend on this interface instead of
 * Paper's [org.bukkit.plugin.java.JavaPlugin] to remain portable and testable.
 */
interface PluginHandle {
    /** The plugin name as declared in plugin.yml / paper-plugin.yml. */
    val name: String

    /** The plugin's data folder on disk (e.g. plugins/CorePlugin/). */
    val dataFolder: File

    /** JUL Logger scoped to this plugin. */
    val logger: Logger

    /** Whether the plugin is currently enabled on the server. */
    fun isEnabled(): Boolean
}
