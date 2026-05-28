package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.IntegerParser.integerParser
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.parser.standard.StringParser.greedyStringParser
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.title.Title
import net.kyori.adventure.util.Ticks

class BroadcastCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val utilityService: UtilityService,
    private val messagesConfig: MessagesConfig
) {
    private val miniMessage = MiniMessage.miniMessage()

    init {
        val broadcastBuilder = manager.commandBuilder("broadcast")
            .permission("core.utility.broadcast")

        // 1. /broadcast chat <message>
        manager.command(
            broadcastBuilder.literal("chat")
                .required("message", greedyStringParser())
                .handler { context ->
                    val message = context.get<String>("message")
                    val component = miniMessage.deserialize(message)
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
                    val component = miniMessage.deserialize(message)
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

        // 4.1 /broadcast bossbar <message>
        manager.command(
            broadcastBuilder.literal("bossbar")
                .required("message", greedyStringParser())
                .handler { context ->
                    val message = context.get<String>("message")
                    sendBossBarBroadcast(message, null, null)
                }
        )

        // 4.2 /broadcast bossbar <color> <message>
        manager.command(
            broadcastBuilder.literal("bossbar")
                .required("color", stringParser())
                .required("message", greedyStringParser())
                .handler { context ->
                    val color = context.get<String>("color")
                    val message = context.get<String>("message")
                    
                    val validColor = try {
                        BossBar.Color.valueOf(color.uppercase())
                    } catch (e: Exception) {
                        null
                    }

                    if (validColor != null) {
                        sendBossBarBroadcast(message, color, null)
                    } else {
                        // Si el primer argumento no es un color válido, es parte del mensaje
                        sendBossBarBroadcast("$color $message", null, null)
                    }
                }
        )

        // 4.3 /broadcast bossbar <duration> <message>
        manager.command(
            broadcastBuilder.literal("bossbar")
                .required("duration", integerParser())
                .required("message", greedyStringParser())
                .handler { context ->
                    val duration = context.get<Int>("duration")
                    val message = context.get<String>("message")
                    sendBossBarBroadcast(message, null, duration)
                }
        )

        // 4.4 /broadcast bossbar <color> <duration> <message>
        manager.command(
            broadcastBuilder.literal("bossbar")
                .required("color", stringParser())
                .required("duration", integerParser())
                .required("message", greedyStringParser())
                .handler { context ->
                    val color = context.get<String>("color")
                    val duration = context.get<Int>("duration")
                    val message = context.get<String>("message")

                    val validColor = try {
                        BossBar.Color.valueOf(color.uppercase())
                    } catch (e: Exception) {
                        null
                    }

                    if (validColor != null) {
                        sendBossBarBroadcast(message, color, duration)
                    } else {
                        // Si el color no es válido, todo (incluido el número) es parte del mensaje
                        sendBossBarBroadcast("$color $duration $message", null, null)
                    }
                }
        )
    }

    private fun sendTitleBroadcast(message: String) {
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

        val nameComp = miniMessage.deserialize(message)
        val bossBar = BossBar.bossBar(nameComp, 1.0f, color, overlay)

        // Mostrar a todos los jugadores online
        Bukkit.getOnlinePlayers().forEach { player ->
            player.showBossBar(bossBar)
        }

        // Destrucción asíncrona segura de la BossBar (Folia-ready)
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { _ ->
            Bukkit.getOnlinePlayers().forEach { player ->
                player.hideBossBar(bossBar)
            }
        }, durationSeconds * 20L)
    }
}
