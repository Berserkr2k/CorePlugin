package com.github.berserkr2k.coreplugin.infrastructure.regions.command

import com.github.berserkr2k.coreplugin.api.regions.CompiledRegion
import com.github.berserkr2k.coreplugin.api.regions.RegionFlags
import com.github.berserkr2k.coreplugin.infrastructure.regions.service.RegionManager
import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.getRegions
import com.github.berserkr2k.coreplugin.common.ColorUtility
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.IntegerParser
import org.incendo.cloud.parser.standard.StringParser

class RegionCommand(
    private val plugin: Plugin,
    private val commandManager: CommandManager<CommandSender>,
    private val session: PlayerSelectionSession,
    private val regionManager: RegionManager,
    private val messagesConfig: MessagesConfig
) {

    init {
        registerCommands()
    }

    private fun getMsg(key: String, vararg placeholders: Pair<String, Any>): String {
        return messagesConfig.getRegions(key, *placeholders)
    }

    private fun registerCommands() {
        val regionBuilder = commandManager.commandBuilder("region")
            .permission("core.region.setup")

        // 1. /region create <id> <priority>
        commandManager.command(
            regionBuilder.literal("create")
                .required("id", StringParser.stringParser())
                .required("priority", IntegerParser.integerParser())
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        sender.sendMessage("Solo jugadores pueden ejecutar este comando.")
                        return@handler
                    }

                    val id = context.get<String>("id").lowercase()
                    val priority = context.get<Int>("priority")
                    val selection = session.getSelection(sender.uniqueId)

                    val pos1 = selection.pos1
                    val pos2 = selection.pos2
                    if (pos1 == null || pos2 == null) {
                        sender.sendMessage(ColorUtility.parse(getMsg("selection-incomplete")))
                        return@handler
                    }

                    sender.sendMessage(ColorUtility.parse(getMsg("compiling")))

                    val minX = minOf(pos1.blockX, pos2.blockX)
                    val minY = minOf(pos1.blockY, pos2.blockY)
                    val minZ = minOf(pos1.blockZ, pos2.blockZ)
                    val maxX = maxOf(pos1.blockX, pos2.blockX)
                    val maxY = maxOf(pos1.blockY, pos2.blockY)
                    val maxZ = maxOf(pos1.blockZ, pos2.blockZ)

                    val region = CompiledRegion(
                        id = id,
                        worldId = pos1.world.uid,
                        priority = priority,
                        minX = minX, minY = minY, minZ = minZ,
                        maxX = maxX, maxY = maxY, maxZ = maxZ,
                        definedFlags = 0,
                        allowedFlags = 0
                    )

                    regionManager.createRegion(region).thenRun {
                        sender.sendMessage(ColorUtility.parse(getMsg("created", "id" to id)))
                    }
                }
        )

        // 2. /region delete <id>
        commandManager.command(
            regionBuilder.literal("delete")
                .required("id", StringParser.stringParser())
                .handler { context ->
                    val sender = context.sender()
                    val id = context.get<String>("id").lowercase()

                    regionManager.removeRegion(id).thenAccept { success ->
                        if (success) {
                            sender.sendMessage(ColorUtility.parse("<green>✔ Región '$id' eliminada de forma permanente.</green>"))
                        } else {
                            sender.sendMessage(ColorUtility.parse(getMsg("region-not-found", "id" to id)))
                        }
                    }
                }
        )

        // 3. /region flag <id> <flag> <allow/deny/remove>
        commandManager.command(
            regionBuilder.literal("flag")
                .required("id", StringParser.stringParser())
                .required("flag", StringParser.stringParser())
                .required("value", StringParser.stringParser())
                .handler { context ->
                    val sender = context.sender()
                    val id = context.get<String>("id").lowercase()
                    val flagStr = context.get<String>("flag").uppercase()
                    val valueStr = context.get<String>("value").lowercase()

                    val flag = RegionFlags.parse(flagStr)
                    if (flag == RegionFlags.NONE) {
                        sender.sendMessage(ColorUtility.parse(getMsg("invalid-flag", "flag" to flagStr)))
                        return@handler
                    }

                    val region = regionManager.getRegion(id)
                    if (region == null) {
                        sender.sendMessage(ColorUtility.parse(getMsg("region-not-found", "id" to id)))
                        return@handler
                    }

                    val updatedRegion = when (valueStr) {
                        "allow" -> {
                            val newDefined = region.definedFlags or flag
                            val newAllowed = region.allowedFlags or flag
                            region.copy(definedFlags = newDefined, allowedFlags = newAllowed)
                        }
                        "deny" -> {
                            val newDefined = region.definedFlags or flag
                            val newAllowed = region.allowedFlags and flag.inv()
                            region.copy(definedFlags = newDefined, allowedFlags = newAllowed)
                        }
                        "remove" -> {
                            val newDefined = region.definedFlags and flag.inv()
                            val newAllowed = region.allowedFlags and flag.inv()
                            region.copy(definedFlags = newDefined, allowedFlags = newAllowed)
                        }
                        else -> {
                            sender.sendMessage(ColorUtility.parse(getMsg("invalid-value")))
                            return@handler
                        }
                    }

                    regionManager.createRegion(updatedRegion).thenRun {
                        if (valueStr == "remove") {
                            sender.sendMessage(ColorUtility.parse(getMsg("flag-removed", "flag" to flagStr, "id" to id)))
                        } else {
                            sender.sendMessage(ColorUtility.parse(getMsg("flag-updated", "flag" to flagStr, "id" to id, "value" to valueStr.uppercase())))
                        }
                    }
                }
        )

        // 4. /region info [id]
        commandManager.command(
            regionBuilder.literal("info")
                .optional("id", StringParser.stringParser())
                .handler { context ->
                    val sender = context.sender()
                    val idOpt = context.optional<String>("id")
                    
                    val region = if (idOpt.isPresent) {
                        regionManager.getRegion(idOpt.get().lowercase())
                    } else {
                        if (sender !is Player) {
                            sender.sendMessage("Debes especificar la id de la región desde la consola.")
                            return@handler
                        }
                        val blockX = sender.location.blockX
                        val blockY = sender.location.blockY
                        val blockZ = sender.location.blockZ
                        val candidates = regionManager.getCurrentIndex().getRegionsInChunk(blockX shr 4, blockZ shr 4)
                        val active = candidates?.filter { it.worldId == sender.world.uid && it.contains(blockX, blockY, blockZ) }
                        active?.maxByOrNull { it.priority }
                    }

                    if (region == null) {
                        if (idOpt.isPresent) {
                            sender.sendMessage(ColorUtility.parse(getMsg("region-not-found", "id" to idOpt.get())))
                        } else {
                            sender.sendMessage(ColorUtility.parse("<red>❌ No te encuentras dentro de ninguna región registrada.</red>"))
                        }
                        return@handler
                    }

                    val flagsList = mutableListOf<String>()
                    val allFlags = listOf(RegionFlags.PVP, RegionFlags.BLOCK_BREAK, RegionFlags.BLOCK_PLACE, RegionFlags.INTERACT)
                    for (f in allFlags) {
                        if (region.hasFlag(f)) {
                            val allowedText = if (region.isAllowed(f)) "<green>ALLOW</green>" else "<red>DENY</red>"
                            flagsList.add("<yellow>${RegionFlags.toString(f)}</yellow>: $allowedText")
                        }
                    }
                    val flagsText = if (flagsList.isEmpty()) "<gray>Ninguna</gray>" else flagsList.joinToString(", ")

                    val msg = """
                        <dark_gray>===========================================</dark_gray>
                        <gold><bold>Región:</bold></gold> <yellow>${region.id}</yellow>
                        <gray>Prioridad:</gray> <white>${region.priority}</white>
                        <gray>Mundo UUID:</gray> <white>${region.worldId}</white>
                        <gray>Límites:</gray> <white>(${region.minX}, ${region.minY}, ${region.minZ}) -> (${region.maxX}, ${region.maxY}, ${region.maxZ})</white>
                        <gray>Banderas:</gray> $flagsText
                        <dark_gray>===========================================</dark_gray>
                    """.trimIndent()
                    sender.sendMessage(ColorUtility.parse(msg))
                }
        )
    }
}
