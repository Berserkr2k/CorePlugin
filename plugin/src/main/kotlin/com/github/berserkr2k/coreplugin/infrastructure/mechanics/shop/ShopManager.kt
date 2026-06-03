package com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop

import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import com.github.berserkr2k.coreplugin.infrastructure.database.DatabaseService
import com.github.berserkr2k.coreplugin.infrastructure.database.*
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ShopManager(
    private val plugin: Plugin,
    private val configManager: ModularConfigManager,
    private val databaseService: DatabaseService
) {
    lateinit var marketConfig: MarketConfig
        private set
    
    val categories = ConcurrentHashMap<String, ShopConfig>()
    val buyVolumeCache = ConcurrentHashMap<String, Int>()
    val sellVolumeCache = ConcurrentHashMap<String, Int>()
    
    @Volatile
    var marketReady = false
        private set

    init {
        loadConfigurations().thenRun {
            loadMarketVolumes().thenRun {
                startPurgeScheduler()
            }
        }
    }

    fun loadConfigurations(): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        configManager.loadModuleConfig("shops/market.conf", MarketConfig::class.java, MarketConfig())
            .thenAccept { loadedMarket ->
                this.marketConfig = loadedMarket
                
                // Crear carpetas de shops/categories si no existen
                val categoriesDir = File(plugin.dataFolder, "shops/categories")
                if (!categoriesDir.exists()) {
                    categoriesDir.mkdirs()
                    // Escribir categorías por defecto
                    writeDefaultCategory(File(categoriesDir, "blocks.conf"), "blocks", "<yellow>Bloques</yellow>", listOf(
                        ShopItemConfig("STONE", "2.0", guiSlot = 10),
                        ShopItemConfig("COBBLESTONE", "1.0", guiSlot = 11),
                        ShopItemConfig("OAK_LOG", "5.0", guiSlot = 12),
                        ShopItemConfig("OAK_PLANKS", "1.5", guiSlot = 13),
                        ShopItemConfig("GLASS", "3.0", guiSlot = 14)
                    ))
                    writeDefaultCategory(File(categoriesDir, "food.conf"), "food", "<gold>Alimentos</gold>", listOf(
                        ShopItemConfig("BREAD", "4.0", guiSlot = 11),
                        ShopItemConfig("COOKED_BEEF", "8.0", guiSlot = 13),
                        ShopItemConfig("GOLDEN_APPLE", "50.0", guiSlot = 15)
                    ))
                    writeDefaultCategory(File(categoriesDir, "minerals.conf"), "minerals", "<aqua>Minerales</aqua>", listOf(
                        ShopItemConfig("COAL", "5.0", guiSlot = 10),
                        ShopItemConfig("IRON_INGOT", "15.0", guiSlot = 11),
                        ShopItemConfig("GOLD_INGOT", "35.0", guiSlot = 12),
                        ShopItemConfig("DIAMOND", "120.0", guiSlot = 14),
                        ShopItemConfig("EMERALD", "150.0", guiSlot = 15),
                        ShopItemConfig("NETHERITE_INGOT", "800.0", guiSlot = 16)
                    ))
                }
                
                val files = categoriesDir.listFiles { _, name -> name.endsWith(".conf") } ?: emptyArray()
                val futures = files.map { file ->
                    val relativePath = "shops/categories/${file.name}"
                    configManager.loadModuleConfig(relativePath, ShopConfig::class.java, ShopConfig())
                        .thenAccept { category ->
                            categories[category.shopId] = category
                        }
                }
                CompletableFuture.allOf(*futures.toTypedArray()).thenRun {
                    future.complete(null)
                }.exceptionally { ex ->
                    future.completeExceptionally(ex)
                    null
                }
            }.exceptionally { ex ->
                future.completeExceptionally(ex)
                null
            }
        return future
    }

    private fun writeDefaultCategory(file: File, id: String, title: String, items: List<ShopItemConfig>) {
        val itemsStr = items.joinToString(",\n") { item ->
            """        {
            material = "${item.material}"
            basePrice = "${item.basePrice}"
            priceFloorPercent = null
            priceCeilingPercent = null
            saturationConstant = null
            spread = null
            guiSlot = ${item.guiSlot}
            allowBuy = true
            allowSell = true
            customModelData = null
            enchantments = {}
        }"""
        }
        
        file.writeText("""
            shopId = "$id"
            displayName = "$title"
            guiSize = 45
            items = [
$itemsStr
            ]
        """.trimIndent())
    }

    fun loadMarketVolumes(): CompletableFuture<Void> {
        val now = System.currentTimeMillis()
        val windowMillis = marketConfig.historyWindowHours * 60L * 60L * 1000L
        val threshold = now - windowMillis
        
        val query = "SELECT item_id, transaction_type, SUM(quantity) FROM market_transactions WHERE timestamp >= ? GROUP BY item_id, transaction_type"
        return databaseService.queryAsync(
            query,
            preparer = { ps -> ps.setLong(1, threshold) },
            mapper = { rs -> Triple(rs.getString(1), rs.getString(2), rs.getInt(3)) }
        ).thenAccept { results ->
            results.forEach { (itemId, type, sum) ->
                if (type.equals("BUY", ignoreCase = true)) {
                    buyVolumeCache[itemId] = sum
                } else if (type.equals("SELL", ignoreCase = true)) {
                    sellVolumeCache[itemId] = sum
                }
            }
            marketReady = true
            plugin.logger.info("¡Caché de volumen de mercado dinámico cargado con éxito! El mercado está listo.")
        }.exceptionally { e ->
            plugin.logger.severe("Fallo crítico al precargar volúmenes de mercado: ${e.message}")
            marketReady = true // Habilitar acceso de emergencia ante fallos catastróficos de DB
            null
        }
    }

    fun refreshMarketCache() {
        val now = System.currentTimeMillis()
        val windowMillis = marketConfig.historyWindowHours * 60L * 60L * 1000L
        val threshold = now - windowMillis
        
        val tempBuy = ConcurrentHashMap<String, Int>()
        val tempSell = ConcurrentHashMap<String, Int>()
        
        databaseService.transactionAsync { conn ->
            // Purga automática de registros inútiles mayores a 24 horas
            conn.prepareStatement("DELETE FROM market_transactions WHERE timestamp < ?").use { purgePs ->
                purgePs.setLong(1, threshold)
                purgePs.executeUpdate()
            }
            
            // Cargar volumen de transacciones de la ventana activa
            val query = "SELECT item_id, transaction_type, SUM(quantity) FROM market_transactions WHERE timestamp >= ? GROUP BY item_id, transaction_type"
            conn.prepareStatement(query).use { ps ->
                ps.setLong(1, threshold)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        val itemId = rs.getString(1)
                        val type = rs.getString(2)
                        val sum = rs.getInt(3)
                        if (type.equals("BUY", ignoreCase = true)) {
                            tempBuy[itemId] = sum
                        } else if (type.equals("SELL", ignoreCase = true)) {
                            tempSell[itemId] = sum
                        }
                    }
                }
            }
        }.thenRun {
            // Swap atómico y seguro de referencias
            buyVolumeCache.clear()
            buyVolumeCache.putAll(tempBuy)
            sellVolumeCache.clear()
            sellVolumeCache.putAll(tempSell)
            plugin.logger.fine("Caché de mercado dinámico de 24 horas actualizada de forma asíncrona.")
        }.exceptionally { e ->
            plugin.logger.severe("Fallo al actualizar el caché del mercado: ${e.message}")
            null
        }
    }

    private fun startPurgeScheduler() {
        val interval = marketConfig.purgeIntervalMinutes.toLong()
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _ ->
            refreshMarketCache()
        }, interval, interval, TimeUnit.MINUTES)
    }

    fun recordTransaction(shopId: String, itemId: String, type: String, quantity: Int): CompletableFuture<Void> {
        // Impacto síncrono local en memoria
        if (type.equals("BUY", ignoreCase = true)) {
            buyVolumeCache.merge(itemId, quantity) { old, new -> old + new }
        } else {
            sellVolumeCache.merge(itemId, quantity) { old, new -> old + new }
        }
        
        // Impacto asíncrono e inmutable en base de datos
        val sql = "INSERT INTO market_transactions (shop_id, item_id, transaction_type, quantity, timestamp) VALUES (?, ?, ?, ?, ?)"
        return databaseService.executeAsync(sql) { ps ->
            ps.setString(1, shopId)
            ps.setString(2, itemId)
            ps.setString(3, type.uppercase())
            ps.setInt(4, quantity)
            ps.setLong(5, System.currentTimeMillis())
        }.thenAccept { }.exceptionally { e ->
            plugin.logger.severe("Error de persistencia SQL en recordTransaction: ${e.message}")
            null as Void?
        }
    }

    fun getItemId(item: ShopItemConfig): String {
        return item.material.lowercase() + (if (item.customModelData != null) "_${item.customModelData}" else "")
    }

    fun getBuyPrice(item: ShopItemConfig): BigDecimal {
        val itemId = getItemId(item)
        val buys = buyVolumeCache[itemId] ?: 0
        val sells = sellVolumeCache[itemId] ?: 0
        val basePrice = BigDecimal(item.basePrice)
        
        val k = item.saturationConstant ?: marketConfig.defaultSaturation
        val floorPct = item.priceFloorPercent ?: marketConfig.defaultPriceFloorPercent
        val ceilingPct = item.priceCeilingPercent ?: marketConfig.defaultPriceCeilingPercent
        
        val diff = buys - sells
        val multiplier = BigDecimal.ONE.add(BigDecimal(diff).divide(BigDecimal(k), 4, RoundingMode.HALF_UP))
        
        val floorMultiplier = BigDecimal(floorPct).divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
        val ceilingMultiplier = BigDecimal(ceilingPct).divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
        
        val clampedMultiplier = multiplier.coerceIn(floorMultiplier, ceilingMultiplier)
        
        return basePrice.multiply(clampedMultiplier).setScale(2, RoundingMode.HALF_UP)
    }

    fun getSellPrice(item: ShopItemConfig): BigDecimal {
        val buyPrice = getBuyPrice(item)
        val spread = item.spread ?: marketConfig.defaultSpread
        val rawSell = buyPrice.multiply(BigDecimal.ONE.subtract(BigDecimal(spread)))
        
        val minDiff = BigDecimal("0.01")
        val maxSell = buyPrice.subtract(minDiff)
        
        val finalSell = if (rawSell > maxSell) maxSell else rawSell
        return if (finalSell < BigDecimal.ZERO) BigDecimal.ZERO else finalSell.setScale(2, RoundingMode.HALF_UP)
    }

    fun simulateBulkBuy(item: ShopItemConfig, quantity: Int): Pair<BigDecimal, List<BigDecimal>> {
        val itemId = getItemId(item)
        var currentBuys = buyVolumeCache[itemId] ?: 0
        val sells = sellVolumeCache[itemId] ?: 0
        val basePrice = BigDecimal(item.basePrice)
        
        val k = item.saturationConstant ?: marketConfig.defaultSaturation
        val floorPct = item.priceFloorPercent ?: marketConfig.defaultPriceFloorPercent
        val ceilingPct = item.priceCeilingPercent ?: marketConfig.defaultPriceCeilingPercent
        
        val floorMultiplier = BigDecimal(floorPct).divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
        val ceilingMultiplier = BigDecimal(ceilingPct).divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
        
        var totalPrice = BigDecimal.ZERO
        val unitPrices = ArrayList<BigDecimal>(quantity)
        
        for (i in 0 until quantity) {
            val diff = currentBuys - sells
            val multiplier = BigDecimal.ONE.add(BigDecimal(diff).divide(BigDecimal(k), 4, RoundingMode.HALF_UP))
            val clampedMultiplier = multiplier.coerceIn(floorMultiplier, ceilingMultiplier)
            
            val unitPrice = basePrice.multiply(clampedMultiplier).setScale(2, RoundingMode.HALF_UP)
            totalPrice = totalPrice.add(unitPrice)
            unitPrices.add(unitPrice)
            
            currentBuys++
        }
        return Pair(totalPrice, unitPrices)
    }

    fun simulateBulkSell(item: ShopItemConfig, quantity: Int): Pair<BigDecimal, List<BigDecimal>> {
        val itemId = getItemId(item)
        val buys = buyVolumeCache[itemId] ?: 0
        var currentSells = sellVolumeCache[itemId] ?: 0
        val basePrice = BigDecimal(item.basePrice)
        
        val k = item.saturationConstant ?: marketConfig.defaultSaturation
        val floorPct = item.priceFloorPercent ?: marketConfig.defaultPriceFloorPercent
        val ceilingPct = item.priceCeilingPercent ?: marketConfig.defaultPriceCeilingPercent
        val spread = item.spread ?: marketConfig.defaultSpread
        
        val floorMultiplier = BigDecimal(floorPct).divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
        val ceilingMultiplier = BigDecimal(ceilingPct).divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
        
        var totalValue = BigDecimal.ZERO
        val unitPrices = ArrayList<BigDecimal>(quantity)
        
        for (i in 0 until quantity) {
            val diff = buys - currentSells
            val multiplier = BigDecimal.ONE.add(BigDecimal(diff).divide(BigDecimal(k), 4, RoundingMode.HALF_UP))
            val clampedMultiplier = multiplier.coerceIn(floorMultiplier, ceilingMultiplier)
            
            val buyPrice = basePrice.multiply(clampedMultiplier).setScale(2, RoundingMode.HALF_UP)
            val rawSell = buyPrice.multiply(BigDecimal.ONE.subtract(BigDecimal(spread)))
            
            val minDiff = BigDecimal("0.01")
            val maxSell = buyPrice.subtract(minDiff)
            
            val unitSellPrice = if (rawSell > maxSell) maxSell else rawSell
            val finalUnitSell = if (unitSellPrice < BigDecimal.ZERO) BigDecimal.ZERO else unitSellPrice.setScale(2, RoundingMode.HALF_UP)
            
            totalValue = totalValue.add(finalUnitSell)
            unitPrices.add(finalUnitSell)
            
            currentSells++
        }
        return Pair(totalValue, unitPrices)
    }

    private fun BigDecimal.coerceIn(min: BigDecimal, max: BigDecimal): BigDecimal {
        if (this < min) return min
        if (this > max) return max
        return this
    }
}
