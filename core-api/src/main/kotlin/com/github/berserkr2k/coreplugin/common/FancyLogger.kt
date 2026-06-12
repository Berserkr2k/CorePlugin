package com.github.berserkr2k.coreplugin.common

import org.bukkit.Bukkit

object FancyLogger {

    /**
     * Imprime un log decorativo en la consola con colores premium y estructura uniforme para acciones de administración.
     */
    fun logAdminAction(module: String, actionText: String) {
        val message = "<dark_gray>[<gold><bold>$module</bold></gold>]</dark_gray> <gray>⚡</gray> <yellow>$actionText</yellow>"
        Bukkit.getConsoleSender().sendMessage(ColorUtility.parse(message))
    }

    /**
     * Imprime un log general en la consola con colores premium.
     */
    fun logInfo(module: String, text: String) {
        val message = "<dark_gray>[<aqua><bold>$module</bold></aqua>]</dark_gray> <gray>»</gray> <white>$text</white>"
        Bukkit.getConsoleSender().sendMessage(ColorUtility.parse(message))
    }

    /**
     * Imprime una advertencia elegante en la consola.
     */
    fun logWarning(module: String, text: String) {
        val message = "<dark_gray>[<yellow><bold>$module</bold></yellow>]</dark_gray> <red>⚠</red> <orange>$text</orange>"
        Bukkit.getConsoleSender().sendMessage(ColorUtility.parse(message))
    }
}
