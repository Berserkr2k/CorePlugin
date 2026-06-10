package com.github.berserkr2k.coreplugin.infrastructure.regions.command

import com.github.berserkr2k.coreplugin.api.regions.RegionFlags
import com.github.berserkr2k.coreplugin.api.regions.WorldIndexRegistry
import com.github.berserkr2k.coreplugin.infrastructure.regions.RegionConfig
import com.github.berserkr2k.coreplugin.infrastructure.regions.service.RegionManager
import com.github.berserkr2k.coreplugin.infrastructure.regions.resolver.RegionRuleResolver
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
    private val resolver: RegionRuleResolver,
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

        // 1. /region pos1
        commandManager.command(
            regionBuilder.literal("pos1")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        sender.sendMessage("Solo jugadores pueden establecer selecciones.")
                        return@handler
                    }
                    val block = sender.location.block
                    val sel = session.getSelection(sender.uniqueId)
                    sel.pos1 = block.location
                    sender.sendMessage("§a[!] Posición 1 establecida en ${block.x}, ${block.y}, ${block.z}.")
                }
        )

        // 2. /region pos2
        commandManager.command(
            regionBuilder.literal("pos2")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        sender.sendMessage("Solo jugadores pueden establecer selecciones.")
                        return@handler
                    }
                    val block = sender.location.block
                    val sel = session.getSelection(sender.uniqueId)
                    sel.pos2 = block.location
                    sender.sendMessage("§a[!] Posición 2 establecida en ${block.x}, ${block.y}, ${block.z}.")
                }
        )

        // 3. /region create <id> <priority>
        commandManager.command(
            regionBuilder.literal("create")
                .required("id", StringParser.stringParser())
                .required("priority", IntegerParser.integerParser())
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        sender.sendMessage("Solo jugadores pueden crear regiones.")
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

                    val dto = RegionConfig(
                        id = id,
                        world = pos1.world.name,
                        priority = priority,
                        minX = minX, minY = minY, minZ = minZ,
                        maxX = maxX, maxY = maxY, maxZ = maxZ,
                        allowFlags = emptyList(),
                        denyFlags = emptyList()
                    )

                    regionManager.createRegion(dto).thenRun {
                        sender.sendMessage(ColorUtility.parse(getMsg("created", "id" to id)))
                    }
                }
        )

        // 4. /region delete <id>
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

        // 5. /region flag <id> <flag> <allow/deny/remove>
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

                    val flagValue = RegionFlags.parse(flagStr)
                    if (flagValue == RegionFlags.NONE) {
                        sender.sendMessage(ColorUtility.parse(getMsg("invalid-flag", "flag" to flagStr)))
                        return@handler
                    }

                    val dto = regionManager.getRegionDTO(id)
                    if (dto == null) {
                        sender.sendMessage(ColorUtility.parse(getMsg("region-not-found", "id" to id)))
                        return@handler
                    }

                    val updatedAllow = ArrayList(dto.allowFlags)
                    val updatedDeny = ArrayList(dto.denyFlags)

                    when (valueStr) {
                        "allow" -> {
                            if (!updatedAllow.contains(flagStr)) updatedAllow.add(flagStr)
                            updatedDeny.remove(flagStr)
                        }
                        "deny" -> {
                            if (!updatedDeny.contains(flagStr)) updatedDeny.add(flagStr)
                            updatedAllow.remove(flagStr)
                        }
                        "remove" -> {
                            updatedAllow.remove(flagStr)
                            updatedDeny.remove(flagStr)
                        }
                        else -> {
                            sender.sendMessage(ColorUtility.parse(getMsg("invalid-value")))
                            return@handler
                        }
                    }

                    val updatedDto = dto.copy(allowFlags = updatedAllow, denyFlags = updatedDeny)

                    regionManager.createRegion(updatedDto).thenRun {
                        if (valueStr == "remove") {
                            sender.sendMessage(ColorUtility.parse(getMsg("flag-removed", "flag" to flagStr, "id" to id)))
                        } else {
                            sender.sendMessage(ColorUtility.parse(getMsg("flag-updated", "flag" to flagStr, "id" to id, "value" to valueStr.uppercase())))
                        }
                    }
                }
        )

        // 6. /region reload
        commandManager.command(
            regionBuilder.literal("reload")
                .handler { context ->
                    val sender = context.sender()
                    regionManager.loadAllRegions()
                    sender.sendMessage("§a[!] Configuración de regiones recargada de disco y Spatial Index reconstruido con éxito.")
                }
        )

        // 7. /region flags
        commandManager.command(
            regionBuilder.literal("flags")
                .handler { context ->
                    val sender = context.sender()
                    sender.sendMessage("§eBanderas disponibles:")
                    sender.sendMessage("§7- PVP, BLOCK_BREAK, BLOCK_PLACE, INTERACT, CHEST_ACCESS, ENDERCHEST_ACCESS, ANVIL_USE, ENCHANTING_USE")
                }
        )

        // 8. /region info [id]
        commandManager.command(
            regionBuilder.literal("info")
                .optional("id", StringParser.stringParser())
                .handler { context ->
                    val sender = context.sender()
                    val idOpt = context.optional<String>("id")

                    val dto = if (idOpt.isPresent) {
                        regionManager.getRegionDTO(idOpt.get().lowercase())
                    } else {
                        if (sender !is Player) {
                            sender.sendMessage("Debes especificar la id de la región desde la consola.")
                            return@handler
                        }
                        val loc = sender.location
                        val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
                        val active = resolver.resolveActiveRegions(worldIndex, loc.blockX, loc.blockY, loc.blockZ)
                        val highest = active.maxByOrNull { it.priority }
                        highest?.let { regionManager.getRegionDTO(it.id) }
                    }

                    if (dto == null) {
                        if (idOpt.isPresent) {
                            sender.sendMessage(ColorUtility.parse(getMsg("region-not-found", "id" to idOpt.get())))
                        } else {
                            sender.sendMessage(ColorUtility.parse("<red>❌ No te encuentras dentro de ninguna región registrada.</red>"))
                        }
                        return@handler
                    }

                    val msg = """
                        <dark_gray>===========================================</dark_gray>
                        <gold><bold>Región:</bold></gold> <yellow>${dto.id}</yellow>
                        <gray>Prioridad:</gray> <white>${dto.priority}</white>
                        <gray>Mundo:</gray> <white>${dto.world}</white>
                        <gray>Límites:</gray> <white>(${dto.minX}, ${dto.minY}, ${dto.minZ}) -> (${dto.maxX}, ${dto.maxY}, ${dto.maxZ})</white>
                        <gray>Banderas Permitidas:</gray> <green>${dto.allowFlags.joinToString(", ").ifEmpty { "Ninguna" }}</green>
                        <gray>Banderas Denegadas:</gray> <red>${dto.denyFlags.joinToString(", ").ifEmpty { "Ninguna" }}</red>
                        <dark_gray>===========================================</dark_gray>
                    """.trimIndent()
                    sender.sendMessage(ColorUtility.parse(msg))
                }
        )

        // 9. /region debug ...
        commandManager.command(
            regionBuilder.literal("debug")
                .literal("here")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        sender.sendMessage("Solo jugadores.")
                        return@handler
                    }
                    val loc = sender.location
                    val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
                    val active = resolver.resolveActiveRegions(worldIndex, loc.blockX, loc.blockY, loc.blockZ)
                    sender.sendMessage("§eRegiones activas aquí (Count: ${active.size}):")
                    for (reg in active) {
                        sender.sendMessage("§7- ${reg.id} (Prio: ${reg.priority})")
                    }
                }
        )

        commandManager.command(
            regionBuilder.literal("debug")
                .literal("flags")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) return@handler
                    sender.sendMessage("§eBypass general: ${sender.hasPermission("core.region.bypass")}")
                    sender.sendMessage("§eGameMode: ${sender.gameMode}")
                }
        )
    }
}
