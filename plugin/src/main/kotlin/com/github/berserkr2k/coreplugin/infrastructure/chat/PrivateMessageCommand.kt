package com.github.berserkr2k.coreplugin.infrastructure.chat

import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry
import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.getChat
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.bukkit.parser.PlayerParser.playerParser
import org.incendo.cloud.parser.standard.StringParser.greedyStringParser
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PrivateMessageCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val profileRegistry: ProfileRegistry,
    private val messagesConfig: MessagesConfig
) {
    private val lastMessagedTarget = ConcurrentHashMap<UUID, UUID>()

    init {
        // Registrar raíces para /msg y todos sus alias para sobreescribir whispers nativos
        val msgRoots = listOf("msg", "w", "tell", "whisper", "pm", "message")
        for (root in msgRoots) {
            manager.command(
                manager.commandBuilder(root)
                    .permission("core.chat.pm")
                    .required("target", playerParser())
                    .required("message", greedyStringParser())
                    .handler { context ->
                        val sender = context.sender()
                        val target = context.get<Player>("target")
                        val message = context.get<String>("message")

                        if (sender is Player && sender.uniqueId == target.uniqueId) {
                            sender.sendMessage(ColorUtility.parse(messagesConfig.getChat("pm-cannot-msg-self")))
                            return@handler
                        }

                        sendPrivateMessage(sender, target, message)
                    }
            )
        }

        // Registrar raíces para /reply y su alias /r
        val replyRoots = listOf("reply", "r")
        for (root in replyRoots) {
            manager.command(
                manager.commandBuilder(root)
                    .permission("core.chat.pm")
                    .required("message", greedyStringParser())
                    .handler { context ->
                        val sender = context.sender()
                        if (sender !is Player) {
                            sender.sendMessage(ColorUtility.parse(messagesConfig.utility["only-players"] ?: "<red>Solo jugadores pueden ejecutar este comando.</red>"))
                            return@handler
                        }

                        val targetUuid = lastMessagedTarget[sender.uniqueId]
                        if (targetUuid == null) {
                            sender.sendMessage(ColorUtility.parse(messagesConfig.getChat("reply-no-target")))
                            return@handler
                        }

                        val target = Bukkit.getPlayer(targetUuid)
                        if (target == null || !target.isOnline) {
                            sender.sendMessage(ColorUtility.parse(messagesConfig.getChat("pm-player-not-found")))
                            return@handler
                        }

                        val message = context.get<String>("message")
                        sendPrivateMessage(sender, target, message)
                    }
            )
        }

        // Registrar comando /socialspy
        manager.command(
            manager.commandBuilder("socialspy")
                .permission("core.chat.socialspy")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        sender.sendMessage(ColorUtility.parse(messagesConfig.utility["only-players"] ?: "<red>Solo jugadores pueden ejecutar este comando.</red>"))
                        return@handler
                    }

                    val profile = profileRegistry.getProfile(sender.uniqueId)
                    if (profile == null) {
                        sender.sendMessage(ColorUtility.parse("<red>Error al cargar tu perfil de usuario.</red>"))
                        return@handler
                    }

                    profile.socialSpy = !profile.socialSpy
                    profile.markDirty()

                    val key = if (profile.socialSpy) "socialspy-enabled" else "socialspy-disabled"
                    val defaultMsg = if (profile.socialSpy) "<green>SocialSpy habilitado.</green>" else "<red>SocialSpy deshabilitado.</red>"
                    val msg = messagesConfig.chat[key] ?: defaultMsg
                    sender.sendMessage(ColorUtility.parse(msg))
                }
        )
    }

    private fun sendPrivateMessage(sender: CommandSender, target: Player, message: String) {
        val senderName = if (sender is Player) {
            val senderProfile = profileRegistry.getProfile(sender.uniqueId)
            val customColor = senderProfile?.chatColor ?: ""
            if (customColor.isNotEmpty()) "$customColor${sender.name}" else sender.name
        } else {
            "Console"
        }

        val targetProfile = profileRegistry.getProfile(target.uniqueId)
        val targetCustomColor = targetProfile?.chatColor ?: ""
        val targetName = if (targetCustomColor.isNotEmpty()) "$targetCustomColor${target.name}" else target.name

        // Enviar al emisor
        val sentMsg = messagesConfig.getChat("pm-sent-format", "target" to targetName, "message" to message)
        sender.sendMessage(ColorUtility.parse(sentMsg))

        // Enviar al receptor
        val receivedMsg = messagesConfig.getChat("pm-received-format", "sender" to senderName, "message" to message)
        target.sendMessage(ColorUtility.parse(receivedMsg))

        // Actualizar el mapa de respuesta rápida si el emisor es un jugador
        if (sender is Player) {
            lastMessagedTarget[sender.uniqueId] = target.uniqueId
            lastMessagedTarget[target.uniqueId] = sender.uniqueId
        }

        // Emitir a los administradores que tengan activo el SocialSpy
        for (spyPlayer in Bukkit.getOnlinePlayers()) {
            if (sender is Player && spyPlayer.uniqueId == sender.uniqueId) continue
            if (spyPlayer.uniqueId == target.uniqueId) continue

            val spyProfile = profileRegistry.getProfile(spyPlayer.uniqueId)
            if (spyProfile != null && spyProfile.socialSpy) {
                val spyMsg = messagesConfig.getChat("pm-socialspy-format", "sender" to sender.name, "target" to target.name, "message" to message)
                spyPlayer.sendMessage(ColorUtility.parse(spyMsg))
            }
        }
    }
}
