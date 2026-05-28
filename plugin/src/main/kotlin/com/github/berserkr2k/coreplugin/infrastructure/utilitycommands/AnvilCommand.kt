package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import net.kyori.adventure.text.minimessage.MiniMessage

class AnvilCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val utilityService: UtilityService,
    private val messagesConfig: MessagesConfig
) {
    private val miniMessage = MiniMessage.miniMessage()

    init {
        manager.command(
            manager.commandBuilder("anvil")
                .permission("core.utility.anvil")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        val msg = messagesConfig.utility["only-players"] ?: "<red>Solo jugadores pueden ejecutar este comando.</red>"
                        sender.sendMessage(miniMessage.deserialize(msg))
                        return@handler
                    }

                    // Abrir yunque virtual usando la localización del jugador para el plano de región
                    sender.openAnvil(sender.location, true)

                    // Reproducir sonido configurado en utility.conf
                    try {
                        val soundName = utilityService.config.anvil.sound
                        val sound = Sound.valueOf(soundName)
                        sender.playSound(sender.location, sound, 1.0f, 1.0f)
                    } catch (e: IllegalArgumentException) {
                        plugin.logger.warning("Sonido inválido en la configuración de utilidades: ${utilityService.config.anvil.sound}. Usando BLOCK_ANVIL_PLACE como fallback.")
                        sender.playSound(sender.location, Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.0f)
                    }

                    val msg = messagesConfig.utility["anvil-opened"] ?: "<green>Abriendo yunque virtual...</green>"
                    sender.sendMessage(miniMessage.deserialize(msg))
                }
        )
    }
}
