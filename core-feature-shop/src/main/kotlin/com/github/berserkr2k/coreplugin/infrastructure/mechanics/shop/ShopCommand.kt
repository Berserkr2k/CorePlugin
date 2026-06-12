package com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop

import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuService
import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilderFactory
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.StringParser.stringParser

class ShopCommand(
    private val commandManager: CommandManager<CommandSender>,
    private val shopManager: ShopManager,
    private val messageService: MessageService,
    private val menuService: MenuService,
    private val itemFactory: ItemBuilderFactory
) {
    private val manager = commandManager
    private val shopGuis = ShopGuis(
        plugin = shopManager.plugin,
        shopManager = shopManager,
        messageService = messageService,
        menuService = menuService,
        itemFactory = itemFactory
    )

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
                    messageService.send(sender, ShopMessages.ONLY_PLAYERS)
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
                        messageService.send(sender, ShopMessages.CATEGORY_NOT_FOUND, PlaceholderContext.of("category" to category))
                        messageService.send(sender, ShopMessages.CATEGORY_USAGE)
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
                messageService.send(sender, ShopMessages.RELOAD_STARTING)
                
                shopManager.loadConfigurations()
                    .thenCompose { shopManager.loadMarketVolumes() }
                    .thenRun {
                        messageService.send(sender, ShopMessages.RELOAD_SUCCESS)
                        // Re-registrar acciones dinámicas si cambian
                        registerGlobalActions()
                    }
                    .exceptionally { ex ->
                        messageService.send(sender, ShopMessages.RELOAD_FAILED, PlaceholderContext.of("error" to (ex.message ?: ex.toString())))
                        ex.printStackTrace()
                        null
                    }
            }

        manager.command(reloadBuilder)
    }
}
