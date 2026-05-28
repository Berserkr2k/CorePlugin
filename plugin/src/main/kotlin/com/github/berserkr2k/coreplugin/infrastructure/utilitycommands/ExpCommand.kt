package com.github.berserkr2k.coreplugin.infrastructure.utilitycommands

import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.IntegerParser.integerParser
import org.incendo.cloud.bukkit.parser.PlayerParser.playerParser
import net.kyori.adventure.text.minimessage.MiniMessage

class ExpCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val messagesConfig: MessagesConfig
) {
    private val miniMessage = object {
        fun deserialize(text: String) = com.github.berserkr2k.coreplugin.common.ColorUtility.parse(text)
    }

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
                            val msg = messagesConfig.utility["no-permission-other"] ?: "<red>No tienes permiso para aplicar esto a otros jugadores.</red>"
                            sender.sendMessage(miniMessage.deserialize(msg))
                            return@handler
                        }
                        targetOpt.get()
                    } else {
                        if (sender !is Player) {
                            val msg = messagesConfig.utility["only-players"] ?: "<red>Solo jugadores pueden ejecutar este comando.</red>"
                            sender.sendMessage(miniMessage.deserialize(msg))
                            return@handler
                        }
                        sender
                    }

                    val totalXp = ExperienceMath.getPlayerXp(target)
                    val key = "exp-get"
                    val defaultMsg = "<gray>Experiencia de <white><player></white>: </gray><green><level> niveles</green> <gray>(<xp> XP totales)</gray>"
                    val msg = (messagesConfig.utility[key] ?: defaultMsg)
                        .replace("<player>", target.name)
                        .replace("<level>", target.level.toString())
                        .replace("<xp>", totalXp.toString())
                    sender.sendMessage(miniMessage.deserialize(msg))
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
                    val keySender = "exp-set"
                    val defaultSender = "<green>Has establecido la experiencia de <player> en <amount> XP.</green>"
                    val msgSender = (messagesConfig.utility[keySender] ?: defaultSender)
                        .replace("<player>", target.name)
                        .replace("<amount>", amount.toString())
                    sender.sendMessage(miniMessage.deserialize(msgSender))

                    // Mensaje para el receptor (si es diferente)
                    if (sender != target) {
                        val keyReceiver = "exp-set-by-admin"
                        val defaultReceiver = "<green>Tu experiencia ha sido establecida en <amount> XP.</green>"
                        val msgReceiver = (messagesConfig.utility[keyReceiver] ?: defaultReceiver)
                            .replace("<amount>", amount.toString())
                        target.sendMessage(miniMessage.deserialize(msgReceiver))
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
                    val keySender = "exp-give"
                    val defaultSender = "<green>Has dado <amount> XP a <player>.</green>"
                    val msgSender = (messagesConfig.utility[keySender] ?: defaultSender)
                        .replace("<player>", target.name)
                        .replace("<amount>", amount.toString())
                    sender.sendMessage(miniMessage.deserialize(msgSender))

                    // Mensaje para el receptor (si es diferente)
                    if (sender != target) {
                        val keyReceiver = "exp-give-by-admin"
                        val defaultReceiver = "<green>Has recibido <amount> XP.</green>"
                        val msgReceiver = (messagesConfig.utility[keyReceiver] ?: defaultReceiver)
                            .replace("<amount>", amount.toString())
                        target.sendMessage(miniMessage.deserialize(msgReceiver))
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
                    val keySender = "exp-reset"
                    val defaultSender = "<green>Has restablecido la experiencia de <player> a 0.</green>"
                    val msgSender = (messagesConfig.utility[keySender] ?: defaultSender)
                        .replace("<player>", target.name)
                    sender.sendMessage(miniMessage.deserialize(msgSender))

                    // Mensaje para el receptor (si es diferente)
                    if (sender != target) {
                        val keyReceiver = "exp-reset-by-admin"
                        val defaultReceiver = "<red>Tu experiencia ha sido restablecida a 0.</red>"
                        val msgReceiver = messagesConfig.utility[keyReceiver] ?: defaultReceiver
                        target.sendMessage(miniMessage.deserialize(msgReceiver))
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
