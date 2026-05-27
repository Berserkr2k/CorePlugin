package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import net.kyori.adventure.text.minimessage.MiniMessage

class ArmorStandEditorCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val editorConfig: EditorConfig
) {
    private val editorKey = NamespacedKey(plugin, "stand_editor_tool")
    private val miniMessage = MiniMessage.miniMessage()

    init {
        manager.command(
            manager.commandBuilder("core")
               .literal("editor")
               .permission(editorConfig.permissionUseEditor)
               .handler { context ->
                    val player = context.sender() as? Player ?: return@handler
                    val item = ItemStack(Material.valueOf(editorConfig.editorItemMaterial))
                    val meta = item.itemMeta ?: return@handler
                    
                    meta.displayName(miniMessage.deserialize("<gold><bold>Vara de Edición de ArmorStands</bold></gold>"))
                    meta.lore(listOf(
                        miniMessage.deserialize("<gray>Haz click derecho sobre cualquier ArmorStand</gray>"),
                        miniMessage.deserialize("<gray>para abrir el editor de pose y equipamiento.</gray>")
                    ))
                    
                    // Inyección de PDC para identificar el objeto de manera única y persistente
                    meta.persistentDataContainer.set(editorKey, PersistentDataType.BOOLEAN, true)
                    item.itemMeta = meta
                    
                    player.inventory.addItem(item)
                    player.sendMessage(miniMessage.deserialize("<green>¡Has recibido la Vara de Edición!</green>"))
                }
        )
    }
}
