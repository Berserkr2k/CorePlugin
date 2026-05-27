package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import com.github.berserkr2k.coreplugin.common.gui.CustomMenu
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.util.EulerAngle

object ArmorStandEditorGui {

    private val miniMessage = MiniMessage.miniMessage()

    fun open(plugin: Plugin, player: Player, stand: ArmorStand) {
        val menu = CustomMenu(
            miniMessage.deserialize("<gold><bold>Editor: ArmorStand</bold></gold>"),
            27,
            plugin
        )

        // Botones de acción directa con handlers anti-steal garantizados por CustomMenu
        menu.setItem(10, createGuiItem(Material.ARMOR_STAND, "<yellow>Alternar Brazos</yellow>", "<gray>Click para añadir/remover brazos</gray>")) { p, _ ->
            stand.setArms(!stand.hasArms())
            p.sendMessage(miniMessage.deserialize("<green>¡Brazos de la armadura alternados con éxito!</green>"))
        }

        menu.setItem(12, createGuiItem(Material.STONE_SLAB, "<yellow>Alternar Base</yellow>", "<gray>Click para mostrar/ocultar plato inferior</gray>")) { p, _ ->
            stand.setBasePlate(!stand.hasBasePlate())
            p.sendMessage(miniMessage.deserialize("<green>¡Base de la armadura alternada con éxito!</green>"))
        }

        menu.setItem(14, createGuiItem(Material.FEATHER, "<yellow>Alternar Gravedad</yellow>", "<gray>Click para suspender en el aire</gray>")) { p, _ ->
            stand.setGravity(!stand.hasGravity())
            p.sendMessage(miniMessage.deserialize("<green>¡Gravedad de la armadura alternada con éxito!</green>"))
        }

        menu.setItem(16, createGuiItem(Material.PLAYER_HEAD, "<yellow>Editar Pose Predeterminada</yellow>", "<gray>Click para aplicar pose heroica</gray>")) { p, _ ->
            stand.rightArmPose = EulerAngle(Math.toRadians(-45.0), 0.0, Math.toRadians(30.0))
            stand.leftArmPose = EulerAngle(Math.toRadians(-15.0), 0.0, Math.toRadians(-10.0))
            p.sendMessage(miniMessage.deserialize("<green>¡Pose heroica predeterminada aplicada con éxito!</green>"))
        }

        menu.open(player)
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
