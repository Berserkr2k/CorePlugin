package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.bukkit.parser.PlayerParser.playerParser
import org.incendo.cloud.parser.standard.StringParser.greedyStringParser
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import net.kyori.adventure.util.Ticks

class SendTitleCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val messagesConfig: MessagesConfig
) {
    private val miniMessage = MiniMessage.miniMessage()

    init {
        manager.command(
            manager.commandBuilder("sendtitle")
                .required("target", playerParser())
                .required("message", greedyStringParser())
                .permission("core.utility.sendtitle")
                .handler { context ->
                    val target = context.get<Player>("target")
                    val message = context.get<String>("message")

                    val parts = message.split("|", limit = 2)
                    val mainTitleText = parts[0].trim()
                    val subtitleText = if (parts.size > 1) parts[1].trim() else ""

                    val mainTitleComp = miniMessage.deserialize(mainTitleText)
                    val subtitleComp = if (subtitleText.isNotEmpty()) miniMessage.deserialize(subtitleText) else net.kyori.adventure.text.Component.empty()

                    val times = Title.Times.times(
                        Ticks.duration(10L),
                        Ticks.duration(70L),
                        Ticks.duration(20L)
                    )
                    val title = Title.title(mainTitleComp, subtitleComp, times)

                    target.showTitle(title)
                }
        )
    }
}
