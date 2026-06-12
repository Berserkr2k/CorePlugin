package com.github.berserkr2k.coreplugin.infrastructure.kits

import com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService
import com.github.berserkr2k.coreplugin.api.core.state.StateContainer
import com.github.berserkr2k.coreplugin.api.core.state.StateContainerType
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import com.github.berserkr2k.coreplugin.api.core.config.FeatureConfig
import com.github.berserkr2k.coreplugin.api.feature.kits.ClaimResult
import com.github.berserkr2k.coreplugin.api.feature.economy.EconomyService
import com.github.berserkr2k.coreplugin.api.config.ItemConfig
import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilderFactory
import com.github.berserkr2k.coreplugin.api.core.user.ProfileRegistry
import com.github.berserkr2k.coreplugin.api.core.user.UserProfile
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.util.NamingSchemes
import java.io.File
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.filesystem.FeatureFolderProvider
import com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler
import com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler
import com.github.berserkr2k.coreplugin.api.core.scheduler.Task

class KitStateContainer(
    var activeWarmup: Task? = null
) : StateContainer

val KIT_STATE = StateContainerType { KitStateContainer() }

class KitService(
    private val plugin: Plugin,
    private val featureConfig: FeatureConfig,
    private val messageService: MessageService,
    private val taskScheduler: TaskScheduler,
    private val stateService: PlayerStateService
) : org.bukkit.event.Listener, com.github.berserkr2k.coreplugin.api.feature.kits.KitService, com.github.berserkr2k.coreplugin.api.core.lifecycle.Reloadable {
    val kits = ConcurrentHashMap<String, KitConfig>()
    
    private val registry: ServiceRegistry by lazy {
        Bukkit.getServicesManager().load(ServiceRegistry::class.java)
            ?: throw IllegalStateException("ServiceRegistry not found")
    }
    
    private val folderProvider by lazy { registry.get(FeatureFolderProvider::class.java)!! }
    private val kitsFolder by lazy { folderProvider.getFeatureFolder("kits").resolve("kits").toFile() }
    
    private val regionTaskScheduler by lazy { registry.get(RegionTaskScheduler::class.java) }
    private val itemBuilderFactory by lazy { registry.get(ItemBuilderFactory::class.java)!! }
    private val economyService by lazy { registry.get(EconomyService::class.java) }
    private val profileRegistry by lazy { registry.get(ProfileRegistry::class.java) }

    private val mapperFactory = ObjectMapper.factoryBuilder()
        .defaultNamingScheme(NamingSchemes.PASSTHROUGH)
        .build()

    init {
        loadAllKits()
    }

    override suspend fun reload() {
        loadAllKits()
    }

    private fun <T : Any> loadHoconFile(file: File, configClass: Class<T>, defaultInstance: T): T {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }
        val loader = HoconConfigurationLoader.builder()
            .path(file.toPath())
            .defaultOptions { options ->
                options.serializers { builder ->
                    builder.registerAnnotatedObjects(mapperFactory)
                }
            }
            .build()
        val root = loader.load()
        val mapper = mapperFactory.get(configClass)
        return if (root.empty()) {
            mapper.save(defaultInstance, root)
            loader.save(root)
            defaultInstance
        } else {
            mapper.load(root) ?: defaultInstance
        }
    }

    override fun loadAllKits() {
        kits.clear()
        if (!kitsFolder.exists()) {
            kitsFolder.mkdirs()
        }

        val starterFile = kitsFolder.resolve("starter.conf")
        if (!starterFile.exists()) {
            loadHoconFile(starterFile, KitConfig::class.java, KitConfig())
        }

        val confFiles = kitsFolder.listFiles { _, name -> name.endsWith(".conf") } ?: emptyArray()

        for (file in confFiles) {
            val kitId = file.nameWithoutExtension.lowercase()
            try {
                val kitConfig = loadHoconFile(file, KitConfig::class.java, KitConfig())
                kits[kitId] = kitConfig
            } catch (e: Exception) {
                plugin.logger.severe("Error al cargar el kit desde ${file.name}: ${e.message}")
            }
        }
        plugin.logger.info("¡Se han cargado ${kits.size} kits con éxito!")
    }

    override fun getRemainingCooldown(uuid: UUID, kitId: String): Long {
        val config = kits[kitId.lowercase()] ?: return 0L
        val profile = profileRegistry.getProfile(uuid) ?: return 0L
        val lastClaimed = profile.getCooldown(kitId)
        val elapsed = (System.currentTimeMillis() - lastClaimed) / 1000L
        val remaining = config.cooldownSeconds - elapsed
        return if (remaining > 0) remaining else 0L
    }

    override fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return when {
            h > 0 -> "${h}h ${m}m ${s}s"
            m > 0 -> "${m}m ${s}s"
            else -> "${s}s"
        }
    }

    fun buildItemStack(config: ItemConfig): ItemStack? {
        return try {
            itemBuilderFactory.builder(config).build()
        } catch (e: Exception) {
            null
        }
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

    override fun claimKit(player: Player, kitId: String, isGift: Boolean): CompletableFuture<ClaimResult> {
        return CompletableFuture.supplyAsync({
            val config = kits[kitId.lowercase()] ?: return@supplyAsync ClaimResult.Failure(messageService.getRawTemplate(KitMessages.NOT_FOUND).ifEmpty { "<red>El kit especificado no existe.</red>" })
            val profile = profileRegistry.getProfile(player.uniqueId) ?: return@supplyAsync ClaimResult.Failure("<red>Tu perfil no está cargado en el sistema.</red>")

            if (!isGift) {
                if (!player.hasPermission(config.permission)) {
                    return@supplyAsync ClaimResult.Failure(messageService.getRawTemplate(KitMessages.NO_PERMISSION).ifEmpty { "<red>No tienes permiso para esto.</red>" })
                }

                val remaining = getRemainingCooldown(player.uniqueId, kitId)
                val hasBypass = player.hasPermission("core.kits.bypass.cooldown") || 
                                player.hasPermission("core.kits.bypass.cooldown.${kitId.lowercase()}")
                if (remaining > 0 && !hasBypass) {
                    val formattedTime = formatTime(remaining)
                    val msg = (messageService.getRawTemplate(KitMessages.COOLDOWN).ifEmpty { "<red>Debes esperar <time> para volver a reclamar este kit.</red>" })
                        .replace("<time>", formattedTime)
                    return@supplyAsync ClaimResult.Failure(msg)
                }

                if (config.cost > 0.0) {
                    val costBD = BigDecimal(config.cost)
                    val currentBal = economyService.getBalance(player.uniqueId, config.currency)
                    if (currentBal < costBD) {
                        return@supplyAsync ClaimResult.Failure(messageService.getRawTemplate(KitMessages.INSUFFICIENT_FUNDS).ifEmpty { "<red>No tienes dinero suficiente para comprar este kit.</red>" })
                    }
                }
            }

            val itemStacks = config.items.mapNotNull { buildItemStack(it) }
            val requiredSlots = calculateRequiredSlots(player, itemStacks)

            val emptySlots = player.inventory.storageContents.count { it == null || it.type == Material.AIR }
            if (emptySlots < requiredSlots) {
                return@supplyAsync ClaimResult.Failure((messageService.getRawTemplate(KitMessages.NO_SPACE).ifEmpty { "<red>No tienes espacio suficiente en el inventario (<required> ranuras libres requeridas).</red>" }).replace("<required>", requiredSlots.toString()))
            }

            if (!isGift && config.cost > 0.0) {
                val costBD = BigDecimal(config.cost)
                val success = economyService.withdrawCacheBehind(player.uniqueId, config.currency, costBD, "KIT_PURCHASE_$kitId").join()
                if (!success) {
                    return@supplyAsync ClaimResult.Failure(messageService.getRawTemplate(KitMessages.PURCHASE_FAILED).ifEmpty { "<red>Hubo un fallo al procesar la compra del kit.</red>" })
                }
            }

            // Actualizar cooldown en caché (isDirty se marcará a true automáticamente)
            val now = System.currentTimeMillis()
            profile.setCooldown(kitId, now)
 
            regionTaskScheduler.runAtLocation(player.location, Runnable {
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
            })
 
            ClaimResult.Success(messageService.getRawTemplate(KitMessages.CLAIMED).ifEmpty { "<green>¡Has reclamado tu kit con éxito!</green>" })
        }, { command -> taskScheduler.runAsync(command) })
    }
}
