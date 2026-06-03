package com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop

import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.common.gui.*
import com.github.berserkr2k.coreplugin.infrastructure.config.getShops
import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import com.github.berserkr2k.coreplugin.infrastructure.economy.EconomyService
import com.github.berserkr2k.coreplugin.infrastructure.economy.TransactionLockManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.plugin.Plugin
import java.math.BigDecimal
import java.math.RoundingMode

class ShopGuis(
    private val plugin: Plugin,
    private val shopManager: ShopManager,
    private val economyService: EconomyService,
    private val messagesConfig: MessagesConfig
) {

    init {
        // Registrar acciones dinámicas para abrir las tiendas de forma local al instanciar el menú
    }

    private fun getMsg(key: String, vararg placeholders: Pair<String, Any>): String {
        return messagesConfig.getShops(key, *placeholders)
    }


    fun openCategoriesMenu(player: Player) {
        if (!shopManager.marketReady) {
            player.sendMessage(ColorUtility.parse(getMsg("market-regulating")))
            return
        }

        val config = shopManager.marketConfig.categoriesMenu
        val title = ColorUtility.parse(config.title)
        val size = config.size
        
        val menu = CustomMenu(title, size, plugin)
        
        // Registrar acciones locales dinámicas asociadas al menú
        config.items.forEach { (shopId, _) ->
            if (shopId == "history") {
                menu.registerLocalAction("open_shop_history") { p, _ ->
                    openHistoryMenu(p)
                }
            } else {
                menu.registerLocalAction("open_shop_$shopId") { p, _ ->
                    openCategoryMenu(p, shopId)
                }
            }
        }

        menu.loadFromConfig(config)
        menu.open(player)
    }

    fun openHistoryMenu(player: Player) {
        if (!shopManager.marketReady) {
            player.sendMessage(ColorUtility.parse(getMsg("market-regulating")))
            return
        }

        shopManager.getPlayerTransactionHistory(player.uniqueId).thenAccept { history ->
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (!player.isOnline) return@Runnable

                val config = shopManager.marketConfig.historyMenu
                val title = ColorUtility.parse(config.title)
                val size = config.size
                val menu = CustomMenu(title, size, plugin)

                // Fondo de cristal gris (o el configurado en filler)
                if (config.filler.enabled) {
                    val filler = ItemBuilder.fromConfig(config.filler.item).build()
                    for (i in 0 until size) {
                        menu.setItem(i, filler.clone())
                    }
                }

                // Botón de retorno al selector
                val backMatStr = getMsg("back-item-material")
                val backMat = Material.matchMaterial(backMatStr) ?: Material.BARRIER
                val backName = getMsg("back-item-name")
                val backLore = listOf(getMsg("back-item-lore"))

                val backItem = ItemBuilder(backMat)
                    .displayName(backName)
                    .lore(backLore)
                    .build()

                val backSlot = size - 5
                menu.setItem(backSlot, backItem) { p, _ ->
                    openCategoriesMenu(p)
                }

                // Elementos paginados
                menu.placePaginatedItems(
                    config,
                    history,
                    config.previousPageItem,
                    config.nextPageItem
                ) { record, slot ->
                    val matName = record.itemId.substringBefore("_").uppercase()
                    val mat = Material.matchMaterial(matName) ?: org.bukkit.Material.PAPER
                    
                    val nameFormat = if (record.type.equals("BUY", ignoreCase = true)) {
                        shopManager.marketConfig.historyItemBuyFormat
                    } else {
                        shopManager.marketConfig.historyItemSellFormat
                    }
                    val displayName = nameFormat.replace("<material>", mat.name.replace("_", " "))

                    val datePattern = shopManager.marketConfig.historyDateFormat
                    val sdf = try {
                        java.text.SimpleDateFormat(datePattern, java.util.Locale.US)
                    } catch (e: Exception) {
                        java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.US)
                    }
                    val dateStr = sdf.format(java.util.Date(record.timestamp))

                    val totalFormatted = economyService.formatBalance(shopManager.marketConfig.currencyId, record.totalPrice)
                    val lore = shopManager.marketConfig.historyItemLoreFormat.map { line ->
                        line.replace("<category>", record.shopId)
                            .replace("<quantity>", record.quantity.toString())
                            .replace("<total>", totalFormatted)
                            .replace("<date>", dateStr)
                    }

                    val item = ItemBuilder(mat)
                        .displayName(displayName)
                        .lore(lore)
                        .build()

                    menu.setItem(slot, item, null)
                }

                menu.open(player)
            })
        }.exceptionally { ex ->
            plugin.logger.severe("Fallo al abrir historial de transacciones para ${player.name}: ${ex.message}")
            player.sendMessage(ColorUtility.parse(getMsg("history-error")))
            null
        }
    }

    fun openCategoryMenu(player: Player, shopId: String) {
        if (!shopManager.marketReady) {
            player.sendMessage(ColorUtility.parse(getMsg("market-regulating")))
            return
        }

        val categoryConfig = shopManager.categories[shopId] ?: return
        val size = categoryConfig.guiSize
        val title = ColorUtility.parse(categoryConfig.displayName)

        val menu = CustomMenu(title, size, plugin)

        // Fondo de cristal gris
        val filler = ItemConfig(material = "GRAY_STAINED_GLASS_PANE", displayName = " ").toItemStack()
        for (i in 0 until size) {
            menu.setItem(i, filler.clone())
        }

        // Botón de retorno al selector configurable
        val backMatStr = categoryConfig.backItemMaterial ?: getMsg("back-item-material")
        val backMat = Material.matchMaterial(backMatStr) ?: Material.BARRIER
        val backName = categoryConfig.backItemName ?: getMsg("back-item-name")
        val backLoreStr = if (categoryConfig.backItemLore.isNotEmpty()) categoryConfig.backItemLore else listOf(getMsg("back-item-lore"))

        val backItem = ItemConfig(
            material = backMat.name,
            displayName = backName,
            lore = backLoreStr
        ).toItemStack()

        val backSlot = size - 5
        menu.setItem(backSlot, backItem) { p, _ ->
            openCategoriesMenu(p)
        }

        if (categoryConfig.paginated) {
            // Utilizar el sistema de paginación de CustomMenu mapeando los parámetros de ShopConfig
            val dummyMenuConfig = MenuConfig(
                title = categoryConfig.displayName,
                size = categoryConfig.guiSize,
                paginated = true,
                dynamicSlots = categoryConfig.dynamicSlots,
                previousPageSlot = categoryConfig.previousPageSlot,
                nextPageSlot = categoryConfig.nextPageSlot,
                previousPageItem = categoryConfig.previousPageItem,
                nextPageItem = categoryConfig.nextPageItem,
                items = mapOf("back" to MenuItemConfig(slots = listOf(backSlot))) // Proteger el slot de volver
            )

            menu.placePaginatedItems(
                dummyMenuConfig,
                categoryConfig.items,
                categoryConfig.previousPageItem,
                categoryConfig.nextPageItem
            ) { itemConfig, slot ->
                val baseItem = ItemConfig(
                    material = itemConfig.material,
                    customModelData = itemConfig.customModelData,
                    enchantments = itemConfig.enchantments
                ).toItemStack()

                val enrichedItem = enrichItemLore(itemConfig, baseItem)
                menu.setItem(slot, enrichedItem) { p, _ ->
                    openQuantitySubGui(p, shopId, itemConfig)
                }
            }
        } else {
            // Cargar ítems de la categoría de forma clásica
            categoryConfig.items.forEach { itemConfig ->
                val baseItem = ItemConfig(
                    material = itemConfig.material,
                    customModelData = itemConfig.customModelData,
                    enchantments = itemConfig.enchantments
                ).toItemStack()

                val enrichedItem = enrichItemLore(itemConfig, baseItem)
                val suggestedSlot = itemConfig.guiSlot

                val targetSlot = if (suggestedSlot in 0 until size && suggestedSlot != backSlot) suggestedSlot else {
                    var foundSlot = -1
                    for (s in 10 until size - 9) {
                        if (s == backSlot) continue
                        val item = menu.inventory.getItem(s)
                        if (item == null || item.type == Material.GRAY_STAINED_GLASS_PANE) {
                            foundSlot = s
                            break
                        }
                    }
                    foundSlot
                }

                if (targetSlot != -1) {
                    menu.setItem(targetSlot, enrichedItem) { p, _ ->
                        openQuantitySubGui(p, shopId, itemConfig)
                    }
                }
            }
        }

        menu.open(player)
    }

    private fun enrichItemLore(item: ShopItemConfig, itemStack: ItemStack): ItemStack {
        val meta = itemStack.itemMeta ?: return itemStack
        
        // Sobrescribir displayName con la identidad específica si está configurada
        if (item.displayName != null) {
            meta.displayName(ColorUtility.parse(item.displayName))
        }

        val buyPrice = shopManager.getBuyPrice(item)
        val sellPrice = shopManager.getSellPrice(item)

        val taxRate = shopManager.marketConfig.taxRate
        val taxBuy = buyPrice.multiply(BigDecimal(taxRate)).divide(BigDecimal.ONE.add(BigDecimal(taxRate)), 2, RoundingMode.HALF_UP)
        val taxSell = sellPrice.multiply(BigDecimal(taxRate)).divide(BigDecimal.ONE.add(BigDecimal(taxRate)), 2, RoundingMode.HALF_UP)

        val itemId = shopManager.getItemId(item)
        val buys = shopManager.buyVolumeCache[itemId] ?: 0
        val sells = shopManager.sellVolumeCache[itemId] ?: 0
        val netVolume = buys - sells

        val trend = if (netVolume > 0) "<green>▲ Al alza</green>" else if (netVolume < 0) "<red>▼ A la baja</red>" else "<gray>■ Estable</gray>"

        val newLore = mutableListOf<String>()

        // 1. Agregar descripción estética/histórica exclusiva del ítem (Identidad Específica)
        item.lore.forEach { line ->
            newLore.add(line)
        }
        if (item.lore.isNotEmpty()) {
            newLore.add("") // Separador estético
        }

        // 2. Agregar formato transaccional genérico reutilizable (Estructura Genérica de messages.conf)
        newLore.add(getMsg("lore-separator"))
        if (item.allowBuy) {
            newLore.add(getMsg("buy-price-format").replace("<price>", economyService.formatBalance(shopManager.marketConfig.currencyId, buyPrice)))
            newLore.add(getMsg("buy-tax-format").replace("<tax>", economyService.formatBalance(shopManager.marketConfig.currencyId, taxBuy)))
        } else {
            newLore.add(getMsg("buy-disabled"))
        }
        if (item.allowSell) {
            newLore.add(getMsg("sell-price-format").replace("<price>", economyService.formatBalance(shopManager.marketConfig.currencyId, sellPrice)))
            newLore.add(getMsg("sell-tax-format").replace("<tax>", economyService.formatBalance(shopManager.marketConfig.currencyId, taxSell)))
        } else {
            newLore.add(getMsg("sell-disabled"))
        }
        newLore.add(getMsg("lore-separator"))
        newLore.add(getMsg("lore-volume").replace("<volume>", (buys + sells).toString()))
        newLore.add(getMsg("lore-trend").replace("<trend>", trend))
        newLore.add(getMsg("lore-separator"))
        newLore.add(getMsg("lore-click"))

        val combinedLore = ArrayList<net.kyori.adventure.text.Component>()
        newLore.forEach { combinedLore.add(ColorUtility.parse(it)) }
        meta.lore(combinedLore)
        itemStack.itemMeta = meta
        return itemStack
    }

    fun openQuantitySubGui(player: Player, shopId: String, itemConfig: ShopItemConfig) {
        val title = ColorUtility.parse("<dark_gray>Transacción Simétrica</dark_gray>")
        val menu = CustomMenu(title, 27, plugin)

        // Rellenar fondo decorativo
        val filler = ItemConfig(material = "GRAY_STAINED_GLASS_PANE", displayName = " ").toItemStack()
        for (i in 0 until 27) {
            menu.setItem(i, filler.clone())
        }

        // Slot 4: Item previsualizador inerte con su lore de identidad enriquecido
        val baseItem = ItemConfig(
            material = itemConfig.material,
            customModelData = itemConfig.customModelData,
            enchantments = itemConfig.enchantments
        ).toItemStack()
        val previewItem = enrichItemLore(itemConfig, baseItem.clone())
        menu.setItem(4, previewItem)

        // Slot 13: Divisor Central Tintado
        val divisor = ItemConfig(material = "BLACK_STAINED_GLASS_PANE", displayName = " ").toItemStack()
        menu.setItem(13, divisor)

        // Slot 22: Botón de Volver configurable
        val backMatStr = getMsg("back-item-material")
        val backMat = Material.matchMaterial(backMatStr) ?: Material.BARRIER
        val backName = getMsg("back-item-name")
        val backLoreStr = listOf(getMsg("back-item-lore"))

        val backItem = ItemConfig(
            material = backMat.name,
            displayName = backName,
            lore = backLoreStr
        ).toItemStack()

        menu.setItem(22, backItem) { p, _ ->
            openCategoryMenu(p, shopId)
        }

        // --- COMPRAS ( Slots 10, 11, 12 ) ---
        setupBuyButton(menu, 10, 1, itemConfig, shopId)
        setupBuyButton(menu, 11, 32, itemConfig, shopId)
        setupBuyButton(menu, 12, 64, itemConfig, shopId)

        // --- VENTAS ( Slots 16, 15, 14 ) ---
        setupSellButton(menu, 16, 1, itemConfig, shopId)
        setupSellButton(menu, 15, 32, itemConfig, shopId)
        setupSellButton(menu, 14, 64, itemConfig, shopId)

        // --- COMPRAR MÁXIMO ( Slot 21 ) ---
        if (itemConfig.allowBuy) {
            val playerBal = economyService.getBalance(player.uniqueId, shopManager.marketConfig.currencyId)
            val maxBuyRes = calculateMaxPurchase(player, itemConfig, playerBal)
            val count = maxBuyRes.first
            val totalCost = maxBuyRes.second
            
            val maxBuyItem = ItemConfig(
                material = "EMERALD_BLOCK",
                displayName = "<green><bold>Comprar Máximo</bold></green>",
                lore = listOf(
                    "<gray>Llena tu inventario con este ítem.</gray>",
                    "",
                    "<gray>Cantidad a comprar: <gold>$count uds.</gold>",
                    "<gray>Costo Estimado:    <green>${economyService.formatBalance(shopManager.marketConfig.currencyId, totalCost)}</green>",
                    "",
                    "<yellow>▶ Haz clic para comprar</yellow>"
                )
            ).toItemStack()
            
            menu.setItem(21, maxBuyItem) { p, _ ->
                executePurchase(p, shopId, itemConfig, true)
            }
        }

        // --- VENDER TODO ( Slot 23 ) ---
        if (itemConfig.allowSell) {
            val itemsCount = getPlayerItemCount(player, baseItem)
            val sellRes = shopManager.simulateBulkSell(itemConfig, itemsCount)
            val totalVal = sellRes.first

            val sellAllItem = ItemConfig(
                material = "REDSTONE_BLOCK",
                displayName = "<red><bold>Vender Todo</bold></red>",
                lore = listOf(
                    "<gray>Vacía tu inventario de este ítem.</gray>",
                    "",
                    "<gray>Cantidad a vender: <gold>$itemsCount uds.</gold>",
                    "<gray>Valor Estimado:    <red>${economyService.formatBalance(shopManager.marketConfig.currencyId, totalVal)}</red>",
                    "",
                    "<yellow>▶ Haz clic para vender</yellow>"
                )
            ).toItemStack()

            menu.setItem(23, sellAllItem) { p, _ ->
                executeSale(p, shopId, itemConfig, true)
            }
        }

        menu.open(player)
    }

    private fun setupBuyButton(menu: CustomMenu, slot: Int, qty: Int, item: ShopItemConfig, shopId: String) {
        if (!item.allowBuy) {
            val disabled = ItemConfig(material = "RED_STAINED_GLASS_PANE", displayName = "<red>Compra Deshabilitada</red>").toItemStack()
            menu.setItem(slot, disabled)
            return
        }

        val sim = shopManager.simulateBulkBuy(item, qty)
        val totalCost = sim.first

        val btn = ItemConfig(
            material = "GREEN_STAINED_GLASS_PANE",
            displayName = "<green><bold>Comprar $qty</bold></green>",
            lore = listOf(
                "<gray>Compra una cantidad de $qty unidades.</gray>",
                "",
                "<gray>Costo Total: <green>${economyService.formatBalance(shopManager.marketConfig.currencyId, totalCost)}</green>",
                "",
                "<yellow>▶ Haz clic para comprar</yellow>"
            )
        ).toItemStack()

        menu.setItem(slot, btn) { p, _ ->
            executePurchase(p, shopId, item, false, qty)
        }
    }

    private fun setupSellButton(menu: CustomMenu, slot: Int, qty: Int, item: ShopItemConfig, shopId: String) {
        if (!item.allowSell) {
            val disabled = ItemConfig(material = "RED_STAINED_GLASS_PANE", displayName = "<red>Venta Deshabilitada</red>").toItemStack()
            menu.setItem(slot, disabled)
            return
        }

        val sim = shopManager.simulateBulkSell(item, qty)
        val totalVal = sim.first

        val btn = ItemConfig(
            material = "RED_STAINED_GLASS_PANE",
            displayName = "<red><bold>Vender $qty</bold></red>",
            lore = listOf(
                "<gray>Vende una cantidad de $qty unidades.</gray>",
                "",
                "<gray>Valor Total: <red>${economyService.formatBalance(shopManager.marketConfig.currencyId, totalVal)}</red>",
                "",
                "<yellow>▶ Haz clic para vender</yellow>"
            )
        ).toItemStack()

        menu.setItem(slot, btn) { p, _ ->
            executeSale(p, shopId, item, false, qty)
        }
    }

    // --- PIPELINE DE TRANSACCIÓN DE COMPRA COMPLETO Y SEGURO ---
    private fun executePurchase(player: Player, shopId: String, itemConfig: ShopItemConfig, isMax: Boolean, qty: Int = 0) {
        val uuid = player.uniqueId
        if (!TransactionLockManager.acquire(uuid)) {
            player.sendMessage(ColorUtility.parse(getMsg("locked")))
            return
        }

        val baseItem = ItemConfig(
            material = itemConfig.material,
            customModelData = itemConfig.customModelData,
            enchantments = itemConfig.enchantments
        ).toItemStack()

        // 1. Validar Espacio en Inventario (Main Thread)
        val space = getInventorySpace(player, baseItem)
        if (space <= 0) {
            player.sendMessage(ColorUtility.parse(getMsg("no-space")))
            TransactionLockManager.release(uuid)
            return
        }

        val finalQty = if (isMax) space else qty
        if (finalQty <= 0 || (!isMax && space < qty)) {
            player.sendMessage(ColorUtility.parse(getMsg("no-space-qty", "qty" to finalQty.toString())))
            TransactionLockManager.release(uuid)
            return
        }

        // 2. Simular Compra y Validar Fondos (Main Thread)
        val playerBal = economyService.getBalance(uuid, shopManager.marketConfig.currencyId)
        val simRes = shopManager.simulateBulkBuy(itemConfig, finalQty)
        var totalCost = simRes.first
        var actualQty = finalQty

        if (playerBal < totalCost) {
            if (isMax) {
                // Ajustar al máximo que puede costear
                val adjMax = calculateMaxPurchase(player, itemConfig, playerBal)
                actualQty = adjMax.first
                totalCost = adjMax.second
                if (actualQty <= 0) {
                    player.sendMessage(ColorUtility.parse(getMsg("no-funds")))
                    TransactionLockManager.release(uuid)
                    return
                }
            } else {
                player.sendMessage(ColorUtility.parse(getMsg("no-funds")))
                TransactionLockManager.release(uuid)
                return
            }
        }

        // 3. Modificar balance en Caché y Delegar base de datos de forma asíncrona (Síncrono/Atómico en Caché)
        economyService.withdrawCacheBehind(uuid, shopManager.marketConfig.currencyId, totalCost, "SHOP_BUY")
            .thenAccept { success ->
                if (success) {
                    // 4. SQL Commit Exitoso -> Entregar ítems físicos de forma segura (Main Thread)
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        try {
                            val itemStack = baseItem.clone()
                            itemStack.amount = actualQty
                            player.inventory.addItem(itemStack)
                            
                            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
                            player.sendMessage(ColorUtility.parse(
                                getMsg("buy-success", 
                                    "qty" to actualQty.toString(), 
                                    "price" to economyService.formatBalance(shopManager.marketConfig.currencyId, totalCost)
                                )
                            ))
                            
                            // Registrar volumen de mercado
                            val itemId = shopManager.getItemId(itemConfig)
                            shopManager.recordTransaction(player, shopId, itemId, "BUY", actualQty, totalCost)
                        } finally {
                            TransactionLockManager.release(uuid)
                            // Refrescar GUI
                            openQuantitySubGui(player, shopId, itemConfig)
                        }
                    })
                } else {
                    // 5. SQL Commit Fallido -> Reversión automática hecha por EconomyService.kt
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        player.sendMessage(ColorUtility.parse(getMsg("error-db")))
                        TransactionLockManager.release(uuid)
                    })
                }
            }
    }

    // --- PIPELINE DE TRANSACCIÓN DE VENTA COMPLETO Y SEGURO ---
    private fun executeSale(player: Player, shopId: String, itemConfig: ShopItemConfig, isAll: Boolean, qty: Int = 0) {
        val uuid = player.uniqueId
        if (!TransactionLockManager.acquire(uuid)) {
            player.sendMessage(ColorUtility.parse(getMsg("locked")))
            return
        }

        val baseItem = ItemConfig(
            material = itemConfig.material,
            customModelData = itemConfig.customModelData,
            enchantments = itemConfig.enchantments
        ).toItemStack()

        // 1. Obtener y Validar ítems físicos que posee en Inventario (Main Thread)
        val ownedItems = mutableListOf<Pair<ItemStack, BigDecimal>>() // stack to durability factor
        val inv = player.inventory
        for (i in 0 until 36) {
            val item = inv.getItem(i)
            if (item != null && isMatch(item, baseItem) && !isBlockedByPdc(item)) {
                ownedItems.add(Pair(item, getDurabilityFactor(item)))
            }
        }

        val totalOwnedCount = ownedItems.sumOf { it.first.amount }
        if (totalOwnedCount <= 0) {
            player.sendMessage(ColorUtility.parse(getMsg("no-items")))
            TransactionLockManager.release(uuid)
            return
        }

        val finalQty = if (isAll) totalOwnedCount else qty
        if (finalQty <= 0 || (!isAll && totalOwnedCount < qty)) {
            player.sendMessage(ColorUtility.parse(getMsg("no-items-qty", "qty" to finalQty.toString())))
            TransactionLockManager.release(uuid)
            return
        }

        // 2. Simular Venta progresiva aplicando durabilidad individual (Main Thread)
        val simRes = shopManager.simulateBulkSell(itemConfig, finalQty)
        val unitPrices = simRes.second

        // Distribuir el descuento por durabilidad para cada ítem vendido
        var totalPayout = BigDecimal.ZERO
        
        // Clonar lista de poseídos para simular remoción
        val removePlan = mutableListOf<Pair<Int, Int>>() // slot to amount to remove
        val durabilityFactors = mutableListOf<BigDecimal>()

        var remainingToSell = finalQty
        for (i in 0 until 36) {
            if (remainingToSell <= 0) break
            val item = inv.getItem(i)
            if (item != null && isMatch(item, baseItem) && !isBlockedByPdc(item)) {
                val toTake = minOf(item.amount, remainingToSell)
                removePlan.add(Pair(i, toTake))
                
                val factor = getDurabilityFactor(item)
                for (j in 0 until toTake) {
                    durabilityFactors.add(factor)
                }
                
                remainingToSell -= toTake
            }
        }

        // Calcular el valor final escalado por durabilidad
        for (i in 0 until finalQty) {
            val factor = durabilityFactors.getOrElse(i) { BigDecimal.ONE }
            val unitPrice = unitPrices[i]
            val scaledPrice = unitPrice.multiply(factor).setScale(2, RoundingMode.HALF_UP)
            totalPayout = totalPayout.add(scaledPrice)
        }

        // 3. Depositar fondos en caché y delegar base de datos de forma asíncrona (Síncrono/Atómico en Caché)
        economyService.depositCacheBehind(uuid, shopManager.marketConfig.currencyId, totalPayout, "SHOP_SELL")
            .thenAccept { success ->
                if (success) {
                    // 4. SQL Commit Exitoso -> Remover físicamente los ítems del inventario (Main Thread)
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        try {
                            removePlan.forEach { (slot, amount) ->
                                val item = inv.getItem(slot)
                                if (item != null) {
                                    if (item.amount <= amount) {
                                        inv.setItem(slot, null)
                                    } else {
                                        item.amount -= amount
                                    }
                                }
                            }

                            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
                            player.sendMessage(ColorUtility.parse(
                                getMsg("sell-success", 
                                    "qty" to finalQty.toString(), 
                                    "payout" to economyService.formatBalance(shopManager.marketConfig.currencyId, totalPayout)
                                )
                            ))

                            // Registrar volumen de mercado
                            val itemId = shopManager.getItemId(itemConfig)
                            shopManager.recordTransaction(player, shopId, itemId, "SELL", finalQty, totalPayout)
                        } finally {
                            TransactionLockManager.release(uuid)
                            // Refrescar GUI
                            openQuantitySubGui(player, shopId, itemConfig)
                        }
                    })
                } else {
                    // 5. SQL Commit Fallido -> Reversión automática hecha por EconomyService.kt
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        player.sendMessage(ColorUtility.parse(getMsg("error-db")))
                        TransactionLockManager.release(uuid)
                    })
                }
            }
    }

    // --- MÉTODOS UTILITARIOS DE INVENTARIO Y NBT ---
    private fun getInventorySpace(player: Player, item: ItemStack): Int {
        var space = 0
        val inv = player.inventory
        for (i in 0 until 36) {
            val current = inv.getItem(i)
            if (current == null || current.type == Material.AIR) {
                space += item.maxStackSize
            } else if (isMatch(current, item)) {
                space += (item.maxStackSize - current.amount).coerceAtLeast(0)
            }
        }
        return space
    }

    private fun getPlayerItemCount(player: Player, item: ItemStack): Int {
        var count = 0
        val inv = player.inventory
        for (i in 0 until 36) {
            val current = inv.getItem(i)
            if (current != null && isMatch(current, item) && !isBlockedByPdc(current)) {
                count += current.amount
            }
        }
        return count
    }

    private fun isMatch(playerItem: ItemStack, shopItem: ItemStack): Boolean {
        if (playerItem.type != shopItem.type) return false

        val playerMeta = playerItem.itemMeta ?: return shopItem.itemMeta == null
        val shopMeta = shopItem.itemMeta ?: return true

        // Comparar CustomModelData
        val playerCmd = if (playerMeta.hasCustomModelData()) playerMeta.customModelData else null
        val shopCmd = if (shopMeta.hasCustomModelData()) shopMeta.customModelData else null
        if (playerCmd != shopCmd) return false

        // Comparar Encantamientos
        for ((enchant, level) in shopMeta.enchants) {
            if (playerMeta.getEnchantLevel(enchant) != level) return false
        }

        return true
    }

    private fun getDurabilityFactor(itemStack: ItemStack): BigDecimal {
        val meta = itemStack.itemMeta
        if (meta is Damageable) {
            val maxDurability = itemStack.type.maxDurability.toInt()
            if (maxDurability > 0) {
                val remaining = maxDurability - meta.damage
                val factor = remaining.toDouble() / maxDurability.toDouble()
                return BigDecimal(factor).coerceIn(BigDecimal.ZERO, BigDecimal.ONE)
            }
        }
        return BigDecimal.ONE
    }

    private fun calculateMaxPurchase(player: Player, item: ShopItemConfig, playerBalance: BigDecimal): Pair<Int, BigDecimal> {
        val baseItem = ItemConfig(
            material = item.material,
            customModelData = item.customModelData,
            enchantments = item.enchantments
        ).toItemStack()
        
        val space = getInventorySpace(player, baseItem)
        if (space <= 0) return Pair(0, BigDecimal.ZERO)

        val itemId = shopManager.getItemId(item)
        var currentBuys = shopManager.buyVolumeCache[itemId] ?: 0
        val sells = shopManager.sellVolumeCache[itemId] ?: 0
        val basePrice = BigDecimal(item.basePrice)
        val k = item.saturationConstant ?: shopManager.marketConfig.defaultSaturation
        val floorPct = item.priceFloorPercent ?: shopManager.marketConfig.defaultPriceFloorPercent
        val ceilingPct = item.priceCeilingPercent ?: shopManager.marketConfig.defaultPriceCeilingPercent

        val floorMultiplier = BigDecimal(floorPct).divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
        val ceilingMultiplier = BigDecimal(ceilingPct).divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)

        var totalPrice = BigDecimal.ZERO
        var count = 0

        for (i in 0 until space) {
            val diff = currentBuys - sells
            val multiplier = BigDecimal.ONE.add(BigDecimal(diff).divide(BigDecimal(k), 4, RoundingMode.HALF_UP))
            val clampedMultiplier = multiplier.coerceIn(floorMultiplier, ceilingMultiplier)
            val unitPrice = basePrice.multiply(clampedMultiplier).setScale(2, RoundingMode.HALF_UP)

            if (totalPrice.add(unitPrice) > playerBalance) {
                break
            }
            totalPrice = totalPrice.add(unitPrice)
            count++
            currentBuys++
        }
        return Pair(count, totalPrice)
    }

    private fun isBlockedByPdc(itemStack: ItemStack): Boolean {
        val meta = itemStack.itemMeta ?: return false
        val pdc = meta.persistentDataContainer
        for (blockedKeyStr in shopManager.marketConfig.blockedSellPdcKeys) {
            val namespacedKey = if (blockedKeyStr.contains(":")) {
                val parts = blockedKeyStr.split(":", limit = 2)
                NamespacedKey(parts[0].lowercase(), parts[1].lowercase())
            } else {
                NamespacedKey("coreplugin", blockedKeyStr.lowercase())
            }
            if (pdc.has(namespacedKey, PersistentDataType.BOOLEAN)) {
                if (pdc.get(namespacedKey, PersistentDataType.BOOLEAN) == true) {
                    return true
                }
            }
            if (pdc.has(namespacedKey, PersistentDataType.STRING)) {
                val valStr = pdc.get(namespacedKey, PersistentDataType.STRING)
                if (valStr.equals("true", ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }
}
