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
                            
                            val display = target.passengers.filterIsInstance<TextDisplay>().firstOrNull()
                                ?: (target.world.spawnEntity(target.location.clone().add(0.0, 2.1, 0.0), EntityType.TEXT_DISPLAY) as TextDisplay).also {
                                    it.setGravity(false)
                                    it.billboard = Display.Billboard.CENTER
                                    target.addPassenger(it)
                                }
                            
                            display.text(miniMessage.deserialize("<gold>Cargando podio...</gold>"))
                            
                            leaderboardService.registerLeaderboard(id, rank, target.location).thenRun {
                                leaderboardService.activeArmorStands["${id}_$rank"] = target.uniqueId
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
                            
                            val display = loc.world.spawnEntity(loc.clone().add(0.0, 2.1, 0.0), EntityType.TEXT_DISPLAY) as TextDisplay
                            display.setGravity(false)
                            display.billboard = Display.Billboard.CENTER
                            display.text(miniMessage.deserialize("<gold>Cargando podio...</gold>"))
                            
                            stand.addPassenger(display)
                            
                            leaderboardService.registerLeaderboard(id, rank, loc).thenRun {
                                leaderboardService.activeArmorStands["${id}_$rank"] = stand.uniqueId
                                player.sendMessage(miniMessage.deserialize("<green>¡Nuevo ArmorStand de podio creado para '$id' (Top $rank)!</green>"))
                                leaderboardService.refreshAllLeaderboards()
                            }
                        }
                    }
                }
        )
    }
}
