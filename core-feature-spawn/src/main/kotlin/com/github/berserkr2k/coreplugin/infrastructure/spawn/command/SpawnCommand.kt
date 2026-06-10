package com.github.berserkr2k.coreplugin.infrastructure.spawn.command

import com.github.berserkr2k.coreplugin.infrastructure.spawn.service.SpawnService
import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.getSpawn
import com.github.berserkr2k.coreplugin.common.ColorUtility
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.CommandManager

class SpawnCommand(
    private val commandManager: CommandManager<CommandSender>,
    private val spawnService: SpawnService,
    private val messagesConfig: MessagesConfig
) {
    init {
        registerCommands()
    }

    private fun registerCommands() {
        commandManager.command(
            commandManager.commandBuilder("spawn")
                .permission("core.spawn.use")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        sender.sendMessage("Solo jugadores pueden ir al spawn.")
                        return@handler
                    }
                    spawnService.teleportToSpawn(sender)
                }
        )

        commandManager.command(
            commandManager.commandBuilder("setspawn")
                .permission("core.spawn.setup")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        sender.sendMessage("Solo jugadores pueden establecer el spawn.")
                        return@handler
                    }
                    spawnService.setSpawnLocation(sender.location)
                    sender.sendMessage(ColorUtility.parse(messagesConfig.getSpawn("set-success")))
                }
        )
    }
}
