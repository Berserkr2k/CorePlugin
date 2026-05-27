package com.github.berserkr2k.coreplugin.v1_21_R3.nametag

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.wrappers.WrappedChatComponent
import com.comphenix.protocol.wrappers.WrappedTeamParameters
import com.github.berserkr2k.coreplugin.api.nametag.NameTagAdapter
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.Optional

class ModernNameTagAdapter : NameTagAdapter {

    private val miniMessage = MiniMessage.miniMessage()
    private val gsonSerializer = GsonComponentSerializer.gson()
    private val protocolManager = ProtocolLibrary.getProtocolManager()

    /**
     * En la 1.21 los paquetes ya no usan Strings simples ("§c"),
     * usan componentes JSON estrictos.
     */
    private fun translateToJson(text: String): String {
        if (text.isEmpty()) return "{\"text\":\"\"}"
        val component = miniMessage.deserialize(text)
        return gsonSerializer.serialize(component)
    }

    override fun update(player: Player, groupName: String, priority: Int, prefix: String, suffix: String, nameColor: String) {
        val teamName = String.format("%02d_%s", priority, groupName)
        val packet = protocolManager.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM)

        packet.integers.write(0, 0)
        packet.strings.write(0, teamName)

        // Convertimos tu String del YAML al formato de ProtocolLib de forma segura
        val formatColor = try {
            com.comphenix.protocol.wrappers.EnumWrappers.ChatFormatting.valueOf(nameColor.uppercase())
        } catch (e: Exception) {
            com.comphenix.protocol.wrappers.EnumWrappers.ChatFormatting.WHITE
        }

        val parameters = WrappedTeamParameters.newBuilder()
            .displayName(WrappedChatComponent.fromJson(translateToJson(teamName)))
            .prefix(WrappedChatComponent.fromJson(translateToJson(prefix)))
            .suffix(WrappedChatComponent.fromJson(translateToJson(suffix)))
            .nametagVisibility("always")
            .collisionRule("always")
            .color(formatColor) // <-- ¡AQUÍ INYECTAMOS EL COLOR DEL NOMBRE!
            .build()

        // 1. Le decimos explícitamente a Kotlin que es un Optional de Cualquier cosa (Any)
        @Suppress("UNCHECKED_CAST")
        val optionalClass = Optional::class.java as Class<Optional<Any>>

        // 2. Usamos el modificador específico con nuestra clase explícita
        packet.getSpecificModifier(optionalClass).write(0, Optional.of(parameters.handle))

        // Inyectamos al jugador en la colección del paquete
        packet.getSpecificModifier(Collection::class.java).write(0, listOf(player.name))

        // Enviamos el paquete a todos
        Bukkit.getOnlinePlayers().forEach { target ->
            protocolManager.sendServerPacket(target, packet)
        }
    }

    override fun remove(player: Player) {
        val packet = protocolManager.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM)
        packet.integers.write(0, 1) // Modo 1: Eliminar Equipo
        packet.strings.write(0, player.name)

        Bukkit.getOnlinePlayers().forEach { target ->
            protocolManager.sendServerPacket(target, packet)
        }
    }
}