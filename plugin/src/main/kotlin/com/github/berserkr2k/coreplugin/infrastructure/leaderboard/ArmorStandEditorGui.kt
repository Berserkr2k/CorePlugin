package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import net.kyori.adventure.text.minimessage.MiniMessage

object ArmorStandEditorGui {

    private val miniMessage = MiniMessage.miniMessage()

    fun open(player: Player, stand: ArmorStand) {
        val inv: Inventory = Bukkit.createInventory(
            null, 
            27, 
            miniMessage.deserialize("<gold><bold>Editor: ArmorStand</bold></gold>")
        )

        // Botones de acción directa representados por componentes tipo-seguros
        inv.setItem(10, createGuiItem(Material.ARMOR_STAND, "<yellow>Alternar Brazos</yellow>", "<gray>Click para añadir/remover brazos</gray>"))
        inv.setItem(12, createGuiItem(Material.STONE_SLAB, "<yellow>Alternar Base</yellow>", "<gray>Click para mostrar/ocultar plato inferior</gray>"))
        inv.setItem(14, createGuiItem(Material.FEATHER, "<yellow>Alternar Gravedad</yellow>", "<gray>Click para suspender en el aire</gray>"))
        inv.setItem(16, createGuiItem(Material.PLAYER_HEAD, "<yellow>Editar Pose Predeterminada</yellow>", "<gray>Click para aplicar pose heroica</gray>"))

        player.openInventory(inv)
    }

    private fun createGuiItem(material: Material, title: String, loreLine: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item
        meta.displayName(miniMessage.deserialize(title))
        meta.lore(listOf(miniMessage.deserialize(loreLine)))
        item.itemMeta = meta
        return item
    }
}
