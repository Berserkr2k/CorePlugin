package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.IntegerParser.integerParser
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.parser.standard.StringParser.greedyStringParser
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.title.Title
import net.kyori.adventure.util.Ticks
import com.github.berserkr2k.coreplugin.common.ColorUtility

import com.github.berserkr2k.coreplugin.api.core.message.MessageService

class BroadcastCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val utilityService: UtilityService,
    private val messageService: MessageService
) {

    init {
        val broadcastBuilder = manager.commandBuilder("broadcast")
            .permission("core.utility.broadcast")

        // 1. /broadcast chat <message>
        manager.command(
            broadcastBuilder.literal("chat")
                .required("message", greedyStringParser())
                .handler { context ->
                    val message = context.get<String>("message")
                    val component = ColorUtility.parse(message)
                    Bukkit.getOnlinePlayers().forEach { player ->
                        player.sendMessage(component)
                    }
                    Bukkit.getConsoleSender().sendMessage(component)
                }
        )

        // 2. /broadcast actionbar <message>
        manager.command(
            broadcastBuilder.literal("actionbar")
                .required("message", greedyStringParser())
                .handler { context ->
                    val message = context.get<String>("message")
                    val component = ColorUtility.parse(message)
                    Bukkit.getOnlinePlayers().forEach { player ->
                        player.sendActionBar(component)
                    }
                }
        )

        // 3. /broadcast title <message>
        manager.command(
            broadcastBuilder.literal("title")
                .required("message", greedyStringParser())
                .handler { context ->
                    val message = context.get<String>("message")
                    sendTitleBroadcast(message)
                }
        )

        // 4. /broadcast bossbar <arguments>
        manager.command(
            broadcastBuilder.literal("bossbar")
                .required("arguments", greedyStringParser())
                .handler { context ->
                    val arguments = context.get<String>("arguments")
                    val input = arguments.trim()
                    val parts = input.split(Regex("\\s+"))

                    var parsedColor: String? = null
                    var parsedDuration: Int? = null
                    var messageStartIndex = 0

                    if (parts.isNotEmpty()) {
                        val firstWord = parts[0]
                        val colorEnum = try {
                            BossBar.Color.valueOf(firstWord.uppercase())
                        } catch (e: Exception) {
                            null
                        }

                        if (colorEnum != null) {
                            parsedColor = firstWord
                            messageStartIndex += firstWord.length

                            // Consumir espacios después del color
                            while (messageStartIndex < input.length && input[messageStartIndex].isWhitespace()) {
                                messageStartIndex++
                            }

                            val secondWord = parts.getOrNull(1)
                            if (secondWord != null) {
                                val durationVal = secondWord.toIntOrNull()
                                if (durationVal != null) {
                                    parsedDuration = durationVal
                                    messageStartIndex += secondWord.length

                                    // Consumir espacios después de la duración
                                    while (messageStartIndex < input.length && input[messageStartIndex].isWhitespace()) {
                                        messageStartIndex++
                                    }
                                }
                            }
                        } else {
                            // Si la primera palabra no es un color, verificar si es duración
                            val durationVal = firstWord.toIntOrNull()
                            if (durationVal != null) {
                                parsedDuration = durationVal
                                messageStartIndex += firstWord.length

                                // Consumir espacios después de la duración
                                while (messageStartIndex < input.length && input[messageStartIndex].isWhitespace()) {
                                    messageStartIndex++
                                }
                            }
                        }
                    }

                    val finalMessage = if (messageStartIndex < input.length) {
                        val msg = input.substring(messageStartIndex)
                        if (msg.trim().isEmpty()) input else msg
                    } else {
                        input
                    }

                    sendBossBarBroadcast(finalMessage, parsedColor, parsedDuration)
                }
        )
    }

    private fun sendTitleBroadcast(message: String) {
        val parts = message.split("|", limit = 2)
        val mainTitleText = parts[0].trim()
        val subtitleText = if (parts.size > 1) parts[1].trim() else ""

        val mainTitleComp = ColorUtility.parse(mainTitleText)
        val subtitleComp = if (subtitleText.isNotEmpty()) ColorUtility.parse(subtitleText) else net.kyori.adventure.text.Component.empty()

        val times = Title.Times.times(
            Ticks.duration(10L),
            Ticks.duration(70L),
            Ticks.duration(20L)
        )
        val title = Title.title(mainTitleComp, subtitleComp, times)

        Bukkit.getOnlinePlayers().forEach { player ->
            player.showTitle(title)
        }
    }

    private fun sendBossBarBroadcast(
        message: String,
        customColorStr: String?,
        customDuration: Int?
    ) {
        val config = utilityService.config.bossbar
        val colorStr = customColorStr ?: config.defaultColor
        val durationSeconds = customDuration ?: config.defaultDurationSeconds

        val color = try {
            BossBar.Color.valueOf(colorStr.uppercase())
        } catch (e: Exception) {
            BossBar.Color.PURPLE
        }

        val overlay = try {
            BossBar.Overlay.valueOf(config.defaultOverlay.uppercase())
        } catch (e: Exception) {
            BossBar.Overlay.PROGRESS
        }

        val nameComp = ColorUtility.parse(message)
        val bossBar = BossBar.bossBar(nameComp, 1.0f, color, overlay)

        // Mostrar a todos los jugadores online
        Bukkit.getOnlinePlayers().forEach { player ->
            player.showBossBar(bossBar)
        }

        // Destrucción asíncrona segura de la BossBar (Folia-ready)
        utilityService.taskScheduler.runSyncLater({
            Bukkit.getOnlinePlayers().forEach { player ->
                player.hideBossBar(bossBar)
            }
        }, durationSeconds * 20L)
    }
}
