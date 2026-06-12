package com.github.berserkr2k.coreplugin.infrastructure.regions.command

import com.github.berserkr2k.coreplugin.api.framework.regions.RegionFlags
import com.github.berserkr2k.coreplugin.infrastructure.regions.WorldIndexRegistry
import com.github.berserkr2k.coreplugin.infrastructure.regions.RegionConfig
import com.github.berserkr2k.coreplugin.infrastructure.regions.service.RegionManager
import com.github.berserkr2k.coreplugin.infrastructure.regions.resolver.RegionRuleResolver
import com.github.berserkr2k.coreplugin.infrastructure.regions.RegionMessages
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import com.github.berserkr2k.coreplugin.api.core.message.CoreMessages
import com.github.berserkr2k.coreplugin.api.protection.permissions.Permissions
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.IntegerParser
import org.incendo.cloud.parser.standard.StringParser
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry

class RegionCommand(
    private val plugin: Plugin,
    private val commandManager: CommandManager<CommandSender>,
    private val regionManager: RegionManager,
    private val messageService: MessageService
) {
    private val session = regionManager.selectionSession
    private val resolver = regionManager.resolver
    private val serviceRegistry = org.bukkit.Bukkit.getServicesManager().load(com.github.berserkr2k.coreplugin.api.di.ServiceRegistry::class.java)
        ?: throw IllegalStateException("ServiceRegistry not found in ServicesManager")

    init {
        registerCommands()
    }

    private fun registerCommands() {
        val regionBuilder = commandManager.commandBuilder("region")
            .permission(Permissions.REGION_SETUP)

        // 1. /region pos1
        commandManager.command(
            regionBuilder.literal("pos1")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        messageService.send(sender, CoreMessages.ONLY_PLAYERS)
                        return@handler
                    }
                    val block = sender.location.block
                    val sel = session.getSelection(sender.uniqueId)
                    sel.pos1 = block.location
                    messageService.send(
                        sender,
                        RegionMessages.SELECTION_POS1,
                        PlaceholderContext.of(
                            "x" to block.x.toString(),
                            "y" to block.y.toString(),
                            "z" to block.z.toString()
                        )
                    )
                }
        )

        // 2. /region pos2
        commandManager.command(
            regionBuilder.literal("pos2")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        messageService.send(sender, CoreMessages.ONLY_PLAYERS)
                        return@handler
                    }
                    val block = sender.location.block
                    val sel = session.getSelection(sender.uniqueId)
                    sel.pos2 = block.location
                    messageService.send(
                        sender,
                        RegionMessages.SELECTION_POS2,
                        PlaceholderContext.of(
                            "x" to block.x.toString(),
                            "y" to block.y.toString(),
                            "z" to block.z.toString()
                        )
                    )
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
                        messageService.send(sender, CoreMessages.ONLY_PLAYERS)
                        return@handler
                    }

                    val id = context.get<String>("id").lowercase()
                    val priority = context.get<Int>("priority")
                    val selection = session.getSelection(sender.uniqueId)

                    val pos1 = selection.pos1
                    val pos2 = selection.pos2
                    if (pos1 == null || pos2 == null) {
                        messageService.send(sender, RegionMessages.SELECTION_INCOMPLETE)
                        return@handler
                    }

                    messageService.send(sender, RegionMessages.COMPILING)

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
                        denyFlags = listOf(
                            "build", "use", "interact", "pvp", "vehicle-place", "vehicle-destroy",
                            "tnt", "creeper-explosion", "ghast-fireball", "other-explosion",
                            "enderman-grief", "fire-spread", "lava-fire"
                        )
                    )

                    regionManager.createRegion(dto).thenRun {
                        messageService.send(sender, RegionMessages.CREATED, PlaceholderContext.of("id" to id))
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
                            messageService.send(sender, RegionMessages.DELETED, PlaceholderContext.of("id" to id))
                        } else {
                            messageService.send(sender, RegionMessages.REGION_NOT_FOUND, PlaceholderContext.of("id" to id))
                        }
                    }
                }
        )

        // 5. /region flag <id> & /region edit <id>
        val editHandler = { context: org.incendo.cloud.context.CommandContext<CommandSender> ->
            val sender = context.sender()
            if (sender !is Player) {
                messageService.send(sender, CoreMessages.ONLY_PLAYERS)
            } else {
                val id = context.get<String>("id").lowercase()
                val dto = regionManager.getRegionDTO(id)
                if (dto == null) {
                    messageService.send(sender, RegionMessages.REGION_NOT_FOUND, PlaceholderContext.of("id" to id))
                } else {
                    val gui = com.github.berserkr2k.coreplugin.infrastructure.regions.gui.RegionFlagEditorGui(plugin, regionManager, serviceRegistry)
                    gui.openCategorySelection(sender, id)
                }
            }
        }

        commandManager.command(
            regionBuilder.literal("flag")
                .required("id", StringParser.stringParser())
                .handler { editHandler(it) }
        )

        commandManager.command(
            regionBuilder.literal("edit")
                .required("id", StringParser.stringParser())
                .handler { editHandler(it) }
        )

        // 6. /region reload
        commandManager.command(
            regionBuilder.literal("reload")
                .handler { context ->
                    val sender = context.sender()
                    regionManager.loadAllRegions()
                    messageService.send(sender, RegionMessages.RELOADED)
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
                            messageService.send(sender, RegionMessages.CONSOLE_ONLY_ID)
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
                            messageService.send(sender, RegionMessages.REGION_NOT_FOUND, PlaceholderContext.of("id" to idOpt.get()))
                        } else {
                            messageService.send(sender, RegionMessages.NOT_IN_REGION)
                        }
                        return@handler
                    }

                    messageService.send(
                        sender,
                        RegionMessages.REGION_INFO,
                        PlaceholderContext.of(
                            "id" to dto.id,
                            "priority" to dto.priority.toString(),
                            "world" to dto.world,
                            "min_x" to dto.minX.toString(),
                            "min_y" to dto.minY.toString(),
                            "min_z" to dto.minZ.toString(),
                            "max_x" to dto.maxX.toString(),
                            "max_y" to dto.maxY.toString(),
                            "max_z" to dto.maxZ.toString(),
                            "allow_flags" to dto.allowFlags.joinToString(", ").ifEmpty { "Ninguna" },
                            "deny_flags" to dto.denyFlags.joinToString(", ").ifEmpty { "Ninguna" }
                        )
                    )
                }
        )

        // 9. /region debug ...
        commandManager.command(
            regionBuilder.literal("debug")
                .literal("here")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        messageService.send(sender, CoreMessages.ONLY_PLAYERS)
                        return@handler
                    }
                    val loc = sender.location
                    val worldIndex = WorldIndexRegistry.getIndex(loc.world.uid)
                    val active = resolver.resolveActiveRegions(worldIndex, loc.blockX, loc.blockY, loc.blockZ)
                    messageService.send(sender, RegionMessages.DEBUG_HERE_HEADER, PlaceholderContext.of("count" to active.size.toString()))
                    for (reg in active) {
                        messageService.send(
                            sender,
                            RegionMessages.DEBUG_HERE_ITEM,
                            PlaceholderContext.of("id" to reg.id, "priority" to reg.priority.toString())
                        )
                    }
                }
        )

        commandManager.command(
            regionBuilder.literal("debug")
                .literal("flags")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) return@handler
                    messageService.send(
                        sender,
                        RegionMessages.DEBUG_FLAGS,
                        PlaceholderContext.of(
                            "bypass" to sender.hasPermission(Permissions.REGION_BYPASS).toString(),
                            "gamemode" to sender.gameMode.name
                        )
                    )
                }
        )
    }
}
