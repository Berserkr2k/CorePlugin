package com.github.berserkr2k.coreplugin.infrastructure.kits

import com.github.berserkr2k.coreplugin.common.ColorUtility
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.bukkit.parser.PlayerParser.playerParser
import org.incendo.cloud.parser.standard.StringParser.stringParser

class KitCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val kitService: KitService,
    private val guis: KitGuis
) {

    init {
        val kitBuilder = manager.commandBuilder("kit")

        // 1. /kit (Abrir selector gráfico)
        manager.command(
            kitBuilder.handler { context ->
                val sender = context.sender()
                if (sender !is Player) {
                    sender.sendMessage(ColorUtility.parse("<red>Solo jugadores pueden abrir el menú de selección de kits. Usa /kit <kit> <player></red>"))
                    return@handler
                }
                guis.openKitSelector(sender)
            }
        )

        // 2. /kit reload (Recargar archivos de kits)
        manager.command(
            kitBuilder.literal("reload")
                .permission("core.kits.admin")
                .handler { context ->
                    val sender = context.sender()
                    kitService.loadAllKits()
                    sender.sendMessage(ColorUtility.parse("<green>¡Configuraciones de Kits recargadas con éxito en tiempo real!</green>"))
                }
        )

        // 3. /kit <kitId> (Reclamar kit directamente)
        manager.command(
            kitBuilder.required("kitId", stringParser())
                .handler { context ->
                    val sender = context.sender()
                    val kitId = context.get<String>("kitId")

                    if (sender !is Player) {
                        sender.sendMessage(ColorUtility.parse("<red>Solo jugadores pueden reclamar kits directamente. Usa /kit <kit> <player></red>"))
                        return@handler
                    }

                    kitService.claimKit(sender, kitId, false).thenAccept { result ->
                        when (result) {
                            is ClaimResult.Success -> sender.sendMessage(ColorUtility.parse(result.message))
                            is ClaimResult.Failure -> sender.sendMessage(ColorUtility.parse(result.reason))
                        }
                    }
                }
        )

        // 4. /kit <kitId> <player> (Regalar kit a otro jugador)
        manager.command(
            kitBuilder.required("kitId", stringParser())
                .required("target", playerParser())
                .permission("core.kits.admin")
                .handler { context ->
                    val sender = context.sender()
                    val kitId = context.get<String>("kitId")
                    val target = context.get<Player>("target")

                    kitService.claimKit(target, kitId, true).thenAccept { result ->
                        when (result) {
                            is ClaimResult.Success -> {
                                sender.sendMessage(ColorUtility.parse("<green>¡Has entregado con éxito el kit '$kitId' a ${target.name}!</green>"))
                                target.sendMessage(ColorUtility.parse("<green>¡Has recibido el kit '$kitId' de parte de un administrador!</green>"))
                            }
                            is ClaimResult.Failure -> {
                                sender.sendMessage(ColorUtility.parse("<red>Error al entregar kit: ${result.reason}</red>"))
                            }
                        }
                    }
                }
        )

        // 5. /showkit <kitId> (Previsualizar kit en UI)
        manager.command(
            manager.commandBuilder("showkit")
                .required("kitId", stringParser())
                .handler { context ->
                    val sender = context.sender()
                    val kitId = context.get<String>("kitId")

                    if (sender !is Player) {
                        sender.sendMessage(ColorUtility.parse("<red>Solo jugadores pueden previsualizar los kits en la interfaz gráfica.</red>"))
                        return@handler
                    }

                    val config = kitService.kits[kitId.lowercase()]
                    if (config == null) {
                        sender.sendMessage(ColorUtility.parse("<red>El kit especificado no existe.</red>"))
                        return@handler
                    }

                    guis.openKitShowcase(sender, kitId)
                }
        )
    }
}
