package com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop

import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import com.github.berserkr2k.coreplugin.api.framework.menu.*
import com.github.berserkr2k.coreplugin.api.framework.item.*
import com.github.berserkr2k.coreplugin.api.config.ItemConfig
import com.github.berserkr2k.coreplugin.api.framework.economy.EconomyService
import com.github.berserkr2k.coreplugin.common.TransactionLockManager
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
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

import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler

class ShopGuis(
    private val plugin: Plugin,
    private val shopManager: ShopManager,
    private val messageService: MessageService,
    private val menuService: MenuService,
    private val itemFactory: ItemBuilderFactory
) {
    private val serviceRegistry = org.bukkit.Bukkit.getServicesManager().load(com.github.berserkr2k.coreplugin.api.di.ServiceRegistry::class.java)
        ?: throw IllegalStateException("ServiceRegistry not found in ServicesManager")

    private val economyService = serviceRegistry.get(EconomyService::class.java)
        ?: throw IllegalStateException("EconomyService not found")

    private val regionTaskScheduler = serviceRegistry.get(RegionTaskScheduler::class.java)!!
    private val itemBuilderFactory = itemFactory

    init {
        // Registrar acciones dinámicas para abrir las tiendas de forma local al instanciar el menú
    }

    private fun getMsg(key: String, vararg placeholders: Pair<String, Any>): String {
        val enumKey = try {
            ShopMessages.valueOf(key.uppercase().replace("-", "_"))
        } catch (e: Exception) {
            null
        }
        if (enumKey == null) {
            plugin.logger.warning("No se encontró clave enum ShopMessages para '$key'")
            return ""
        }
        val raw = messageService.getRawTemplate(enumKey)
        var msg = raw
        for (ph in placeholders) {
            msg = msg.replace("<${ph.first}>", ph.second.toString())
        }
        return msg
    }


    fun openCategoriesMenu(player: Player) {
        if (!shopManager.marketReady) {
            messageService.send(player, ShopMessages.MARKET_REGULATING)
            return
        }
 
        val config = shopManager.marketConfig.categoriesMenu
        val title = ColorUtility.parse(config.title)
        val size = config.size
        
        val builder = menuService.createBuilder()
            .title(title)
            .slots(size)
        
        // Registrar acciones locales dinámicas asociadas al menú
        config.items.forEach { (shopId, _) ->
            if (shopId == "history") {
                builder.registerAction("open_shop_history") { p ->
                    openHistoryMenu(p)
                }
            } else {
                builder.registerAction("open_shop_$shopId") { p ->
                    openCategoryMenu(p, shopId)
                }
            }
        }
 
        builder.loadFromConfig(config)
        builder.build().open(player)
    }

    fun openHistoryMenu(player: Player) {
        if (!shopManager.marketReady) {
            messageService.send(player, ShopMessages.MARKET_REGULATING)
            return
        }

        shopManager.getPlayerTransactionHistory(player.uniqueId).thenAccept { history ->
            regionTaskScheduler.runAtEntity(player, Runnable {
                if (!player.isOnline) return@Runnable

                val config = shopManager.marketConfig.historyMenu
                val title = ColorUtility.parse(config.title)
                val size = config.size
                val builder = menuService.createBuilder()
                    .title(title)
                    .slots(size)

                // Fondo de cristal gris (o el configurado en filler)
                if (config.filler.enabled) {
                    val filler = itemBuilderFactory.builder(config.filler.item).build()
                    val fillerBtn = Button.builder().icon(filler).build()
                    builder.fill(fillerBtn)
                }

                // Botón de retorno al selector
                val backMatStr = getMsg("back-item-material")
                val backMat = Material.matchMaterial(backMatStr) ?: Material.BARRIER
                val backName = getMsg("back-item-name")
                val backLore = listOf(getMsg("back-item-lore"))

                val backItem = itemFactory.create(backMat)
                    .name(ColorUtility.parse(backName))
                    .lore(backLore.map { ColorUtility.parse(it) })
                    .build()

                val backSlot = size - 5
                val backBtn = Button.builder()
                    .icon(backItem)
                    .onClick { p -> openCategoriesMenu(p) }
                    .build()
                builder.button(backSlot, backBtn)

                // Elementos paginados
                builder.placePaginatedItems(
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

                    val item = itemFactory.create(mat)
                        .name(ColorUtility.parse(displayName))
                        .lore(lore.map { ColorUtility.parse(it) })
                        .build()

                    val btn = Button.builder().icon(item).build()
                    builder.button(slot, btn)
                }

                builder.build().open(player)
            })
        }.exceptionally { ex ->
            plugin.logger.severe("Fallo al abrir historial de transacciones para ${player.name}: ${ex.message}")
            messageService.send(player, ShopMessages.HISTORY_ERROR)
            null
        }
    }

    fun openCategoryMenu(player: Player, shopId: String) {
        if (!shopManager.marketReady) {
            messageService.send(player, ShopMessages.MARKET_REGULATING)
            return
        }
 
        val categoryConfig = shopManager.categories[shopId] ?: return
        val size = categoryConfig.guiSize
        val title = ColorUtility.parse(categoryConfig.displayName)
 
        val builder = menuService.createBuilder()
            .title(title)
            .slots(size)
 
        // Fondo de cristal gris
        val filler = itemBuilderFactory.builder(
            ItemConfig(material = "GRAY_STAINED_GLASS_PANE", displayName = " ")
        ).build()
        val fillerBtn = Button.builder().icon(filler).build()
        builder.fill(fillerBtn)
 
        // Botón de retorno al selector configurable
        val backMatStr = categoryConfig.backItemMaterial ?: getMsg("back-item-material")
        val backMat = Material.matchMaterial(backMatStr) ?: Material.BARRIER
        val backName = categoryConfig.backItemName ?: getMsg("back-item-name")
        val backLoreStr = if (categoryConfig.backItemLore.isNotEmpty()) categoryConfig.backItemLore else listOf(getMsg("back-item-lore"))
 
        val backItem = itemBuilderFactory.builder(
            ItemConfig(
                material = backMat.name,
                displayName = backName,
                lore = backLoreStr
            )
        ).build()
 
        val backSlot = size - 5
        val backBtn = Button.builder()
            .icon(backItem)
            .onClick { p -> openCategoriesMenu(p) }
            .build()
        builder.button(backSlot, backBtn)
 
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
 
            builder.placePaginatedItems(
                dummyMenuConfig,
                categoryConfig.items,
                categoryConfig.previousPageItem,
                categoryConfig.nextPageItem
            ) { itemConfig, slot ->
                val baseItem = itemBuilderFactory.builder(
                    ItemConfig(
                        material = itemConfig.material,
                        customModelData = itemConfig.customModelData,
                        enchantments = itemConfig.enchantments
                    )
                ).build()
 
                val enrichedItem = enrichItemLore(itemConfig, baseItem)
                val btn = Button.builder()
                    .icon(enrichedItem)
                    .onClick { p -> openQuantitySubGui(p, shopId, itemConfig) }
                    .build()
                builder.button(slot, btn)
            }
        } else {
            // Cargar ítems de la categoría de forma clásica
            val occupiedSlots = mutableSetOf<Int>()
            occupiedSlots.add(backSlot)
 
            categoryConfig.items.forEach { itemConfig ->
                val baseItem = itemBuilderFactory.builder(
                    ItemConfig(
                        material = itemConfig.material,
                        customModelData = itemConfig.customModelData,
                        enchantments = itemConfig.enchantments
                    )
                ).build()
 
                val enrichedItem = enrichItemLore(itemConfig, baseItem)
                val suggestedSlot = itemConfig.guiSlot
 
                val targetSlot = if (suggestedSlot in 0 until size && suggestedSlot != backSlot) {
                    suggestedSlot
                } else {
                    var foundSlot = -1
                    for (s in 10 until size - 9) {
                        if (s == backSlot || s in occupiedSlots) continue
                        foundSlot = s
                        break
                    }
                    foundSlot
                }
 
                if (targetSlot != -1) {
                    occupiedSlots.add(targetSlot)
                    val btn = Button.builder()
                        .icon(enrichedItem)
                        .onClick { p -> openQuantitySubGui(p, shopId, itemConfig) }
                        .build()
                    builder.button(targetSlot, btn)
                }
            }
        }
 
        builder.build().open(player)
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

        val trend = if (netVolume > 0) getMsg("trend-up") else if (netVolume < 0) getMsg("trend-down") else getMsg("trend-stable")

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
        val title = ColorUtility.parse(getMsg("quantity-gui-title"))
        val builder = menuService.createBuilder()
            .title(title)
            .slots(27)
 
        // Rellenar fondo decorativo
        val filler = itemBuilderFactory.builder(
            ItemConfig(material = getMsg("quantity-gui-background-material"), displayName = " ")
        ).build()
        builder.fill(Button.builder().icon(filler).build())
 
        // Slot 4: Item previsualizador inerte con su lore de identidad enriquecido
        val baseItem = itemBuilderFactory.builder(
            ItemConfig(
                material = itemConfig.material,
                customModelData = itemConfig.customModelData,
                enchantments = itemConfig.enchantments
            )
        ).build()
        val previewItem = enrichItemLore(itemConfig, baseItem.clone())
        builder.button(4, Button.builder().icon(previewItem).build())
 
        // Slot 13: Divisor Central Tintado
        val divisor = itemBuilderFactory.builder(
            ItemConfig(material = getMsg("quantity-gui-divisor-material"), displayName = " ")
        ).build()
        builder.button(13, Button.builder().icon(divisor).build())
 
        // Slot 22: Botón de Volver configurable
        val backMatStr = getMsg("back-item-material")
        val backMat = Material.matchMaterial(backMatStr) ?: Material.BARRIER
        val backName = getMsg("back-item-name")
        val backLoreStr = listOf(getMsg("back-item-lore"))
 
        val backItem = itemBuilderFactory.builder(
            ItemConfig(
                material = backMat.name,
                displayName = backName,
                lore = backLoreStr
            )
        ).build()
 
        val backBtn = Button.builder()
            .icon(backItem)
            .onClick { p -> openCategoryMenu(p, shopId) }
            .build()
        builder.button(22, backBtn)
 
        // --- COMPRAS ( Slots 10, 11, 12 ) ---
        setupBuyButton(builder, 10, 1, itemConfig, shopId)
        setupBuyButton(builder, 11, 32, itemConfig, shopId)
        setupBuyButton(builder, 12, 64, itemConfig, shopId)
 
        // --- VENTAS ( Slots 16, 15, 14 ) ---
        setupSellButton(builder, 16, 1, itemConfig, shopId)
        setupSellButton(builder, 15, 32, itemConfig, shopId)
        setupSellButton(builder, 14, 64, itemConfig, shopId)
 
        // --- COMPRAR MÁXIMO ( Slot 21 ) ---
        if (itemConfig.allowBuy) {
            val playerBal = economyService.getBalance(player.uniqueId, shopManager.marketConfig.currencyId)
            val maxBuyRes = calculateMaxPurchase(player, itemConfig, playerBal)
            val count = maxBuyRes.first
            val totalCost = maxBuyRes.second
            
            val formattedPrice = economyService.formatBalance(shopManager.marketConfig.currencyId, totalCost)
            val rawLore = getMsg("buy-max-lore", "qty" to count.toString(), "price" to formattedPrice)
            val maxBuyItem = itemBuilderFactory.builder(
                ItemConfig(
                    material = getMsg("buy-max-material"),
                    displayName = getMsg("buy-max-name"),
                    lore = rawLore.split("\n")
                )
            ).build()
            
            val maxBuyBtn = Button.builder()
                .icon(maxBuyItem)
                .onClick { p -> executePurchase(p, shopId, itemConfig, true) }
                .build()
            builder.button(21, maxBuyBtn)
        }
 
        // --- VENDER TODO ( Slot 23 ) ---
        if (itemConfig.allowSell) {
            val itemsCount = getPlayerItemCount(player, baseItem)
            val sellRes = shopManager.simulateBulkSell(itemConfig, itemsCount)
            val totalVal = sellRes.first
 
            val formattedPrice = economyService.formatBalance(shopManager.marketConfig.currencyId, totalVal)
            val rawLore = getMsg("sell-all-lore", "qty" to itemsCount.toString(), "price" to formattedPrice)
            val sellAllItem = itemBuilderFactory.builder(
                ItemConfig(
                    material = getMsg("sell-all-material"),
                    displayName = getMsg("sell-all-name"),
                    lore = rawLore.split("\n")
                )
            ).build()
 
            val sellAllBtn = Button.builder()
                .icon(sellAllItem)
                .onClick { p -> executeSale(p, shopId, itemConfig, true) }
                .build()
            builder.button(23, sellAllBtn)
        }
 
        builder.build().open(player)
    }
 
    private fun setupBuyButton(builder: MenuBuilder, slot: Int, qty: Int, item: ShopItemConfig, shopId: String) {
        if (!item.allowBuy) {
            val disabled = itemBuilderFactory.builder(
                ItemConfig(material = getMsg("disabled-material"), displayName = getMsg("buy-disabled-name"))
            ).build()
            builder.button(slot, Button.builder().icon(disabled).build())
            return
        }
 
        val sim = shopManager.simulateBulkBuy(item, qty)
        val totalCost = sim.first
 
        val formattedPrice = economyService.formatBalance(shopManager.marketConfig.currencyId, totalCost)
        val rawLore = getMsg("buy-qty-lore", "qty" to qty.toString(), "price" to formattedPrice)
        val btn = itemBuilderFactory.builder(
            ItemConfig(
                material = getMsg("buy-qty-material"),
                displayName = getMsg("buy-qty-name").replace("<qty>", qty.toString()),
                lore = rawLore.split("\n")
            )
        ).build()
 
        val button = Button.builder()
            .icon(btn)
            .onClick { p -> executePurchase(p, shopId, item, false, qty) }
            .build()
        builder.button(slot, button)
    }
 
    private fun setupSellButton(builder: MenuBuilder, slot: Int, qty: Int, item: ShopItemConfig, shopId: String) {
        if (!item.allowSell) {
            val disabled = itemBuilderFactory.builder(
                ItemConfig(material = getMsg("disabled-material"), displayName = getMsg("sell-disabled-name"))
            ).build()
            builder.button(slot, Button.builder().icon(disabled).build())
            return
        }
 
        val sim = shopManager.simulateBulkSell(item, qty)
        val totalVal = sim.first
 
        val formattedPrice = economyService.formatBalance(shopManager.marketConfig.currencyId, totalVal)
        val rawLore = getMsg("sell-qty-lore", "qty" to qty.toString(), "price" to formattedPrice)
        val btn = itemBuilderFactory.builder(
            ItemConfig(
                material = getMsg("sell-qty-material"),
                displayName = getMsg("sell-qty-name").replace("<qty>", qty.toString()),
                lore = rawLore.split("\n")
            )
        ).build()
 
        val button = Button.builder()
            .icon(btn)
            .onClick { p -> executeSale(p, shopId, item, false, qty) }
            .build()
        builder.button(slot, button)
    }

    // --- PIPELINE DE TRANSACCIÓN DE COMPRA COMPLETO Y SEGURO ---
    private fun executePurchase(player: Player, shopId: String, itemConfig: ShopItemConfig, isMax: Boolean, qty: Int = 0) {
        val uuid = player.uniqueId
        if (!TransactionLockManager.acquire(uuid)) {
            messageService.send(player, ShopMessages.LOCKED)
            return
        }

        val baseItem = itemBuilderFactory.builder(
            ItemConfig(
                material = itemConfig.material,
                customModelData = itemConfig.customModelData,
                enchantments = itemConfig.enchantments
            )
        ).build()

        // 1. Validar Espacio en Inventario (Main Thread)
        val space = getInventorySpace(player, baseItem)
        if (space <= 0) {
            messageService.send(player, ShopMessages.NO_SPACE)
            TransactionLockManager.release(uuid)
            return
        }

        val finalQty = if (isMax) space else qty
        if (finalQty <= 0 || (!isMax && space < qty)) {
            messageService.send(player, ShopMessages.NO_SPACE_QTY, PlaceholderContext.of("qty" to finalQty.toString()))
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
                    messageService.send(player, ShopMessages.NO_FUNDS)
                    TransactionLockManager.release(uuid)
                    return
                }
            } else {
                messageService.send(player, ShopMessages.NO_FUNDS)
                TransactionLockManager.release(uuid)
                return
            }
        }

        // 3. Modificar balance en Caché y Delegar base de datos de forma asíncrona (Síncrono/Atómico en Caché)
        economyService.withdraw(uuid, totalCost, shopManager.marketConfig.currencyId)
            .thenAccept { success ->
                if (success) {
                    // 4. SQL Commit Exitoso -> Entregar ítems físicos de forma segura (Main Thread)
                    regionTaskScheduler.runAtEntity(player, Runnable {
                        try {
                            val itemStack = baseItem.clone()
                            itemStack.amount = actualQty
                            player.inventory.addItem(itemStack)
                            
                            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
                            messageService.send(
                                player,
                                ShopMessages.BUY_SUCCESS,
                                PlaceholderContext.of(
                                    "qty" to actualQty.toString(),
                                    "price" to economyService.formatBalance(shopManager.marketConfig.currencyId, totalCost)
                                )
                            )
                            
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
                    regionTaskScheduler.runAtEntity(player, Runnable {
                        messageService.send(player, ShopMessages.ERROR_DB)
                        TransactionLockManager.release(uuid)
                    })
                }
            }
    }

    // --- PIPELINE DE TRANSACCIÓN DE VENTA COMPLETO Y SEGURO ---
    private fun executeSale(player: Player, shopId: String, itemConfig: ShopItemConfig, isAll: Boolean, qty: Int = 0) {
        val uuid = player.uniqueId
        if (!TransactionLockManager.acquire(uuid)) {
            messageService.send(player, ShopMessages.LOCKED)
            return
        }

        val baseItem = itemBuilderFactory.builder(
            ItemConfig(
                material = itemConfig.material,
                customModelData = itemConfig.customModelData,
                enchantments = itemConfig.enchantments
            )
        ).build()

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
            messageService.send(player, ShopMessages.NO_ITEMS)
            TransactionLockManager.release(uuid)
            return
        }

        val finalQty = if (isAll) totalOwnedCount else qty
        if (finalQty <= 0 || (!isAll && totalOwnedCount < qty)) {
            messageService.send(player, ShopMessages.NO_ITEMS_QTY, PlaceholderContext.of("qty" to finalQty.toString()))
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
        economyService.deposit(uuid, totalPayout, shopManager.marketConfig.currencyId)
            .thenAccept { success ->
                if (success) {
                    // 4. SQL Commit Exitoso -> Remover físicamente los ítems del inventario (Main Thread)
                    regionTaskScheduler.runAtEntity(player, Runnable {
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
                            messageService.send(
                                player,
                                ShopMessages.SELL_SUCCESS,
                                PlaceholderContext.of(
                                    "qty" to finalQty.toString(),
                                    "payout" to economyService.formatBalance(shopManager.marketConfig.currencyId, totalPayout)
                                )
                            )

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
                    regionTaskScheduler.runAtEntity(player, Runnable {
                        messageService.send(player, ShopMessages.ERROR_DB)
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
        val baseItem = itemBuilderFactory.builder(
            ItemConfig(
                material = item.material,
                customModelData = item.customModelData,
                enchantments = item.enchantments
            )
        ).build()
        
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
