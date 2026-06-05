package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.common.gui.ItemBuilder
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager

class ArmorStandEditorCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val editorConfig: EditorConfig
) {

    init {
        manager.command(
            manager.commandBuilder("core")
               .literal("editor")
               .permission(editorConfig.permissionUseEditor)
               .handler { context ->
                    val player = context.sender() as? Player ?: return@handler
                    
                    val item = ItemBuilder(Material.valueOf(editorConfig.editorItemMaterial))
                        .displayName("<gold><bold>Vara de Edición de ArmorStands</bold></gold>")
                        .lore(listOf(
                            "<gray>Haz click derecho sobre cualquier ArmorStand</gray>",
                            "<gray>para abrir el editor de pose y equipamiento.</gray>"
                        ))
                        .pdc("stand_editor_tool", true)
                        .build()
                    
                    player.inventory.addItem(item)
                    player.sendMessage(ColorUtility.parse("<green>¡Has recibido la Vara de Edición!</green>"))
                }
        )
    }
}
