package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.bukkit.parser.PlayerParser.playerParser
import org.incendo.cloud.parser.standard.StringParser.greedyStringParser
import net.kyori.adventure.title.Title
import net.kyori.adventure.util.Ticks
import com.github.berserkr2k.coreplugin.common.ColorUtility

import com.github.berserkr2k.coreplugin.api.core.message.MessageService

class SendTitleCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val utilityService: UtilityService,
    private val messageService: MessageService
) {

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

                    val mainTitleComp = ColorUtility.parse(mainTitleText)
                    val subtitleComp = if (subtitleText.isNotEmpty()) ColorUtility.parse(subtitleText) else net.kyori.adventure.text.Component.empty()

                    val fadeIn = utilityService.config.title.fadeInTicks.toLong()
                    val stay = utilityService.config.title.stayTicks.toLong()
                    val fadeOut = utilityService.config.title.fadeOutTicks.toLong()

                    val times = Title.Times.times(
                        Ticks.duration(fadeIn),
                        Ticks.duration(stay),
                        Ticks.duration(fadeOut)
                    )
                    val title = Title.title(mainTitleComp, subtitleComp, times)

                    target.showTitle(title)
                }
        )
    }
}
