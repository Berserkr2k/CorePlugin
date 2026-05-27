package com.github.berserkr2k.coreplugin.nametag

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.github.berserkr2k.coreplugin.CorePlugin
import org.bukkit.Bukkit

/**
 * Escucha el tráfico de red para proteger los NameTags.
 * Si otro plugin envía un Scoreboard, este interceptor reinyecta los rangos.
 */
class NameTagInterceptor(private val core: CorePlugin) {

    fun register() {
        val protocolManager = ProtocolLibrary.getProtocolManager()

        protocolManager.addPacketListener(
            object : PacketAdapter(
                core, // Usamos 'core'
                PacketType.Play.Server.SCOREBOARD_OBJECTIVE,
                PacketType.Play.Server.SCOREBOARD_DISPLAY_OBJECTIVE
            ) {
                override fun onPacketSending(event: PacketEvent) {
                    val player = event.player
                    if (player == null || !player.isOnline) return

                    Bukkit.getScheduler().runTaskLater(core, Runnable {
                        if (player.isOnline) {
                            // 2. LLAMAMOS EXACTAMENTE A NUESTRO CORE
                            core.nameTagManager.applyTag(player)
                        }
                    }, 1L)
                }
            }
        )
        core.logger.info("Escudo interceptor de NameTags activado.")
    }
}