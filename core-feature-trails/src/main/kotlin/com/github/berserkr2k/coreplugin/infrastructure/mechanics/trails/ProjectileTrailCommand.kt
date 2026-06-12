package com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails

import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.CommandManager
import org.incendo.cloud.bukkit.parser.PlayerParser.playerParser
import org.incendo.cloud.parser.standard.StringParser.stringParser

class ProjectileTrailCommand(
    private val manager: CommandManager<CommandSender>,
    private val trailManager: ProjectileTrailManager,
    private val messageService: com.github.berserkr2k.coreplugin.api.core.message.MessageService,
    private val menuService: com.github.berserkr2k.coreplugin.api.framework.menu.MenuService,
    private val itemBuilderFactory: com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilderFactory
) {
    private val guis: TrailGuis

    init {
        val registry = org.bukkit.Bukkit.getServicesManager().load(com.github.berserkr2k.coreplugin.api.di.ServiceRegistry::class.java)
            ?: throw IllegalStateException("ServiceRegistry not found in ServicesManager")
        val regionTaskScheduler = registry.get(com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler::class.java)

        this.guis = TrailGuis(trailManager, regionTaskScheduler, menuService, itemBuilderFactory, messageService)
        // 1. /trail (Abrir selector gráfico)
        manager.command(
            manager.commandBuilder("trail")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        messageService.send(sender, TrailMessages.ONLY_PLAYERS)
                        return@handler
                    }
                    guis.openTrailSelector(sender)
                }
        )

        // 2. /trails (Alias para abrir selector gráfico)
        manager.command(
            manager.commandBuilder("trails")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        messageService.send(sender, TrailMessages.ONLY_PLAYERS)
                        return@handler
                    }
                    guis.openTrailSelector(sender)
                }
        )

        // 3. /core trail reload (Administración: Recargar archivos de estelas)
        manager.command(
            manager.commandBuilder("core")
                .literal("trail")
                .literal("reload")
                .permission("core.trail.admin")
                .handler { context ->
                    val sender = context.sender()
                    trailManager.loadAllTrails()
                    messageService.send(sender, TrailMessages.RELOADED)
                }
        )

        // 4. /core trail set <player> <trailId> (Administración: Forzar estela a un jugador)
        manager.command(
            manager.commandBuilder("core")
                .literal("trail")
                .literal("set")
                .required("target", playerParser())
                .required("trailId", stringParser())
                .permission("core.trail.admin")
                .handler { context ->
                    val sender = context.sender()
                    val target = context.get<Player>("target")
                    val trailId = context.get<String>("trailId").lowercase()

                    val config = trailManager.trails[trailId]
                    if (config == null) {
                        messageService.send(sender, TrailMessages.NOT_FOUND, PlaceholderContext.of("id" to trailId))
                        return@handler
                    }

                    trailManager.savePlayerTrail(target.uniqueId, trailId).thenRun {
                        messageService.send(sender, TrailMessages.ADMIN_EQUIPPED_SENDER, PlaceholderContext.of("name" to config.displayName, "target" to target.name))
                        messageService.send(target, TrailMessages.ADMIN_EQUIPPED_TARGET, PlaceholderContext.of("name" to config.displayName))
                    }
                }
        )

        // 5. /core trail clear <player> (Administración: Remover estela a un jugador)
        manager.command(
            manager.commandBuilder("core")
                .literal("trail")
                .literal("clear")
                .required("target", playerParser())
                .permission("core.trail.admin")
                .handler { context ->
                    val sender = context.sender()
                    val target = context.get<Player>("target")

                    trailManager.savePlayerTrail(target.uniqueId, null).thenRun {
                        messageService.send(sender, TrailMessages.ADMIN_CLEARED_SENDER, PlaceholderContext.of("target" to target.name))
                        messageService.send(target, TrailMessages.ADMIN_CLEARED_TARGET)
                    }
                }
        )
    }
}
