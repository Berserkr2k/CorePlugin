package com.github.berserkr2k.coreplugin.infrastructure.listeners

import com.github.berserkr2k.coreplugin.domain.chat.ChatConfig
import com.github.berserkr2k.coreplugin.domain.chat.ChatConfig.ChatFormatSection
import com.github.berserkr2k.coreplugin.common.LegacyPlaceholderBridge
import io.papermc.paper.chat.ChatRenderer
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class ModernChatModuleListener(
    private val chatConfig: ChatConfig,
    private val papiBridge: LegacyPlaceholderBridge,
    private val profileRegistry: com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry
) : Listener, ChatRenderer {

    private val systemParser = MiniMessage.miniMessage()

    @EventHandler(priority = EventPriority.HIGH)
    fun onAsyncPlayerChat(event: AsyncChatEvent) {
        event.renderer(this)
    }

    override fun render(
        source: Player, 
        sourceDisplayName: Component, 
        message: Component, 
        viewer: Audience
    ): Component {
        val format = resolveActiveFormat(source)
        val prefixComponent = papiBridge.parseLegacyStringSecurely(source, format.prefix)

        val profile = profileRegistry.getProfile(source.uniqueId)
        val customColor = profile?.chatColor ?: ""
        val nameReplacement = if (customColor.isNotEmpty()) {
            "$customColor${source.name}"
        } else {
            source.name
        }
        val rawNameWithPapi = format.nameFormat.replace("<player>", nameReplacement)
        var nameComponent = papiBridge.parseLegacyStringSecurely(source, rawNameWithPapi)

        if (format.tooltipLines.isNotEmpty()) {
            val hoverBuilder = StringBuilder()
            format.tooltipLines.forEachIndexed { idx, line ->
                hoverBuilder.append(line)
                if (idx < format.tooltipLines.size - 1) hoverBuilder.append("\n")
            }
            val hoverComponent = papiBridge.parseLegacyStringSecurely(source, hoverBuilder.toString())
            nameComponent = nameComponent.hoverEvent(HoverEvent.showText(hoverComponent))
        }

        if (format.clickActionValue.isNotEmpty()) {
            val resolvedValue = format.clickActionValue.replace("<player>", source.name)
            val action = ClickEvent.Action.valueOf(format.clickActionType.uppercase())
            nameComponent = nameComponent.clickEvent(ClickEvent.clickEvent(action, resolvedValue))
        }

        val plainTextMessage = MiniMessage.miniMessage().serialize(message)
        val sanitizedMessageContent = papiBridge.parseLegacyStringSecurely(source, format.chatColor + plainTextMessage)

        return Component.empty()
           .append(prefixComponent)
           .append(nameComponent)
           .append(sanitizedMessageContent)
    }

    private fun resolveActiveFormat(player: Player): ChatFormatSection {
        return chatConfig.formats.entries.asSequence()
           .filter { player.hasPermission("chatformat.${it.key}") || it.key.equals("default", ignoreCase = true) }
           .map { it.value }
           .minByOrNull { it.priority } ?: chatConfig.formats["default"]!!
    }
}
