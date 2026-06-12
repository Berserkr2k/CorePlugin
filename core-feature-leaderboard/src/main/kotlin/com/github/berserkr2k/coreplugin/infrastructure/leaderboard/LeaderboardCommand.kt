package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.parser.standard.IntegerParser.integerParser
import com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.feature.leaderboard.LeaderboardService
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import com.github.berserkr2k.coreplugin.api.protection.permissions.Permissions

class LeaderboardCommand(
    private val manager: CommandManager<CommandSender>,
    private val leaderboardService: LeaderboardService,
    private val messageService: MessageService
) {
    private val registry = org.bukkit.Bukkit.getServicesManager().load(com.github.berserkr2k.coreplugin.api.di.ServiceRegistry::class.java)
        ?: throw IllegalStateException("ServiceRegistry not found in ServicesManager")
    private val plugin = registry.get(Plugin::class.java)
    private val leaderboardKey = NamespacedKey(plugin, "leaderboard_id")
    private val rankKey = NamespacedKey(plugin, "leaderboard_rank")
    private val regionTaskScheduler = registry.get(RegionTaskScheduler::class.java)

    init {
        manager.command(
            manager.commandBuilder("core")
                .literal("leaderboard")
                .literal("setup")
                .required("id", stringParser())
                .optional("rank", integerParser())
                .permission(Permissions.LEADERBOARD_SETUP)
                .handler { context ->
                    val player = context.sender() as? Player ?: return@handler
                    val id = context.get<String>("id")
                    val rank = context.getOrDefault("rank", 1)

                    // Verificamos si mira a un ArmorStand a menos de 5 bloques
                    val target = player.getTargetEntity(5)
                    
                    if (target is ArmorStand) {
                        regionTaskScheduler.runAtLocation(target.location, Runnable {
                            target.persistentDataContainer.set(leaderboardKey, PersistentDataType.STRING, id)
                            target.persistentDataContainer.set(rankKey, PersistentDataType.INTEGER, rank)
                            
                            // Limpiar antiguos pasajeros displays si los tuviese
                            target.passengers.forEach { it.remove() }
                            
                            leaderboardService.registerLeaderboard(id, rank, target.location).thenRun {
                                messageService.send(
                                    player,
                                    LeaderboardMessages.REGISTERED,
                                    PlaceholderContext.of("id" to id, "rank" to rank.toString())
                                )
                                leaderboardService.refreshAllLeaderboards()
                            }
                        })
                    } else {
                        val loc = player.location.clone()
                        regionTaskScheduler.runAtLocation(loc, Runnable {
                            val stand = loc.world.spawnEntity(loc, EntityType.ARMOR_STAND) as ArmorStand
                            stand.setArms(true)
                            stand.setBasePlate(true)
                            stand.setGravity(false)
                            stand.isCustomNameVisible = false
                            stand.persistentDataContainer.set(leaderboardKey, PersistentDataType.STRING, id)
                            stand.persistentDataContainer.set(rankKey, PersistentDataType.INTEGER, rank)
                            
                            leaderboardService.registerLeaderboard(id, rank, loc).thenRun {
                                messageService.send(
                                    player,
                                    LeaderboardMessages.CREATED,
                                    PlaceholderContext.of("id" to id, "rank" to rank.toString())
                                )
                                leaderboardService.refreshAllLeaderboards()
                            }
                        })
                    }
                }
        )

        manager.command(
            manager.commandBuilder("core")
                .literal("leaderboard")
                .literal("remove")
                .required("id", stringParser())
                .required("rank", integerParser())
                .permission(Permissions.LEADERBOARD_SETUP)
                .handler { context ->
                    val player = context.sender() as? Player ?: return@handler
                    val id = context.get<String>("id")
                    val rank = context.get<Int>("rank")

                    leaderboardService.unregisterLeaderboard(id, rank).thenAccept { success ->
                        if (success) {
                            messageService.send(
                                player,
                                LeaderboardMessages.DELETED,
                                PlaceholderContext.of("id" to id, "rank" to rank.toString())
                            )
                        } else {
                            messageService.send(
                                player,
                                LeaderboardMessages.NOT_FOUND,
                                PlaceholderContext.of("id" to id, "rank" to rank.toString())
                            )
                        }
                    }
                }
        )

        manager.command(
            manager.commandBuilder("core")
                .literal("leaderboard")
                .literal("reload")
                .permission(Permissions.LEADERBOARD_SETUP)
                .handler { context ->
                    leaderboardService.reloadLeaderboards().thenRun {
                        messageService.send(context.sender(), LeaderboardMessages.RELOADED)
                    }
                }
        )
    }
}
