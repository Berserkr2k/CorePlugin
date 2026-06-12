package com.github.berserkr2k.coreplugin.infrastructure.spawn.command

import com.github.berserkr2k.coreplugin.infrastructure.spawn.service.SpawnService
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.CoreMessages
import com.github.berserkr2k.coreplugin.infrastructure.spawn.SpawnMessages
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.CommandManager

class SpawnCommand(
    private val commandManager: CommandManager<CommandSender>,
    private val spawnService: SpawnService,
    private val messageService: MessageService
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
                        messageService.send(sender, CoreMessages.ONLY_PLAYERS)
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
                        messageService.send(sender, CoreMessages.ONLY_PLAYERS)
                        return@handler
                    }
                    val loc = sender.location
                    val centeredX = loc.blockX + 0.5
                    val centeredY = loc.blockY.toDouble()
                    val centeredZ = loc.blockZ + 0.5
                    val snappedYaw = (Math.round(loc.yaw / 45.0) * 45.0).toFloat()
                    val snappedPitch = (Math.round(loc.pitch / 45.0) * 45.0).toFloat()
                    
                    val centeredLoc = org.bukkit.Location(loc.world, centeredX, centeredY, centeredZ, snappedYaw, snappedPitch)
                    spawnService.setSpawnLocation(centeredLoc)
                    messageService.send(sender, SpawnMessages.SET_SUCCESS)
                }
        )
    }
}
