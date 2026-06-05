package com.github.berserkr2k.coreplugin.infrastructure.listeners

import com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry
import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin

class UserProfileListener(
    private val plugin: Plugin,
    private val profileRegistry: ProfileRegistry
) : Listener {

    @EventHandler
    fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
        val uuid = event.uniqueId
        val name = event.name

        // 1. Verificar bloqueo cross-server (Velocity/BungeeCord)
        var attempts = 0
        while (profileRegistry.isSyncLocked(uuid)) {
            attempts++
            if (attempts >= 50) { // 5 segundos de espera (50 * 100ms)
                plugin.logger.warning("⚠️ Se expiró por tiempo límite el bloqueo de sincronización para $name ($uuid).")
                break
            }
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                break
            }
        }

        // 2. Cargar perfil de usuario en caché de forma segura y síncrona en el hilo del pre-login asíncrono
        try {
            profileRegistry.loadProfile(uuid, name).join()
        } catch (e: Exception) {
            plugin.logger.severe("❌ Fallo crítico al cargar el perfil de usuario para $name ($uuid): ${e.message}")
            e.printStackTrace()
            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                Component.text("§cError al inicializar tu perfil de usuario. Por favor reintenta ingresar en unos momentos.")
            )
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        val uuid = player.uniqueId

        // Guardar síncronamente al salir en el programador global para asegurar la persistencia antes de que salte al otro servidor
        try {
            profileRegistry.unloadAndSave(uuid).join()
        } catch (e: Exception) {
            plugin.logger.severe("❌ Error al guardar perfil de salida para ${player.name}: ${e.message}")
        }
    }
}
