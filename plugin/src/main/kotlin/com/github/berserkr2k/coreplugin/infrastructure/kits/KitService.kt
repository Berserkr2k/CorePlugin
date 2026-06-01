package com.github.berserkr2k.coreplugin.infrastructure.kits

import com.github.berserkr2k.coreplugin.infrastructure.database.DatabaseService
import com.github.berserkr2k.coreplugin.infrastructure.database.*
import com.github.berserkr2k.coreplugin.infrastructure.economy.EconomyService
import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.enchantments.Enchantment
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.io.File
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture

sealed class ClaimResult {
    data class Success(val message: String) : ClaimResult()
    data class Failure(val reason: String) : ClaimResult()
}

class KitService(
    private val plugin: Plugin,
    private val configManager: ModularConfigManager,
    private val databaseService: DatabaseService,
    private val economyService: EconomyService,
    private val messagesConfig: MessagesConfig
) : Listener {
    val kits = ConcurrentHashMap<String, KitConfig>()
    val cooldownCache = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>>()
    
    private val kitsFolder = plugin.dataFolder.resolve("kits")

    init {
        createTables()
        loadAllKits()
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    private fun createTables() {
        databaseService.initFuture.thenAccept {
            val sql = """
                CREATE TABLE IF NOT EXISTS player_kit_cooldowns (
                    uuid VARCHAR(36) NOT NULL,
                    kit_name VARCHAR(64) NOT NULL,
                    claimed_at BIGINT NOT NULL,
                    PRIMARY KEY (uuid, kit_name)
                );
            """.trimIndent()
            databaseService.execute(sql)
        }.exceptionally { e ->
            plugin.logger.severe("Fallo al crear la tabla de cooldowns de kits: ${e.message}")
            null
        }
    }

    fun loadAllKits() {
        kits.clear()
        if (!kitsFolder.exists()) {
            kitsFolder.mkdirs()
        }

        val starterFile = kitsFolder.resolve("starter.conf")
        if (!starterFile.exists()) {
            configManager.loadModuleConfig("kits/starter.conf", KitConfig::class.java, KitConfig()).join()
        }

        val confFiles = kitsFolder.listFiles { _, name -> name.endsWith(".conf") } ?: emptyArray()

        for (file in confFiles) {
            val kitId = file.nameWithoutExtension.lowercase()
            try {
                // Delegar al ModularConfigManager centralizado
                val kitConfig = configManager.loadModuleConfig("kits/${file.name}", KitConfig::class.java, KitConfig()).join()
                kits[kitId] = kitConfig
            } catch (e: Exception) {
                plugin.logger.severe("Error al cargar el kit desde ${file.name}: ${e.message}")
            }
        }
        plugin.logger.info("¡Se han cargado ${kits.size} kits con éxito!")
    }

    fun loadPlayerCooldowns(uuid: UUID): CompletableFuture<Void> {
        val sql = "SELECT kit_name, claimed_at FROM player_kit_cooldowns WHERE uuid = ?"
        return databaseService.queryAsync(
            sql,
            preparer = { stmt -> stmt.setString(1, uuid.toString()) },
            mapper = { rs -> rs.getString(1).lowercase() to rs.getLong(2) }
        ).thenAccept { results ->
            val playerCooldowns = ConcurrentHashMap<String, Long>()
            results.forEach { (kitName, claimedAt) ->
                playerCooldowns[kitName] = claimedAt
            }
            cooldownCache[uuid] = playerCooldowns
        }
    }

    fun unloadPlayerCooldowns(uuid: UUID) {
        cooldownCache.remove(uuid)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        loadPlayerCooldowns(event.player.uniqueId)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        unloadPlayerCooldowns(event.player.uniqueId)
    }

    fun getRemainingCooldown(uuid: UUID, kitId: String): Long {
        val config = kits[kitId.lowercase()] ?: return 0L
        val playerCooldowns = cooldownCache[uuid] ?: return 0L
        val lastClaimed = playerCooldowns[kitId.lowercase()] ?: return 0L
        val elapsed = (System.currentTimeMillis() - lastClaimed) / 1000L
        val remaining = config.cooldownSeconds - elapsed
        return if (remaining > 0) remaining else 0L
    }

    fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return when {
            h > 0 -> "${h}h ${m}m ${s}s"
            m > 0 -> "${m}m ${s}s"
            else -> "${s}s"
        }
    }

    fun buildItemStack(config: KitConfig.KitItem): ItemStack? {
        val mat = Material.matchMaterial(config.material.uppercase()) ?: return null
        
        val builder = com.github.berserkr2k.coreplugin.common.gui.ItemBuilder(mat, config.amount)
        if (config.displayName != null && config.displayName.isNotEmpty()) {
            builder.displayName(config.displayName)
        }
        if (config.lore.isNotEmpty()) {
            builder.lore(config.lore)
        }
        config.enchantments.forEach { (enchantName, level) ->
            @Suppress("DEPRECATION")
            val enchant = Enchantment.getByName(enchantName.uppercase()) 
                ?: Enchantment.getByKey(NamespacedKey.minecraft(enchantName.lowercase()))
            if (enchant != null) {
                builder.enchant(enchant, level)
            }
        }
        builder.unbreakable(config.unbreakable)
        if (config.customModelData != null) {
            builder.customModelData(config.customModelData)
        }
        
        return builder.build()
    }

    private fun calculateRequiredSlots(player: Player, items: List<ItemStack>): Int {
        val tempInv = Bukkit.createInventory(null, 36)
        player.inventory.storageContents.forEachIndexed { i, stack ->
            if (stack != null && stack.type != Material.AIR) {
                tempInv.setItem(i, stack.clone())
            }
        }
        
        var neededSlots = 0
        for (item in items) {
            val leftovers = tempInv.addItem(item.clone())
            if (leftovers.isNotEmpty()) {
                neededSlots += 999 
            }
        }
        
        val playerEmpty = player.inventory.storageContents.count { it == null || it.type == Material.AIR }
        val tempEmpty = tempInv.storageContents.count { it == null || it.type == Material.AIR }
        val slotsUsed = playerEmpty - tempEmpty
        return maxOf(0, slotsUsed + neededSlots)
    }

    fun claimKit(player: Player, kitId: String, isGift: Boolean): CompletableFuture<ClaimResult> {
        return CompletableFuture.supplyAsync({
            val config = kits[kitId.lowercase()] ?: return@supplyAsync ClaimResult.Failure(messagesConfig.utility["kit-not-found"] ?: "<red>El kit especificado no existe.</red>")
            
            if (!isGift) {
                if (!player.hasPermission(config.permission)) {
                    return@supplyAsync ClaimResult.Failure(messagesConfig.utility["no-permission"] ?: "<red>No tienes permiso para esto.</red>")
                }

                val remaining = getRemainingCooldown(player.uniqueId, kitId)
                val hasBypass = player.hasPermission("core.kits.bypass.cooldown") || 
                                player.hasPermission("core.kits.bypass.cooldown.${kitId.lowercase()}")
                if (remaining > 0 && !hasBypass) {
                    val formattedTime = formatTime(remaining)
                    val msg = (messagesConfig.utility["kit-cooldown"] ?: "<red>Debes esperar <time> para volver a reclamar este kit.</red>")
                        .replace("<time>", formattedTime)
                    return@supplyAsync ClaimResult.Failure(msg)
                }

                if (config.cost > 0.0) {
                    val costBD = BigDecimal(config.cost)
                    val currentBal = economyService.getBalance(player.uniqueId, config.currency)
                    if (currentBal < costBD) {
                        return@supplyAsync ClaimResult.Failure(messagesConfig.utility["kit-insufficient-funds"] ?: "<red>No tienes dinero suficiente para comprar este kit.</red>")
                    }
                }
            }

            val itemStacks = config.items.mapNotNull { buildItemStack(it) }
            val requiredSlots = calculateRequiredSlots(player, itemStacks)

            val emptySlots = player.inventory.storageContents.count { it == null || it.type == Material.AIR }
            if (emptySlots < requiredSlots) {
                return@supplyAsync ClaimResult.Failure((messagesConfig.utility["kit-no-space"] ?: "<red>No tienes espacio suficiente en el inventario (<required> ranuras libres requeridas).</red>").replace("<required>", requiredSlots.toString()))
            }

            if (!isGift && config.cost > 0.0) {
                val costBD = BigDecimal(config.cost)
                val success = economyService.withdrawCacheBehind(player.uniqueId, config.currency, costBD, "KIT_PURCHASE_$kitId").join()
                if (!success) {
                    return@supplyAsync ClaimResult.Failure(messagesConfig.utility["kit-purchase-failed"] ?: "<red>Hubo un fallo al procesar la compra del kit.</red>")
                }
            }

            val now = System.currentTimeMillis()
            try {
                databaseService.execute("REPLACE INTO player_kit_cooldowns (uuid, kit_name, claimed_at) VALUES (?, ?, ?)") { ps ->
                    ps.setString(1, player.uniqueId.toString())
                    ps.setString(2, kitId.lowercase())
                    ps.setLong(3, now)
                }
                cooldownCache.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }[kitId.lowercase()] = now
            } catch (e: Exception) {
                plugin.logger.severe("Fallo al persistir cooldown para ${player.name} en el kit $kitId: ${e.message}")
            }

            Bukkit.getRegionScheduler().run(plugin, player.location) { _ ->
                itemStacks.forEach { item ->
                    player.inventory.addItem(item)
                }

                config.commands.forEach { cmdTemplate ->
                    val resolvedCmd = cmdTemplate.replace("%player_name%", player.name)
                    if (resolvedCmd.startsWith("console:", ignoreCase = true)) {
                        val command = resolvedCmd.substring("console:".length).trim()
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
                    } else if (resolvedCmd.startsWith("player:", ignoreCase = true)) {
                        val command = resolvedCmd.substring("player:".length).trim()
                        player.performCommand(command)
                    }
                }

                try {
                    val sound = Sound.valueOf(config.effects.sound.uppercase())
                    player.playSound(player.location, sound, 1.0f, 1.0f)
                } catch (e: Exception) {
                }

                try {
                    val particle = Particle.valueOf(config.effects.particle.uppercase())
                    player.spawnParticle(particle, player.location.add(0.0, 1.0, 0.0), 15, 0.5, 0.5, 0.5, 0.05)
                } catch (e: Exception) {
                }
            }

            ClaimResult.Success(messagesConfig.utility["kit-claimed"] ?: "<green>¡Has reclamado tu kit con éxito!</green>")
        }, { command -> Bukkit.getAsyncScheduler().runNow(plugin) { _ -> command.run() } })
    }
}
