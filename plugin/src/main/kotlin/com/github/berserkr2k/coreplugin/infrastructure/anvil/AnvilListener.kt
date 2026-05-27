package com.github.berserkr2k.coreplugin.infrastructure.anvil

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.ItemStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import java.util.regex.Pattern

class AnvilListener(private val config: AnvilConfig) : Listener {

    private val colorParser = MiniMessage.builder()
       .tags(TagResolver.builder().resolver(StandardTags.color()).build())
       .build()

    private val styleParser = MiniMessage.builder()
       .tags(TagResolver.builder().resolver(StandardTags.decorations()).build())
       .build()

    @EventHandler
    fun onAnvilPrepare(event: PrepareAnvilEvent) {
        val view = event.view
        val player = view.player as? Player ?: return
        val renameText = view.renameText ?: return

        // 1. Filtrado de Moderación con Regex dinámico desde Config
        if (!player.hasPermission(config.permissionBypass)) {
            val normalizedText = renameText.lowercase()
               .replace("0", "o").replace("1", "i")
               .replace("3", "e").replace("4", "a")
                
            val escapedWords = config.blacklistWords.map { Pattern.quote(it) }
            if (escapedWords.isNotEmpty()) {
                val regexPattern = Pattern.compile("(?i)\\b(${escapedWords.joinToString("|")})\\b")
                if (regexPattern.matcher(normalizedText).find()) {
                    event.result = null
                    return
                }
            }
        }

        // 2. Procesamiento de formato basado en permisos estandarizados
        val resultItem: ItemStack = event.result ?: return
        val meta = resultItem.itemMeta ?: return

        val formattedDisplayName: Component = when {
            player.hasPermission(config.permissionColor) -> {
                if (renameText.contains("<") && renameText.contains(">")) {
                    MiniMessage.miniMessage().deserialize(renameText)
                } else {
                    net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(renameText)
                }
            }
            player.hasPermission(config.permissionStyle) -> {
                if (renameText.contains("&")) {
                    net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(renameText)
                } else {
                    styleParser.deserialize(renameText)
                }
            }
            else -> {
                Component.text(renameText)
            }
        }

        meta.displayName(formattedDisplayName)
        resultItem.itemMeta = meta
        
        view.repairCost = maxOf(view.repairCost, config.minRepairCost)
        event.result = resultItem
    }
}
