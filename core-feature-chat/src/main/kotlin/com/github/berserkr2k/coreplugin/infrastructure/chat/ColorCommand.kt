package com.github.berserkr2k.coreplugin.infrastructure.chat

import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.common.gui.CustomMenu
import com.github.berserkr2k.coreplugin.common.gui.ItemBuilder
import com.github.berserkr2k.coreplugin.common.gui.toItemStack
import com.github.berserkr2k.coreplugin.infrastructure.config.ItemConfig
import com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.getChat
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.scheduler.RegionTaskScheduler

class ColorCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val profileRegistry: ProfileRegistry,
    private val configManager: ModularConfigManager,
    private val messagesConfig: MessagesConfig,
    private val serviceRegistry: ServiceRegistry
) {
    private val regionTaskScheduler = serviceRegistry.get(RegionTaskScheduler::class.java)
    private var menuConfig = ColorMenuConfig()

    init {
        configManager.loadModuleConfig("menus/color-selector.conf", ColorMenuConfig::class.java, ColorMenuConfig())
            .thenAccept { this.menuConfig = it }

        manager.command(
            manager.commandBuilder("color")
                .permission("core.chat.color.use")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        sender.sendMessage(ColorUtility.parse(messagesConfig.utility["only-players"] ?: "<red>Solo jugadores pueden ejecutar este comando.</red>"))
                        return@handler
                    }

                    openColorMenu(sender)
                }
        )
    }

    fun reload() {
        configManager.loadModuleConfig("menus/color-selector.conf", ColorMenuConfig::class.java, ColorMenuConfig())
            .thenAccept { this.menuConfig = it }
    }

    private fun openColorMenu(player: Player) {
        val profile = profileRegistry.getProfile(player.uniqueId)
        if (profile == null) {
            player.sendMessage(ColorUtility.parse("<red>Error al cargar tu perfil de usuario.</red>"))
            return
        }

        val menuTitle = ColorUtility.parse(menuConfig.title)
        val menu = CustomMenu(menuTitle, menuConfig.size, plugin)

        // 1. Relleno de fondo si está activo
        if (menuConfig.filler.enabled) {
            val fillerItem = menuConfig.filler.item.toItemStack()
            for (i in 0 until menuConfig.size) {
                menu.setItem(i, fillerItem.clone())
            }
        }

        // 2. Registrar ítems de color
        for (option in menuConfig.colors) {
            val hasPerm = player.hasPermission("core.chat.color.${option.id}") || player.hasPermission("core.chat.color.all")
            val isActive = profile.chatColor == option.format

            val materialEnum = Material.matchMaterial(option.material) ?: Material.STONE

            val loreLines = mutableListOf<String>()
            loreLines.add(" ")
            if (isActive) {
                loreLines.add("<green>⭐ ¡Color Equipado!</green>")
                loreLines.add("<gray>Tu nombre en el chat ya tiene este color.</gray>")
            } else if (hasPerm) {
                loreLines.add("<yellow>⚡ Click para Seleccionar</yellow>")
            } else {
                loreLines.add("<red>❌ Bloqueado</red>")
                loreLines.add("<gray>Requiere permiso: <red>core.chat.color.${option.id}</red></gray>")
            }

            val item = ItemBuilder(materialEnum)
                .displayName(option.displayName)
                .lore(loreLines)
                .glow(isActive)
                .build()

            if (option.slot in 0 until menuConfig.size) {
                menu.setItem(option.slot, item) { p, _ ->
                    if (isActive) {
                        p.playSound(p.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f)
                        return@setItem
                    }

                    if (hasPerm) {
                        profile.chatColor = option.format
                        profile.markDirty()
                        p.playSound(p.location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f)
                        
                        val colorMsg = messagesConfig.getChat("color-changed", "color" to option.displayName)
                        p.sendMessage(ColorUtility.parse(colorMsg))
                        
                        // Reabrir regionalmente para refrescar estado en el mismo tick regional
                        regionTaskScheduler.runAtLocation(p.location, Runnable {
                            openColorMenu(p)
                        })
                    } else {
                        p.playSound(p.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f)
                        p.sendMessage(ColorUtility.parse("<red>No tienes permisos para usar este color.</red>"))
                    }
                }
            }
        }

        // 3. Botón de restablecer
        val isResetActive = profile.chatColor == null || profile.chatColor!!.isEmpty()
        val resetSlot = menuConfig.resetItemSlot
        
        val finalLore = menuConfig.resetItem.lore.toMutableList()
        if (finalLore.isNotEmpty()) {
            finalLore.add(if (isResetActive) "<green>⭐ Ya restablecido</green>" else "<yellow>⚡ Click para restablecer</yellow>")
        }
        val finalResetItem = ItemBuilder.fromConfig(menuConfig.resetItem).lore(finalLore).build()

        if (resetSlot in 0 until menuConfig.size) {
            menu.setItem(resetSlot, finalResetItem) { p, _ ->
                if (isResetActive) {
                    p.playSound(p.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f)
                    return@setItem
                }

                profile.chatColor = null
                profile.markDirty()
                p.playSound(p.location, Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.5f)
                p.sendMessage(ColorUtility.parse("<yellow>Has restablecido tu color de chat al valor por defecto.</yellow>"))
                
                regionTaskScheduler.runAtLocation(p.location, Runnable {
                    openColorMenu(p)
                })
            }
        }

        menu.open(player)
    }
}
