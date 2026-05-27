package com.github.berserkr2k.coreplugin.v1_8_R3.nametag

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.github.berserkr2k.coreplugin.api.nametag.NameTagAdapter
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class LegacyNameTagAdapter : NameTagAdapter {

    private val miniMessage = MiniMessage.miniMessage()
    private val serializer = LegacyComponentSerializer.legacySection()
    private val protocolManager = ProtocolLibrary.getProtocolManager()

    private fun translate(text: String): String {
        if (text.isEmpty()) return ""
        val serialized = serializer.serialize(miniMessage.deserialize(text))
        return if (serialized.length > 16) serialized.substring(0, 16) else serialized
    }

    override fun update(
        player: Player,
        groupName: String,
        priority: Int,
        prefix: String,
        suffix: String,
        nameColor: String
    ) {
        val teamName = String.format("%02d_%s", priority, groupName).take(16)

        // Creamos el paquete SCOREBOARD_TEAM usando ProtocolLib
        val packet = protocolManager.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM)

        // --- ESTRUCTURA DEL PAQUETE EN LA 1.8.8 ---
        packet.integers.write(1, 0) // Modo 0: Crear Equipo y Añadir Jugador
        packet.strings.write(0, teamName) // Nombre interno del equipo
        packet.strings.write(1, teamName) // Display Name
        packet.strings.write(2, translate(prefix)) // Prefijo
        packet.strings.write(3, translate(suffix)) // Sufijo
        packet.strings.write(4, "always") // Visibilidad del NameTag
        packet.integers.write(0, 0) // Pack Option (Fuego amigo, etc)

        // Escribimos la lista de jugadores que entran al equipo (en este caso, solo él)
        packet.getSpecificModifier(Collection::class.java).write(0, listOf(player.name))

        // Enviamos el paquete a TODOS los jugadores online para que vean su NameTag
        Bukkit.getOnlinePlayers().forEach { target ->
            protocolManager.sendServerPacket(target, packet)
        }
    }

    override fun remove(player: Player) {
        // En la 1.8, el modo 1 significa "Eliminar Equipo"
        val packet = protocolManager.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM)
        packet.integers.write(1, 1) // Modo 1
        packet.strings.write(0, player.name) // Nombre temporal para borrar

        Bukkit.getOnlinePlayers().forEach { target ->
            protocolManager.sendServerPacket(target, packet)
        }
    }
}