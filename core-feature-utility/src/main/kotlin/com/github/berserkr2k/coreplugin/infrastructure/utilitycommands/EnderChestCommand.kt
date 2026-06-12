package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.bukkit.parser.PlayerParser.playerParser

class EnderChestCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val messageService: MessageService
) {

    init {
        val ecBuilder = manager.commandBuilder("enderchest")
            .optional("target", playerParser())
            .permission("core.utility.enderchest")
            .handler { context ->
                val sender = context.sender()
                if (sender !is Player) {
                    messageService.send(sender, UtilityMessages.ONLY_PLAYERS)
                    return@handler
                }

                val targetOpt = context.optional<Player>("target")
                if (targetOpt.isPresent) {
                    // Abrir cofre de otro jugador
                    if (!sender.hasPermission("core.utility.enderchest.others")) {
                        messageService.send(sender, UtilityMessages.NO_PERMISSION_OTHER)
                        return@handler
                    }
                    val target = targetOpt.get()
                    sender.openInventory(target.enderChest)

                    messageService.send(
                        sender,
                        UtilityMessages.ENDERCHEST_OPENED_OTHER,
                        PlaceholderContext.of(Placeholder.parsed("player", target.name))
                    )
                } else {
                    // Abrir propio cofre
                    sender.openInventory(sender.enderChest)
                    messageService.send(sender, UtilityMessages.ENDERCHEST_OPENED)
                }
            }

        // Registrar /enderchest
        manager.command(ecBuilder)

        // Registrar alias /ec
        manager.command(
            manager.commandBuilder("ec")
                .optional("target", playerParser())
                .permission("core.utility.enderchest")
                .handler(ecBuilder.handler())
        )
    }
}
