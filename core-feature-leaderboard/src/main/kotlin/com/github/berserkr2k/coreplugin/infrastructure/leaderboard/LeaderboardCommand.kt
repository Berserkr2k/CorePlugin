package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.TextDisplay
import org.bukkit.entity.Display
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import net.kyori.adventure.text.minimessage.MiniMessage
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.parser.standard.IntegerParser.integerParser
import org.bukkit.Bukkit

class LeaderboardCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val leaderboardService: LeaderboardService
) {
    private val leaderboardKey = NamespacedKey(plugin, "leaderboard_id")
    private val rankKey = NamespacedKey(plugin, "leaderboard_rank")
    private val miniMessage = MiniMessage.miniMessage()

    init {
        manager.command(
            manager.commandBuilder("core")
                .literal("leaderboard")
                .literal("setup")
                .required("id", stringParser())
                .optional("rank", integerParser())
                .permission("core.leaderboard.setup")
                .handler { context ->
                    val player = context.sender() as? Player ?: return@handler
                    val id = context.get<String>("id")
                    val rank = context.getOrDefault("rank", 1)

                    // Verificamos si mira a un ArmorStand a menos de 5 bloques
                    val target = player.getTargetEntity(5)
                    
                    if (target is ArmorStand) {
                        Bukkit.getRegionScheduler().execute(plugin, target.location) {
                            target.persistentDataContainer.set(leaderboardKey, PersistentDataType.STRING, id)
                            target.persistentDataContainer.set(rankKey, PersistentDataType.INTEGER, rank)
                            
                            // Limpiar antiguos pasajeros displays si los tuviese
                            target.passengers.forEach { it.remove() }
                            
                            leaderboardService.registerLeaderboard(id, rank, target.location).thenRun {
                                player.sendMessage(miniMessage.deserialize("<green>¡ArmorStand registrado con éxito como podio para '$id' (Top $rank)!</green>"))
                                leaderboardService.refreshAllLeaderboards()
                            }
                        }
                    } else {
                        val loc = player.location.clone()
                        Bukkit.getRegionScheduler().execute(plugin, loc) {
                            val stand = loc.world.spawnEntity(loc, EntityType.ARMOR_STAND) as ArmorStand
                            stand.setArms(true)
                            stand.setBasePlate(true)
                            stand.setGravity(false)
                            stand.isCustomNameVisible = false
                            stand.persistentDataContainer.set(leaderboardKey, PersistentDataType.STRING, id)
                            stand.persistentDataContainer.set(rankKey, PersistentDataType.INTEGER, rank)
                            
                            leaderboardService.registerLeaderboard(id, rank, loc).thenRun {
                                player.sendMessage(miniMessage.deserialize("<green>¡Nuevo ArmorStand de podio creado para '$id' (Top $rank)!</green>"))
                                leaderboardService.refreshAllLeaderboards()
                            }
                        }
                    }
                }
        )

        manager.command(
            manager.commandBuilder("core")
                .literal("leaderboard")
                .literal("remove")
                .required("id", stringParser())
                .required("rank", integerParser())
                .permission("core.leaderboard.setup")
                .handler { context ->
                    val player = context.sender() as? Player ?: return@handler
                    val id = context.get<String>("id")
                    val rank = context.get<Int>("rank")

                    leaderboardService.unregisterLeaderboard(id, rank).thenAccept { success ->
                        if (success) {
                            player.sendMessage(miniMessage.deserialize("<green>¡Podio para '$id' (Top $rank) eliminado con éxito!</green>"))
                        } else {
                            player.sendMessage(miniMessage.deserialize("<red>No se encontró ningún podio para la clasificación '$id' con Rank $rank.</red>"))
                        }
                    }
                }
        )

        manager.command(
            manager.commandBuilder("core")
                .literal("leaderboard")
                .literal("reload")
                .permission("core.leaderboard.setup")
                .handler { context ->
                    leaderboardService.reloadLeaderboards().thenRun {
                        context.sender().sendMessage(miniMessage.deserialize("<green>✔ ¡Configuraciones de clasificaciones recargadas y actualizadas con éxito!</green>"))
                    }
                }
        )
    }
}
