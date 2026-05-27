package com.github.berserkr2k.coreplugin.command.cosmetic

import com.github.berserkr2k.coreplugin.CorePlugin
import com.github.berserkr2k.coreplugin.api.gui.CustomMenu
import com.github.berserkr2k.coreplugin.api.gui.GuiItem
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ColorCommand(private val plugin: CorePlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Solo jugadores pueden usar este comando.")
            return true
        }

        if (!sender.hasPermission("coreplugin.color")) {
            sender.sendMessage("§cNo tienes permiso para cambiar tu color de chat.")
            return true
        }

        // Menú de 4 filas para que entren los 16 colores cómodamente
        val menu = CustomMenu("<dark_gray>Elige tu Color de Chat", 4)

        // Los 16 colores estándar de Minecraft 1.8
        val colors = listOf(
            ColorData(10, "§4Rojo Oscuro", "&4"),
            ColorData(11, "§cRojo", "&c"),
            ColorData(12, "§6Dorado", "&6"),
            ColorData(13, "§eAmarillo", "§e"),
            ColorData(14, "§2Verde Oscuro", "§2"),
            ColorData(15, "§aVerde", "§a"),
            ColorData(16, "§bCeleste", "§b"),
            ColorData(19, "§3Cian", "§3"),
            ColorData(20, "§1Azul Oscuro", "§1"),
            ColorData(21, "§9Azul", "§9"),
            ColorData(22, "§dRosa", "§d"),
            ColorData(23, "§5Morado", "§5"),
            ColorData(24, "§fBlanco", "§f"),
            ColorData(25, "§7Gris", "§7"),
            ColorData(28, "§8Gris Oscuro", "§8"),
            ColorData(29, "§0Negro", "§0")
        )

        // NOTA: Para no romper compatibilidad entre 1.8 (datos numéricos) y 1.21 (nombres planos),
        // usaremos el NAME_TAG como ítem temporal hasta que actualicemos tu ItemFactory para soportar cabezas Base64.
        for (color in colors) {
            val item = ItemStack(Material.NAME_TAG)
            val meta = item.itemMeta
            meta?.setDisplayName(color.name)
            meta?.lore = listOf("§7Haz clic para usar", "§7este color en el chat.")
            item.itemMeta = meta

            val btn = GuiItem(item) { event ->
                event.isCancelled = true
                sender.closeInventory()
                plugin.chatManager.savePlayerColor(sender.uniqueId, color.mmTag)
                sender.sendMessage("§a¡Color de chat actualizado a ${color.name}§a!")
            }
            menu.setItem(color.slot, btn)
        }

        menu.open(sender)
        return true
    }

    // Clase auxiliar simple para organizar los datos
    private data class ColorData(val slot: Int, val name: String, val mmTag: String)
}