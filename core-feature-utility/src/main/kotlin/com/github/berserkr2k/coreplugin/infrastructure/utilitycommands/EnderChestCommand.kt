package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.bukkit.parser.PlayerParser.playerParser
import com.github.berserkr2k.coreplugin.common.ColorUtility

class EnderChestCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val messagesConfig: MessagesConfig
) {

    init {
        val ecBuilder = manager.commandBuilder("enderchest")
            .optional("target", playerParser())
            .permission("core.utility.enderchest")
            .handler { context ->
                val sender = context.sender()
                if (sender !is Player) {
                    val msg = messagesConfig.utility["only-players"] ?: "<red>Solo jugadores pueden ejecutar este comando.</red>"
                    sender.sendMessage(ColorUtility.parse(msg))
                    return@handler
                }

                val targetOpt = context.optional<Player>("target")
                if (targetOpt.isPresent) {
                    // Abrir cofre de otro jugador
                    if (!sender.hasPermission("core.utility.enderchest.others")) {
                        val msg = messagesConfig.utility["no-permission-other"] ?: "<red>No tienes permiso para aplicar esto a otros jugadores.</red>"
                        sender.sendMessage(ColorUtility.parse(msg))
                        return@handler
                    }
                    val target = targetOpt.get()
                    sender.openInventory(target.enderChest)

                    val key = "enderchest-opened-other"
                    val defaultMsg = "<green>Abriendo cofre de ender de <player>...</green>"
                    val msg = (messagesConfig.utility[key] ?: defaultMsg).replace("<player>", target.name)
                    sender.sendMessage(ColorUtility.parse(msg))
                } else {
                    // Abrir propio cofre
                    sender.openInventory(sender.enderChest)

                    val key = "enderchest-opened"
                    val defaultMsg = "<green>Abriendo tu cofre de ender...</green>"
                    val msg = messagesConfig.utility[key] ?: defaultMsg
                    sender.sendMessage(ColorUtility.parse(msg))
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
