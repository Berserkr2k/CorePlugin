package com.github.berserkr2k.coreplugin.infrastructure.chat

import com.github.berserkr2k.coreplugin.domain.chat.ChatConfig
import com.github.berserkr2k.coreplugin.domain.chat.ChatConfig.ChatFormatSection
import com.github.berserkr2k.coreplugin.common.LegacyPlaceholderBridge
import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService
import com.github.berserkr2k.coreplugin.api.core.state.StateContainer
import com.github.berserkr2k.coreplugin.api.core.state.StateContainerType
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.CoreMessages
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import io.papermc.paper.chat.ChatRenderer
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.util.regex.Pattern

class ChatStateContainer(
    var lastMessageTime: Long = 0L
) : StateContainer

val CHAT_STATE_TYPE = StateContainerType { ChatStateContainer() }

class ModernChatModuleListener(
    @Volatile var chatConfig: ChatConfig,
    private val papiBridge: LegacyPlaceholderBridge,
    private val profileRegistry: com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry,
    private val stateService: PlayerStateService,
    private val messageService: MessageService
) : Listener, ChatRenderer {

    private val LINK_PATTERN = Pattern.compile(
        "(https?://)?([a-zA-Z0-9\\-]+\\.)+[a-zA-Z]{2,6}(:[0-9]+)?(/\\S*)?|\\b(?:\\d{1,3}\\.){3}\\d{1,3}(:[0-9]+)?\\b",
        Pattern.CASE_INSENSITIVE
    )

    @EventHandler(priority = EventPriority.HIGH)
    fun onAsyncPlayerChat(event: AsyncChatEvent) {
        val player = event.player
        val plainTextMsg = PlainTextComponentSerializer.plainText().serialize(event.message())
        
        val filtered = filterMessage(player, plainTextMsg)
        if (filtered == null) {
            event.isCancelled = true
            return
        }
        
        if (filtered != plainTextMsg) {
            event.message(Component.text(filtered))
        }
        
        event.renderer(this)
    }

    private fun filterMessage(player: Player, rawMessage: String): String? {
        val filter = chatConfig.filter
        if (!filter.enabled) return rawMessage

        // 1. Cooldown Check
        val state = stateService.getContainer(player.uniqueId, CHAT_STATE_TYPE)
        val now = System.currentTimeMillis()
        if (filter.cooldownSeconds > 0.0 && !player.hasPermission("core.chat.bypass.cooldown")) {
            val elapsed = now - state.lastMessageTime
            val required = (filter.cooldownSeconds * 1000).toLong()
            if (elapsed < required) {
                val remaining = (required - elapsed) / 1000.0
                messageService.send(
                    player,
                    CoreMessages.CHAT_COOLDOWN,
                    PlaceholderContext.of(Placeholder.parsed("cooldown", String.format("%.1f", remaining)))
                )
                return null
            }
        }

        // 2. Link Blocker
        if (filter.blockLinks && !player.hasPermission("core.chat.bypass.links")) {
            if (LINK_PATTERN.matcher(rawMessage).find()) {
                messageService.send(player, CoreMessages.CHAT_LINK_BLOCKED)
                return null
            }
        }

        // Message is allowed to be sent; update the last message timestamp
        state.lastMessageTime = now

        var processed = rawMessage

        // 3. Caps Blocker
        if (filter.preventCaps && !player.hasPermission("core.chat.bypass.caps")) {
            if (processed.length >= filter.capsMinLength) {
                var upperCount = 0
                var letterCount = 0
                for (char in processed) {
                    if (char.isLetter()) {
                        letterCount++
                        if (char.isUpperCase()) {
                            upperCount++
                        }
                    }
                }
                if (letterCount > 0) {
                    val pct = (upperCount * 100) / letterCount
                    if (pct >= filter.capsPercentage) {
                        processed = processed.lowercase()
                    }
                }
            }
        }

        // 4. Bad Words Filter (Swears)
        if (filter.blockedWords.isNotEmpty() && !player.hasPermission("core.chat.bypass.swears")) {
            for (word in filter.blockedWords) {
                val escapedWord = Pattern.quote(word)
                // Use word boundaries (?i)\bword\b for exact match, case-insensitive
                val pattern = Pattern.compile("(?i)\\b$escapedWord\\b")
                processed = pattern.matcher(processed).replaceAll(filter.swearReplacement)
            }
        }

        return processed
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
                val replacedLine = line.replace("<player>", source.name)
                hoverBuilder.append(replacedLine)
                if (idx < format.tooltipLines.size - 1) hoverBuilder.append("\n")
            }
            val hoverComponent = papiBridge.parseLegacyStringSecurely(source, hoverBuilder.toString())
            nameComponent = nameComponent.hoverEvent(HoverEvent.showText(hoverComponent))
        }

        if (format.clickActionValue.isNotEmpty()) {
            val resolvedValue = format.clickActionValue.replace("<player>", source.name)
            val action = ClickEvent.Action.valueOf(format.clickActionType.uppercase())
            val clickEvent = when (action) {
                ClickEvent.Action.RUN_COMMAND -> ClickEvent.runCommand(resolvedValue)
                ClickEvent.Action.SUGGEST_COMMAND -> ClickEvent.suggestCommand(resolvedValue)
                ClickEvent.Action.OPEN_URL -> ClickEvent.openUrl(resolvedValue)
                ClickEvent.Action.COPY_TO_CLIPBOARD -> ClickEvent.copyToClipboard(resolvedValue)
                else -> @Suppress("DEPRECATION") ClickEvent.clickEvent(action, resolvedValue)
            }
            nameComponent = nameComponent.clickEvent(clickEvent)
        }

        val plainTextMessage = PlainTextComponentSerializer.plainText().serialize(message)
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
