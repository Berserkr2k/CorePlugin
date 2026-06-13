package com.github.berserkr2k.coreplugin.infrastructure.chat

import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.api.framework.menu.*
import com.github.berserkr2k.coreplugin.api.framework.item.*
import com.github.berserkr2k.coreplugin.api.config.ItemConfig
import com.github.berserkr2k.coreplugin.api.core.user.ProfileRegistry
import com.github.berserkr2k.coreplugin.api.core.config.ConfigService
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.CoreMessages
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import com.github.berserkr2k.coreplugin.infrastructure.chat.ChatMessages
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler

class ColorCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val profileRegistry: ProfileRegistry,
    private val configService: com.github.berserkr2k.coreplugin.api.core.config.ConfigService,
    private val messageService: MessageService,
    private val registry: ServiceRegistry
) {
    private val folderProvider = registry.get(com.github.berserkr2k.coreplugin.api.core.filesystem.FeatureFolderProvider::class.java)!!
    private val menuService = registry.get(com.github.berserkr2k.coreplugin.api.framework.menu.MenuService::class.java)!!
    private val itemBuilderFactory = registry.get(com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilderFactory::class.java)!!
    private val regionTaskScheduler = registry.get(com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler::class.java)!!
    private var menuConfig = ColorMenuConfig()

    private fun loadConfig() {
        val file = folderProvider.getFeatureConfigFolder("chat").resolve("color-selector.conf").toFile()
        this.menuConfig = configService.loadConfig(file, ColorMenuConfig::class.java, ColorMenuConfig())
    }

    init {
        loadConfig()

        manager.command(
            manager.commandBuilder("color")
                .permission("core.chat.color.use")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        messageService.send(sender, CoreMessages.ONLY_PLAYERS)
                        return@handler
                     }

                     openColorMenu(sender)
                }
        )
    }

    fun reload() {
        try {
            loadConfig()
        } catch (e: Exception) {
            plugin.logger.severe("Error al recargar color-selector.conf: ${e.message}")
        }
    }

    private fun openColorMenu(player: Player) {
        val profile = profileRegistry.getProfile(player.uniqueId)
        if (profile == null) {
            messageService.send(player, ChatMessages.CHAT_PROFILE_ERROR)
            return
        }

        val menuTitle = ColorUtility.parse(menuConfig.title)
        val builder = menuService.createBuilder()
            .title(menuTitle)
            .slots(menuConfig.size)

        // 1. Relleno de fondo si está activo
        if (menuConfig.filler.enabled) {
            val fillerItem = itemBuilderFactory.builder(menuConfig.filler.item).build()
            val fillerButton = Button.builder().icon(fillerItem).build()
            builder.fill(fillerButton)
        }

        // 2. Registrar ítems de color
        for (option in menuConfig.colors) {
            val hasPerm = player.hasPermission("core.chat.color.${option.id}") || player.hasPermission("core.chat.color.all")
            val isActive = profile.chatColor == option.format

            val materialEnum = Material.matchMaterial(option.material) ?: Material.STONE

            val loreLines = mutableListOf<String>()
            if (isActive) {
                loreLines.add(" ")
                loreLines.add(messageService.getRawTemplate(ChatMessages.GUI_COLOR_EQUIPPED).ifEmpty { "<green>⭐ ¡Color Equipado!</green>" })
                loreLines.add(messageService.getRawTemplate(ChatMessages.GUI_COLOR_EQUIPPED_LORE).ifEmpty { "<gray>Tu nombre en el chat ya tiene este color.</gray>" })
            } else if (hasPerm) {
                loreLines.add(messageService.getRawTemplate(ChatMessages.GUI_COLOR_SELECT).ifEmpty { "<yellow>⚡ Click para Seleccionar</yellow>" })
            } else {
                loreLines.add(messageService.getRawTemplate(ChatMessages.GUI_COLOR_LOCKED).ifEmpty { "<red>❌ Bloqueado</red>" })
                loreLines.add(messageService.getRawTemplate(ChatMessages.GUI_COLOR_LOCKED_LORE).ifEmpty { "<gray>Requiere permiso: <red>core.chat.color.<id></red></gray>" }.replace("<id>", option.id))
            }

            val item = itemBuilderFactory.builder(materialEnum)
                .displayName(option.displayName)
                .lore(loreLines)
                .glow(isActive)
                .build()

            if (option.slot in 0 until menuConfig.size) {
                val btn = Button.builder()
                    .icon(item)
                    .onClick { p ->
                        if (isActive) {
                            p.playSound(p.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f)
                            return@onClick
                        }

                        if (hasPerm) {
                            profile.chatColor = option.format
                            profile.markDirty()
                            p.playSound(p.location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f)
                            
                            messageService.send(
                                p,
                                ChatMessages.CHAT_COLOR_CHANGED,
                                PlaceholderContext.of(Placeholder.parsed("color", option.displayName))
                            )
                            
                            // Reabrir regionalmente para refrescar estado en el mismo tick regional
                            regionTaskScheduler.runAtLocation(p.location, Runnable {
                                openColorMenu(p)
                            })
                        } else {
                            p.playSound(p.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f)
                            messageService.send(p, ChatMessages.CHAT_COLOR_NO_PERMISSION)
                        }
                    }
                    .build()
                builder.button(option.slot, btn)
            }
        }

        // 3. Botón de restablecer
        val isResetActive = profile.chatColor == null || profile.chatColor!!.isEmpty()
        val resetSlot = menuConfig.resetItemSlot
        
        val finalLore = menuConfig.resetItem.lore.toMutableList()
        if (finalLore.isNotEmpty()) {
            val resetActiveMsg = messageService.getRawTemplate(ChatMessages.GUI_COLOR_RESET_ACTIVE).ifEmpty { "<green>⭐ Ya restablecido</green>" }
            val resetClickMsg = messageService.getRawTemplate(ChatMessages.GUI_COLOR_RESET_CLICK).ifEmpty { "<yellow>⚡ Click para restablecer</yellow>" }
            finalLore.add(if (isResetActive) resetActiveMsg else resetClickMsg)
        }
        val finalResetItem = itemBuilderFactory.builder(menuConfig.resetItem).lore(finalLore).build()

        if (resetSlot in 0 until menuConfig.size) {
            val resetBtn = Button.builder()
                .icon(finalResetItem)
                .onClick { p ->
                    if (isResetActive) {
                        p.playSound(p.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f)
                        return@onClick
                    }

                    profile.chatColor = null
                    profile.markDirty()
                    p.playSound(p.location, Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.5f)
                    messageService.send(p, ChatMessages.CHAT_COLOR_RESET)
                    
                    regionTaskScheduler.runAtLocation(p.location, Runnable {
                        openColorMenu(p)
                    })
                }
                .build()
            builder.button(resetSlot, resetBtn)
        }

        builder.build().open(player)
    }
}
