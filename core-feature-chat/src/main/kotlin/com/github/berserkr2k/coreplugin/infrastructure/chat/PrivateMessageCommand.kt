package com.github.berserkr2k.coreplugin.infrastructure.chat

import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.CoreMessages
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
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
    private val messageService: MessageService
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
                            messageService.send(sender, CoreMessages.CHAT_PM_CANNOT_MSG_SELF)
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
                            messageService.send(sender, CoreMessages.ONLY_PLAYERS)
                            return@handler
                        }

                        val targetUuid = lastMessagedTarget[sender.uniqueId]
                        if (targetUuid == null) {
                            messageService.send(sender, CoreMessages.CHAT_REPLY_NO_TARGET)
                            return@handler
                        }

                        val target = Bukkit.getPlayer(targetUuid)
                        if (target == null || !target.isOnline) {
                            messageService.send(sender, CoreMessages.CHAT_PM_PLAYER_NOT_FOUND)
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
                        messageService.send(sender, CoreMessages.ONLY_PLAYERS)
                        return@handler
                    }

                    val profile = profileRegistry.getProfile(sender.uniqueId)
                    if (profile == null) {
                        messageService.send(sender, CoreMessages.CHAT_PROFILE_ERROR)
                        return@handler
                    }

                    profile.socialSpy = !profile.socialSpy
                    profile.markDirty()

                    val msgKey = if (profile.socialSpy) CoreMessages.CHAT_SOCIALSPY_ENABLED else CoreMessages.CHAT_SOCIALSPY_DISABLED
                    messageService.send(sender, msgKey)
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
        messageService.send(
            sender,
            CoreMessages.CHAT_PM_SENT_FORMAT,
            PlaceholderContext.of(
                TagResolver.resolver(
                    Placeholder.parsed("target", targetName),
                    Placeholder.parsed("message", message)
                )
            )
        )

        // Enviar al receptor
        messageService.send(
            target,
            CoreMessages.CHAT_PM_RECEIVED_FORMAT,
            PlaceholderContext.of(
                TagResolver.resolver(
                    Placeholder.parsed("sender", senderName),
                    Placeholder.parsed("message", message)
                )
            )
        )

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
                messageService.send(
                    spyPlayer,
                    CoreMessages.CHAT_PM_SOCIALSPY_FORMAT,
                    PlaceholderContext.of(
                        TagResolver.resolver(
                            Placeholder.parsed("sender", sender.name),
                            Placeholder.parsed("target", target.name),
                            Placeholder.parsed("message", message)
                        )
                    )
                )
            }
        }
    }
}
