package com.github.berserkr2k.coreplugin.admin

import com.github.berserkr2k.coreplugin.CorePlugin
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class AdminChatListener(private val plugin: CorePlugin) : Listener {

    // Usamos prioridad alta para interceptar antes que los plugins de formato de chat
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onChat(event: AsyncPlayerChatEvent) {
        val admin = event.player
        val targetUuid = plugin.adminSessionManager.pendingCoinEdits[admin.uniqueId] ?: return

        // 1. Ocultamos el mensaje para que el resto del servidor no lo vea
        event.isCancelled = true

        val input = event.message.trim()

        // 2. Opción de escape
        if (input.equals("cancelar", ignoreCase = true)) {
            plugin.adminSessionManager.pendingCoinEdits.remove(admin.uniqueId)
            admin.sendMessage("§c[Admin] Edición de economía cancelada.")
            return
        }

        // 3. Validación de datos
        val newCoins = input.toDoubleOrNull()
        if (newCoins == null || newCoins < 0) {
            admin.sendMessage("§c[Admin] Ingresa un número válido (ej. 150.5) o escribe 'cancelar'.")
            return
        }

        // 4. Limpiamos el estado del administrador
        plugin.adminSessionManager.pendingCoinEdits.remove(admin.uniqueId)

        // 5. Inyección asíncrona a la Base de Datos
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                val tableName = "${plugin.databaseManager.tablePrefix}player_stats"
                plugin.databaseManager.getConnection().use { connection ->
                    val query = "UPDATE $tableName SET coins = ? WHERE uuid = ?"
                    connection.prepareStatement(query).use { ps ->
                        ps.setDouble(1, newCoins)
                        ps.setString(2, targetUuid.toString())
                        val rows = ps.executeUpdate()

                        // 6. Volvemos al hilo principal para confirmar
                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            if (rows > 0) {
                                admin.sendMessage("§a[Admin] Se han establecido las monedas en $newCoins exitosamente.")
                            } else {
                                admin.sendMessage("§c[Admin] El jugador no tiene registros en la base de datos aún.")
                            }
                        })
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    admin.sendMessage("§c[Admin] Error fatal al conectar con SQLite/MariaDB.")
                })
            }
        })
    }
}