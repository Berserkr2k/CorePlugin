package com.github.berserkr2k.coreplugin.infrastructure.kits

import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.feature.kits.ClaimResult
import com.github.berserkr2k.coreplugin.api.core.message.CoreMessages
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuService
import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilderFactory
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
    private val messageService: MessageService,
    private val menuService: MenuService,
    private val itemBuilderFactory: ItemBuilderFactory
) {
    private val guis = KitGuis(
        plugin,
        org.bukkit.Bukkit.getServicesManager().load(com.github.berserkr2k.coreplugin.api.di.ServiceRegistry::class.java)
            ?.get(com.github.berserkr2k.coreplugin.api.core.config.ConfigService::class.java)!!,
        kitService,
        messageService
    )

    init {
        val kitBuilder = manager.commandBuilder("kit")

        // 1. /kit (Abrir selector gráfico)
        manager.command(
            kitBuilder.handler { context ->
                val sender = context.sender()
                if (sender !is Player) {
                    messageService.send(sender, CoreMessages.ONLY_PLAYERS)
                    return@handler
                }
                guis.openKitSelector(sender, menuService, itemBuilderFactory)
            }
        )

        // 2. /kit reload (Recargar archivos de kits)
        manager.command(
            kitBuilder.literal("reload")
                .permission("core.kits.admin")
                .handler { context ->
                    val sender = context.sender()
                    kitService.loadAllKits()
                    guis.reload()
                    messageService.send(sender, KitMessages.RELOADED)
                }
        )

        // 3. /kit <kitId> (Reclamar kit directamente)
        manager.command(
            kitBuilder.required("kitId", stringParser())
                .handler { context ->
                    val sender = context.sender()
                    val kitId = context.get<String>("kitId")

                    if (sender !is Player) {
                        messageService.send(sender, CoreMessages.ONLY_PLAYERS)
                        return@handler
                    }

                    kitService.claimKit(sender, kitId, false).thenAccept { result ->
                        when (result) {
                            is ClaimResult.Success -> messageService.sendRaw(sender, result.message)
                            is ClaimResult.Failure -> messageService.sendRaw(sender, result.reason)
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
                                messageService.send(sender, KitMessages.GIVE_SUCCESS_SENDER, PlaceholderContext.of("kit" to kitId, "target" to target.name))
                                messageService.send(target, KitMessages.GIVE_SUCCESS_RECEIVER, PlaceholderContext.of("kit" to kitId))
                            }
                            is ClaimResult.Failure -> {
                                messageService.send(sender, KitMessages.GIVE_FAILED, PlaceholderContext.of("reason" to result.reason))
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
                        messageService.send(sender, CoreMessages.ONLY_PLAYERS)
                        return@handler
                    }

                    val config = kitService.kits[kitId.lowercase()]
                    if (config == null) {
                        messageService.send(sender, KitMessages.NOT_FOUND)
                        return@handler
                    }

                    guis.openKitShowcase(sender, kitId, menuService, itemBuilderFactory)
                }
        )
    }
}
