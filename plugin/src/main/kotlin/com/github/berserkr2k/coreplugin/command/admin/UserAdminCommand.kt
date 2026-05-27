package com.github.berserkr2k.coreplugin.command.admin

import com.github.berserkr2k.coreplugin.CorePlugin
import com.github.berserkr2k.coreplugin.api.gui.CustomMenu
import com.github.berserkr2k.coreplugin.api.gui.GuiItem
import com.github.berserkr2k.coreplugin.command.SubCommand
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

class UserAdminCommand(private val plugin: CorePlugin) : SubCommand {

    override val name = "user"
    override val permission = "coreplugin.admin"

    override fun execute(player: Player, args: Array<out String>) {
        if (args.isEmpty()) {
            player.sendMessage("§cUso correcto: /core user <jugador>")
            return
        }

        val targetName = args[0]
        player.sendMessage("§e[Admin] Buscando datos de $targetName en la red...")

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            var targetUuid: UUID? = null
            var currentCoins = 0.0

            try {
                val tableName = "${plugin.databaseManager.tablePrefix}player_stats"
                plugin.databaseManager.getConnection().use { connection ->
                    val query = "SELECT uuid, coins FROM $tableName WHERE name = ?"
                    connection.prepareStatement(query).use { ps ->
                        ps.setString(1, targetName)
                        val rs = ps.executeQuery()
                        if (rs.next()) {
                            targetUuid = UUID.fromString(rs.getString("uuid"))
                            currentCoins = rs.getDouble("coins")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // MAGIA KOTLIN: Congelamos el valor en una constante inmutable para que el botón lo pueda leer sin miedo
            val finalUuid = targetUuid

            // Volvemos al hilo principal para crear la GUI
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (finalUuid == null) {
                    player.sendMessage("§c[Admin] El jugador no existe en la base de datos.")
                    return@Runnable
                }

                // 1. Usamos NUESTRO motor. Filas = 3 (27 slots). Usamos MiniMessage para el título
                val menu = CustomMenu("<dark_gray>Admin: <blue>$targetName", 3)

                // 2. Preparamos el ítem
                val emerald = ItemStack(Material.EMERALD)
                val meta = emerald.itemMeta
                meta?.setDisplayName("§aEditar Monedas")
                meta?.lore = listOf(
                    "§7Monedas actuales: §e$currentCoins",
                    "",
                    "§eHaz clic §7para abrir la consola",
                    "§7de edición en el chat."
                )
                emerald.itemMeta = meta

                // 3. Envolvemos en nuestro GuiItem (que ya maneja el clic)
                val editCoinsButton = GuiItem(emerald) { event ->
                    // Aunque nuestro MenuListener general suele cancelarlo, es buena práctica asegurarnos
                    event.isCancelled = true

                    player.closeInventory()

                    // Usamos finalUuid, que es 100% seguro
                    plugin.adminSessionManager.pendingCoinEdits[player.uniqueId] = finalUuid

                    player.sendMessage("§a[Admin] Escribe el nuevo valor de monedas en el chat (o 'cancelar'):")
                }

                // 4. Asignamos al slot 13 (centro exacto de 3 filas) y abrimos
                menu.setItem(13, editCoinsButton)
                menu.open(player)
            })
        })
    }
}