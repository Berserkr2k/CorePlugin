package com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop

import com.github.berserkr2k.coreplugin.common.ColorUtility
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.StringParser.stringParser

import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.getShops

class ShopCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val shopManager: ShopManager,
    private val shopGuis: ShopGuis,
    private val messagesConfig: MessagesConfig
) {

    private fun getMsg(key: String, vararg placeholders: Pair<String, Any>): String {
        return messagesConfig.getShops(key, *placeholders)
    }

    init {
        registerShopCommand()
        registerAdminReloadCommand()
        
        shopManager.initFuture.thenRun {
            registerGlobalActions()
        }
    }

    private fun registerGlobalActions() {
        shopManager.marketConfig.categoriesMenu.items.forEach { (shopId, _) ->
            try {
                com.github.berserkr2k.coreplugin.common.gui.MenuActionRegistry.register("open_shop_$shopId") { player, _ ->
                    if (shopId == "history") {
                        shopGuis.openHistoryMenu(player)
                    } else {
                        shopGuis.openCategoryMenu(player, shopId)
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun registerShopCommand() {
        val shopBuilder = manager.commandBuilder("shop")
            .optional("category", stringParser())
            .permission("core.shop.use")
            .handler { context ->
                val sender = context.sender()
                if (sender !is Player) {
                    sender.sendMessage(ColorUtility.parse(getMsg("only-players")))
                    return@handler
                }

                val category = context.optional<String>("category").orElse(null)
                if (category != null) {
                    val catId = category.lowercase()
                    if (catId == "history" || catId == "historial") {
                        shopGuis.openHistoryMenu(sender)
                    } else if (shopManager.categories.containsKey(catId)) {
                        shopGuis.openCategoryMenu(sender, catId)
                    } else {
                        sender.sendMessage(ColorUtility.parse(getMsg("category-not-found", "category" to category)))
                        sender.sendMessage(ColorUtility.parse(getMsg("category-usage")))
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
                        registerGlobalActions()
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
