package com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails

import com.github.berserkr2k.coreplugin.common.ColorUtility
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.bukkit.parser.PlayerParser.playerParser
import org.incendo.cloud.parser.standard.StringParser.stringParser

class ProjectileTrailCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val trailManager: ProjectileTrailManager,
    private val guis: TrailGuis
) {
    init {
        // 1. /trail (Abrir selector gráfico)
        manager.command(
            manager.commandBuilder("trail")
                .handler { context ->
                    val sender = context.sender()
                    if (sender !is Player) {
                        sender.sendMessage(ColorUtility.parse("<red>Solo jugadores pueden abrir el menú de selección de estelas.</red>"))
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
                        sender.sendMessage(ColorUtility.parse("<red>Solo jugadores pueden abrir el menú de selección de estelas.</red>"))
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
                    sender.sendMessage(ColorUtility.parse("<green>¡Configuraciones de Estelas de Proyectiles recargadas con éxito en tiempo real!</green>"))
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
                        sender.sendMessage(ColorUtility.parse("<red>La estela especificada '$trailId' no existe.</red>"))
                        return@handler
                    }

                    trailManager.savePlayerTrail(target.uniqueId, trailId).thenRun {
                        sender.sendMessage(ColorUtility.parse("<green>¡Estela '${config.displayName}' equipada con éxito a ${target.name}!</green>"))
                        target.sendMessage(ColorUtility.parse("<green>¡Se te ha equipado la estela '${config.displayName}' por un administrador!</green>"))
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
                        sender.sendMessage(ColorUtility.parse("<green>¡Se ha removido la estela de ${target.name} con éxito!</green>"))
                        target.sendMessage(ColorUtility.parse("<yellow>Tu estela de partículas ha sido removida por un administrador.</yellow>"))
                    }
                }
        )
    }
}
