package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilder
import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilderFactory
import com.github.berserkr2k.coreplugin.api.framework.item.lore
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import com.github.berserkr2k.coreplugin.api.core.message.MessageService

class ArmorStandEditorCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val editorConfig: EditorConfig,
    private val messageService: MessageService
) {

    init {
        manager.command(
            manager.commandBuilder("core")
               .literal("editor")
               .permission(editorConfig.permissionUseEditor)
               .handler { context ->
                    val player = context.sender() as? Player ?: return@handler
                    
                    val factory = org.bukkit.Bukkit.getServicesManager().load(ItemBuilderFactory::class.java)!!
                    val key = NamespacedKey(plugin, "stand_editor_tool")
                    val item = factory.create(Material.valueOf(editorConfig.editorItemMaterial))
                        .displayName(editorConfig.editorItemName)
                        .lore(editorConfig.editorItemLore)
                        .writeNBT(key, PersistentDataType.BOOLEAN, true)
                        .build()
                    
                    player.inventory.addItem(item)
                    messageService.send(player, LeaderboardMessages.VARA_RECIBIDA)
                }
        )
    }
}
