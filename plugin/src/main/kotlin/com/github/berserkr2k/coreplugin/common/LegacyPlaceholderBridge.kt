package com.github.berserkr2k.coreplugin.common

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

class LegacyPlaceholderBridge {

    private val miniMessage = MiniMessage.miniMessage()
    private val isPapiEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")

    /**
     * Procesa y convierte texto heredado con variables en un componente Adventure MiniMessage de forma segura.
     */
    fun parseLegacyStringSecurely(player: Player, text: String): Component {
        val resolvedText = if (isPapiEnabled) {
            PapiHook.setPlaceholders(player, text)
        } else {
            text.replace("%player_name%", player.name)
                .replace("%player_displayname%", player.name)
        }
        return miniMessage.deserialize(resolvedText)
    }

    // El uso de un objeto interno evita que el ClassLoader intente cargar clases de PlaceholderAPI
    // a menos que este cargador determine que el plugin está activo.
    private object PapiHook {
        fun setPlaceholders(player: Player, text: String): String {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text)
        }
    }
}
