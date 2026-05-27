package com.github.berserkr2k.coreplugin.v1_21_R3.ui

import com.github.berserkr2k.coreplugin.api.ui.VisualNotifier
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.entity.Player

/**
 * Implementación de notificaciones visuales para Minecraft 1.21.X.
 * Utiliza la API nativa de Spigot para el envío de paquetes,
 * apoyándose en Adventure únicamente para traducir el formato MiniMessage.
 */
class ModernVisualNotifier : VisualNotifier { // Ya no pedimos BukkitAudiences en el constructor

    private val miniMessage = MiniMessage.miniMessage()

    // Serializador para convertir <red> y Hex a código nativo de Spigot 1.16+
    private val serializer = LegacyComponentSerializer.builder()
        .character('§')
        .hexColors()
        .useUnusualXRepeatedCharacterHexFormat()
        .build()

    private fun translate(text: String): String {
        if (text.isEmpty()) return ""
        val component = miniMessage.deserialize(text)
        return serializer.serialize(component)
    }

    override fun sendActionBar(player: Player, message: String) {
        // API Nativa de Spigot para ActionBars
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(translate(message)))
    }

    override fun sendTitle(
        player: Player,
        title: String,
        subtitle: String,
        fadeInTicks: Int,
        stayTicks: Int,
        fadeOutTicks: Int
    ) {
        // API Nativa de Bukkit para Títulos
        player.sendTitle(
            translate(title),
            translate(subtitle),
            fadeInTicks,
            stayTicks,
            fadeOutTicks
        )
    }

    override fun sendTabList(player: Player, header: String, footer: String) {
        player.setPlayerListHeaderFooter(translate(header), translate(footer))
    }
}