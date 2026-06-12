package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import com.github.berserkr2k.coreplugin.api.framework.menu.MenuService
import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilderFactory
import com.github.berserkr2k.coreplugin.common.ColorUtility
import org.bukkit.NamespacedKey
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.EulerAngle
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler
import com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService
import com.github.berserkr2k.coreplugin.api.core.state.StateContainer
import com.github.berserkr2k.coreplugin.api.core.state.StateContainerType
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import com.github.berserkr2k.coreplugin.api.feature.leaderboard.LeaderboardService
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.CoreMessages
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder

class ArmorStandEditorStateContainer(
    var editingStandUuid: UUID? = null,
    var scaleMode: ScaleMode = ScaleMode.COARSE,
    var copiedSettings: CopiedSettings? = null,
    var renamingStandUuid: UUID? = null,
    var originalHandItem: ItemStack? = null
) : StateContainer

class ArmorStandEditorListener(
    private val leaderboardService: LeaderboardService,
    private val menuService: MenuService,
    private val itemBuilderFactory: ItemBuilderFactory
) : Listener {

    private val registry = org.bukkit.Bukkit.getServicesManager().load(com.github.berserkr2k.coreplugin.api.di.ServiceRegistry::class.java)
        ?: throw IllegalStateException("ServiceRegistry not found in ServicesManager")

    private val plugin = registry.get(Plugin::class.java)
    private val editorKey = NamespacedKey(plugin, "stand_editor_tool")
    private val miniMessage = MiniMessage.miniMessage()
    
    private val regionTaskScheduler = registry.get(RegionTaskScheduler::class.java)
    private val stateService = registry.get(PlayerStateService::class.java)
    private val messageService = registry.get(MessageService::class.java)

    private fun getEditorState(uuid: UUID): ArmorStandEditorStateContainer {
        return stateService.getContainer(uuid, ARMOR_STAND_EDITOR_STATE)
    }
    
    companion object {
        val ARMOR_STAND_EDITOR_STATE = StateContainerType { ArmorStandEditorStateContainer() }
        lateinit var poseToolKey: NamespacedKey
    }

    init {
        poseToolKey = NamespacedKey(plugin, "stand_pose_tool")
    }

    private fun getToolData(item: ItemStack?): Triple<UUID, String, String>? {
        if (item == null || item.type.isAir) return null
        val meta = item.itemMeta ?: return null
        val dataStr = meta.persistentDataContainer.get(poseToolKey, PersistentDataType.STRING) ?: return null
        val parts = dataStr.split(":")
        if (parts.size < 3) return null
        val uuid = try { UUID.fromString(parts[0]) } catch (e: Exception) { return null }
        return Triple(uuid, parts[1], parts[2])
    }

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val target = event.rightClicked as? ArmorStand ?: return
        val item = player.inventory.itemInMainHand

        if (item.type.isAir) return
        val meta = item.itemMeta ?: return

        // 1. Validación atómica de la Vara de Edición mediante PDC
        if (meta.persistentDataContainer.has(editorKey, PersistentDataType.BOOLEAN)) {
            event.isCancelled = true
            val state = getEditorState(player.uniqueId)
            state.editingStandUuid = target.uniqueId
            
            // Abrir la GUI nativa del editor
            ArmorStandEditorGui.open(plugin, player, target)
            return
        }

        // 2. Validación de la herramienta de pose
        val toolData = getToolData(item) ?: return
        event.isCancelled = true

        val (standUuid, part, axis) = toolData

        if (player.isSneaking) {
            cleanupPoseTool(player)
            val stand = Bukkit.getEntity(standUuid) as? ArmorStand
            if (stand != null) {
                ArmorStandEditorGui.open(plugin, player, stand)
            } else {
                messageService.send(player, CoreMessages.LEADERBOARD_ARMORSTAND_NOT_FOUND)
            }
            return
        }

        applyPoseChange(player, standUuid, part, axis, isIncrease = false)
    }

    @EventHandler
    fun onPlayerInteractAtEntity(event: PlayerInteractAtEntityEvent) {
        val player = event.player
        val toolData = getToolData(player.inventory.itemInMainHand) ?: return
        event.isCancelled = true

        val (standUuid, part, axis) = toolData

        if (player.isSneaking) {
            cleanupPoseTool(player)
            val stand = Bukkit.getEntity(standUuid) as? ArmorStand
            if (stand != null) {
                ArmorStandEditorGui.open(plugin, player, stand)
            } else {
                messageService.send(player, CoreMessages.LEADERBOARD_ARMORSTAND_NOT_FOUND)
            }
            return
        }

        applyPoseChange(player, standUuid, part, axis, isIncrease = false)
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val toolData = getToolData(player.inventory.itemInMainHand) ?: return
        event.isCancelled = true

        val (standUuid, part, axis) = toolData

        // Sneak + Right Click to return to menu
        if (player.isSneaking && (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK)) {
            cleanupPoseTool(player)
            val stand = Bukkit.getEntity(standUuid) as? ArmorStand
            if (stand != null) {
                ArmorStandEditorGui.open(plugin, player, stand)
            } else {
                messageService.send(player, CoreMessages.LEADERBOARD_ARMORSTAND_NOT_FOUND)
            }
            return
        }

        val isIncrease = when (event.action) {
            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> true
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> false
            else -> return
        }

        applyPoseChange(player, standUuid, part, axis, isIncrease)
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        val toolData = getToolData(player.inventory.itemInMainHand) ?: return
        event.isCancelled = true

        val (standUuid, part, axis) = toolData
        applyPoseChange(player, standUuid, part, axis, isIncrease = true)
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        val item = event.itemDrop.itemStack
        if (item.type.isAir) return
        val meta = item.itemMeta ?: return
        if (meta.persistentDataContainer.has(poseToolKey, PersistentDataType.STRING)) {
            event.isCancelled = true
            
            // Eliminar de forma segura
            val dataStr = meta.persistentDataContainer.get(poseToolKey, PersistentDataType.STRING) ?: ""
            val parts = dataStr.split(":")
            val standUuid = if (parts.isNotEmpty()) try { UUID.fromString(parts[0]) } catch (e: Exception) { null } else null
            
            cleanupPoseTool(player)
            
            val stand = if (standUuid != null) Bukkit.getEntity(standUuid) as? ArmorStand else null
            if (stand != null) {
                ArmorStandEditorGui.open(plugin, player, stand)
            } else {
                messageService.send(player, CoreMessages.LEADERBOARD_ARMORSTAND_NOT_FOUND)
            }
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val item = event.currentItem ?: return
        if (item.type.isAir) return
        val meta = item.itemMeta ?: return
        if (meta.persistentDataContainer.has(poseToolKey, PersistentDataType.STRING)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        cleanupPoseTool(event.player)
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        cleanupPoseTool(event.entity)
    }

    private fun cleanupPoseTool(player: Player) {
        val state = getEditorState(player.uniqueId)
        val backup = state.originalHandItem
        state.originalHandItem = null
        val mainHand = player.inventory.itemInMainHand
        if (mainHand.type != Material.AIR) {
            val meta = mainHand.itemMeta
            if (meta != null && meta.persistentDataContainer.has(poseToolKey, PersistentDataType.STRING)) {
                if (backup != null && backup.type != Material.AIR) {
                    player.inventory.setItemInMainHand(backup)
                } else {
                    player.inventory.setItemInMainHand(ItemStack(Material.AIR))
                }
            }
        }
    }

    private fun applyPoseChange(player: Player, standUuid: UUID, part: String, axis: String, isIncrease: Boolean) {
        val stand = Bukkit.getEntity(standUuid) as? ArmorStand ?: return
        val state = getEditorState(player.uniqueId)
        val scale = state.scaleMode
        val angleRad = Math.toRadians(scale.rotationAngle)

        // Mutar la postura de forma segura en la región del ArmorStand
        regionTaskScheduler.runAtLocation(stand.location) {
            val currentPose = getPartPose(stand, part)
            val delta = if (isIncrease) angleRad else -angleRad
            val newPose = when (axis.uppercase()) {
                "X" -> EulerAngle(currentPose.x + delta, currentPose.y, currentPose.z)
                "Y" -> EulerAngle(currentPose.x, currentPose.y + delta, currentPose.z)
                "Z" -> EulerAngle(currentPose.x, currentPose.y, currentPose.z + delta)
                else -> currentPose
            }
            setPartPose(stand, part, newPose)

            val currentAngleRad = when (axis.uppercase()) {
                "X" -> newPose.x
                "Y" -> newPose.y
                "Z" -> newPose.z
                else -> 0.0
            }
            val currentAngleDeg = Math.toDegrees(currentAngleRad)
            val formattedAngle = String.format(java.util.Locale.US, "%.1f", currentAngleDeg)
            
            val partsName = when(part.uppercase()) {
                "HEAD" -> "Cabeza"
                "BODY" -> "Cuerpo"
                "LEFT_ARM" -> "Brazo Izquierdo"
                "RIGHT_ARM" -> "Brazo Derecho"
                "LEFT_LEG" -> "Pierna Izquierda"
                "RIGHT_LEG" -> "Pierna Derecha"
                else -> part
            }
            val scaleName = if (scale == ScaleMode.COARSE) "GRUESO" else "FINO"
            
            val actionbarTemplate = messageService.getRawTemplate(CoreMessages.LEADERBOARD_POSE_ACTIONBAR).ifEmpty {
                "<gold><bold><part> (<axis>): <angle>°</bold></gold> <gray>(Modo: <mode>)</gray>"
            }
            player.sendActionBar(miniMessage.deserialize(
                actionbarTemplate,
                Placeholder.parsed("part", partsName),
                Placeholder.parsed("axis", axis),
                Placeholder.parsed("angle", formattedAngle),
                Placeholder.parsed("mode", scaleName)
            ))
            
            // Sonido pling
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f)
        }
    }

    private fun getPartPose(stand: ArmorStand, part: String): EulerAngle {
        return when (part.uppercase()) {
            "HEAD" -> stand.headPose
            "BODY" -> stand.bodyPose
            "LEFT_ARM" -> stand.leftArmPose
            "RIGHT_ARM" -> stand.rightArmPose
            "LEFT_LEG" -> stand.leftLegPose
            "RIGHT_LEG" -> stand.rightLegPose
            else -> EulerAngle.ZERO
        }
    }

    private fun setPartPose(stand: ArmorStand, part: String, pose: EulerAngle) {
        when (part.uppercase()) {
            "HEAD" -> stand.headPose = pose
            "BODY" -> stand.bodyPose = pose
            "LEFT_ARM" -> stand.leftArmPose = pose
            "RIGHT_ARM" -> stand.rightArmPose = pose
            "LEFT_LEG" -> stand.leftLegPose = pose
            "RIGHT_LEG" -> stand.rightLegPose = pose
        }
    }

    @EventHandler
    fun onPlayerChat(event: AsyncChatEvent) {
        val player = event.player
        val state = getEditorState(player.uniqueId)
        val targetStandUuid = state.renamingStandUuid
        state.renamingStandUuid = null
        if (targetStandUuid == null) return
        
        event.isCancelled = true
        
        val rawText = PlainTextComponentSerializer.plainText().serialize(event.message())
        val deserialized = LegacyComponentSerializer.legacyAmpersand().deserialize(rawText)
        
        val stand = Bukkit.getEntity(targetStandUuid) as? ArmorStand
        if (stand == null) {
            messageService.send(player, CoreMessages.LEADERBOARD_ARMORSTAND_NOT_FOUND)
            return
        }
        
        regionTaskScheduler.runAtLocation(stand.location) {
            stand.customName(deserialized)
            stand.isCustomNameVisible = true
            messageService.send(player, CoreMessages.LEADERBOARD_NAME_ASSIGNED)
        }
    }
}

enum class ScaleMode(val rotationAngle: Double, val moveDistance: Double) {
    COARSE(15.0, 0.5),
    FINE(1.0, 0.05)
}

data class CopiedSettings(
    val headPose: EulerAngle,
    val bodyPose: EulerAngle,
    val leftArmPose: EulerAngle,
    val rightArmPose: EulerAngle,
    val leftLegPose: EulerAngle,
    val rightLegPose: EulerAngle,
    val isSmall: Boolean,
    val hasArms: Boolean,
    val hasBasePlate: Boolean,
    val isVisible: Boolean,
    val hasGravity: Boolean,
    val isInvulnerable: Boolean,
    val equipment: Map<org.bukkit.inventory.EquipmentSlot, ItemStack?>? = null
)


