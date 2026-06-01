package com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop

import com.github.berserkr2k.coreplugin.common.ColorUtility
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.StringParser.stringParser

class ShopCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val shopManager: ShopManager,
    private val shopGuis: ShopGuis
) {

    init {
        registerShopCommand()
        registerAdminReloadCommand()
    }

    private fun registerShopCommand() {
        val shopBuilder = manager.commandBuilder("shop")
            .optional("category", stringParser())
            .permission("core.shop.use")
            .handler { context ->
                val sender = context.sender()
                if (sender !is Player) {
                    sender.sendMessage(ColorUtility.parse("<red>❌ Solo los jugadores pueden abrir las tiendas.</red>"))
                    return@handler
                }

                val category = context.optional<String>("category").orElse(null)
                if (category != null) {
                    val catId = category.lowercase()
                    if (shopManager.categories.containsKey(catId)) {
                        shopGuis.openCategoryMenu(sender, catId)
                    } else {
                        sender.sendMessage(ColorUtility.parse("<red>❌ La categoría de tienda '$category' no existe.</red>"))
                        sender.sendMessage(ColorUtility.parse("<gray>Usa /shop para ver las categorías disponibles.</gray>"))
                    }
                } else {
                    shopGuis.openCategoriesMenu(sender)
                }
            }

        manager.command(shopBuilder)
    }

    private fun registerAdminReloadCommand() {
        val reloadBuilder = manager.commandBuilder("core")
            .literal("shop")
            .literal("reload")
            .permission("core.shop.admin")
            .handler { context ->
                val sender = context.sender()
                sender.sendMessage(ColorUtility.parse("<yellow>⏳ Recargando la configuración de tiendas...</yellow>"))
                
                shopManager.loadConfigurations()
                    .thenCompose { shopManager.loadMarketVolumes() }
                    .thenRun {
                        sender.sendMessage(ColorUtility.parse("<green>✔ ¡Configuración de tiendas recargada con éxito!</green>"))
                        // Re-registrar acciones dinámicas si cambian
                        shopManager.marketConfig.categoriesMenu.items.forEach { (shopId, _) ->
                            try {
                                org.incendo.cloud.component.CommandComponent.builder<CommandSender, Any>()
                                com.github.berserkr2k.coreplugin.common.gui.MenuActionRegistry.register("open_shop_$shopId") { player, _ ->
                                    shopGuis.openCategoryMenu(player, shopId)
                                }
                            } catch (e: Exception) {}
                        }
                    }
                    .exceptionally { ex ->
                        sender.sendMessage(ColorUtility.parse("<red>❌ Falló la recarga de tiendas: ${ex.message}</red>"))
                        ex.printStackTrace()
                        null
                    }
            }

        manager.command(reloadBuilder)
    }
}
