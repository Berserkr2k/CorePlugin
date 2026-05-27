package com.github.berserkr2k.coreplugin.v1_8_R3.ui

import com.github.berserkr2k.coreplugin.api.ui.VisualNotifier
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import java.time.Duration

/**
 * Implementación de notificaciones visuales exclusiva para Minecraft 1.8.8.
 * Utiliza 'BukkitAudiences' para traducir los componentes modernos a paquetes NMS compatibles.
 *
 * @property adventure Instancia de BukkitAudiences necesaria para enviar mensajes en versiones legacy.
 */
class LegacyVisualNotifier(
    private val adventure: BukkitAudiences
) : VisualNotifier {

    // Instancia de MiniMessage para traducir strings como "<red>Hola" a componentes.
    private val miniMessage = MiniMessage.miniMessage()

    override fun sendActionBar(player: Player, message: String) {
        // Envolvemos al jugador de Bukkit en un "Audience" de Adventure
        val audience = adventure.player(player)
        val component = miniMessage.deserialize(message)

        audience.sendActionBar(component)
    }

    override fun sendTitle(
        player: Player,
        title: String,
        subtitle: String,
        fadeInTicks: Int,
        stayTicks: Int,
        fadeOutTicks: Int
    ) {
        val audience = adventure.player(player)

        // Si el string está vacío, enviamos un componente vacío para que Bukkit no se queje
        val titleComponent = if (title.isNotEmpty()) miniMessage.deserialize(title) else Component.empty()
        val subtitleComponent = if (subtitle.isNotEmpty()) miniMessage.deserialize(subtitle) else Component.empty()

        // Adventure usa java.time.Duration, un tick de Minecraft equivale a 50 milisegundos
        val times = Title.Times.times(
            Duration.ofMillis(fadeInTicks * 50L),
            Duration.ofMillis(stayTicks * 50L),
            Duration.ofMillis(fadeOutTicks * 50L)
        )

        val adventureTitle = Title.title(titleComponent, subtitleComponent, times)
        audience.showTitle(adventureTitle)
    }

    override fun sendTabList(player: Player, header: String, footer: String) {
        val audience = adventure.player(player)
        val h = if (header.isNotEmpty()) miniMessage.deserialize(header) else Component.empty()
        val f = if (footer.isNotEmpty()) miniMessage.deserialize(footer) else Component.empty()

        audience.sendPlayerListHeaderAndFooter(h, f)
    }
}