package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.IntegerParser.integerParser
import org.incendo.cloud.bukkit.parser.PlayerParser.playerParser

class ExpCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val messageService: MessageService
) {

    init {
        val expBuilder = manager.commandBuilder("exp")

        // 1. /exp get [player]
        manager.command(
            expBuilder.literal("get")
                .optional("target", playerParser())
                .permission("core.utility.exp")
                .handler { context ->
                    val sender = context.sender()
                    val targetOpt = context.optional<Player>("target")

                    val target = if (targetOpt.isPresent) {
                        if (!sender.hasPermission("core.utility.exp.others")) {
                            messageService.send(sender, UtilityMessages.NO_PERMISSION_OTHER)
                            return@handler
                        }
                        targetOpt.get()
                    } else {
                        if (sender !is Player) {
                            messageService.send(sender, UtilityMessages.ONLY_PLAYERS)
                            return@handler
                        }
                        sender
                    }

                    val totalXp = ExperienceMath.getPlayerXp(target)
                    messageService.send(
                        sender,
                        UtilityMessages.EXP_GET,
                        PlaceholderContext.of(
                            TagResolver.resolver(
                                Placeholder.parsed("player", target.name),
                                Placeholder.parsed("level", target.level.toString()),
                                Placeholder.parsed("xp", totalXp.toString())
                            )
                        )
                    )
                }
        )

        // 2. /exp set <player> <amount>
        manager.command(
            expBuilder.literal("set")
                .required("target", playerParser())
                .required("amount", integerParser(0))
                .permission("core.utility.exp.admin")
                .handler { context ->
                    val sender = context.sender()
                    val target = context.get<Player>("target")
                    val amount = context.get<Int>("amount")

                    ExperienceMath.setPlayerXp(target, amount)

                    // Mensaje para el emisor
                    messageService.send(
                        sender,
                        UtilityMessages.EXP_SET,
                        PlaceholderContext.of(
                            TagResolver.resolver(
                                Placeholder.parsed("player", target.name),
                                Placeholder.parsed("amount", amount.toString())
                            )
                        )
                    )

                    // Mensaje para el receptor (si es diferente)
                    if (sender != target) {
                        messageService.send(
                            target,
                            UtilityMessages.EXP_SET_BY_ADMIN,
                            PlaceholderContext.of(Placeholder.parsed("amount", amount.toString()))
                        )
                    }
                }
        )

        // 3. /exp give <player> <amount>
        manager.command(
            expBuilder.literal("give")
                .required("target", playerParser())
                .required("amount", integerParser())
                .permission("core.utility.exp.admin")
                .handler { context ->
                    val sender = context.sender()
                    val target = context.get<Player>("target")
                    val amount = context.get<Int>("amount")

                    val currentXp = ExperienceMath.getPlayerXp(target)
                    val newXp = (currentXp + amount).coerceAtLeast(0)
                    ExperienceMath.setPlayerXp(target, newXp)

                    // Mensaje para el emisor
                    messageService.send(
                        sender,
                        UtilityMessages.EXP_GIVE,
                        PlaceholderContext.of(
                            TagResolver.resolver(
                                Placeholder.parsed("player", target.name),
                                Placeholder.parsed("amount", amount.toString())
                            )
                        )
                    )

                    // Mensaje para el receptor (si es diferente)
                    if (sender != target) {
                        messageService.send(
                            target,
                            UtilityMessages.EXP_GIVE_BY_ADMIN,
                            PlaceholderContext.of(Placeholder.parsed("amount", amount.toString()))
                        )
                    }
                }
        )

        // 4. /exp reset <player>
        manager.command(
            expBuilder.literal("reset")
                .required("target", playerParser())
                .permission("core.utility.exp.admin")
                .handler { context ->
                    val sender = context.sender()
                    val target = context.get<Player>("target")

                    ExperienceMath.setPlayerXp(target, 0)

                    // Mensaje para el emisor
                    messageService.send(
                        sender,
                        UtilityMessages.EXP_RESET,
                        PlaceholderContext.of(Placeholder.parsed("player", target.name))
                    )

                    // Mensaje para el receptor (si es diferente)
                    if (sender != target) {
                        messageService.send(target, UtilityMessages.EXP_RESET_BY_ADMIN)
                    }
                }
        )
    }
}

/**
 * Helper object implementing accurate experience math for Bukkit players.
 */
object ExperienceMath {
    /**
     * Calculates the total amount of XP required to reach a specific level.
     */
    fun getXpForLevel(level: Int): Int {
        return when {
            level <= 15 -> level * level + 6 * level
            level <= 30 -> (2.5 * level * level - 40.5 * level + 360).toInt()
            else -> (4.5 * level * level - 162.5 * level + 2220).toInt()
        }
    }

    /**
     * Calculates the amount of XP needed to progress from the current level to the next.
     */
    fun getXpNeededForNextLevel(level: Int): Int {
        return when {
            level <= 15 -> 2 * level + 7
            level <= 30 -> 5 * level - 38
            else -> 9 * level - 158
        }
    }

    /**
     * Determines the level corresponding to a total amount of XP points.
     */
    fun getLevelFromXp(xp: Int): Int {
        var level = 0
        while (getXpForLevel(level + 1) <= xp) {
            level++
        }
        return level
    }

    /**
     * Sets a player's experience points exactly and accurately updates levels and progress bar.
     */
    fun setPlayerXp(player: Player, totalXp: Int) {
        val safeXp = totalXp.coerceAtLeast(0)
        player.totalExperience = safeXp
        
        val level = getLevelFromXp(safeXp)
        player.level = level
        
        val xpForCurrentLevel = getXpForLevel(level)
        val xpDifference = safeXp - xpForCurrentLevel
        val xpNeededForNext = getXpNeededForNextLevel(level)
        
        player.exp = if (xpNeededForNext > 0) {
            xpDifference.toFloat() / xpNeededForNext.toFloat()
        } else {
            0.0f
        }
    }
    
    /**
     * Resolves the actual, exact total XP points a player currently possesses.
     */
    fun getPlayerXp(player: Player): Int {
        val xpForLevel = getXpForLevel(player.level)
        val xpForProgress = Math.round(player.exp * getXpNeededForNextLevel(player.level)).toInt()
        return xpForLevel + xpForProgress
    }
}
