package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit

class ArmorStandEditorListener(
    private val plugin: Plugin,
    private val leaderboardService: LeaderboardService,
    private val messagesConfig: MessagesConfig
) : Listener {

    private val editorKey = NamespacedKey(plugin, "stand_editor_tool")
    private val miniMessage = MiniMessage.miniMessage()
    
    companion object {
        // Almacenamiento seguro multihilo para seguir el objetivo editado de cada jugador en Folia
        val playerEditingSessions = ConcurrentHashMap<UUID, UUID>()
        val playerScale = ConcurrentHashMap<UUID, ScaleMode>()
        val copiedSettings = ConcurrentHashMap<UUID, CopiedSettings>()
        val renamingSessions = ConcurrentHashMap<UUID, UUID>()
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEntityEvent) {
        val player = event.player
        val target = event.rightClicked as? ArmorStand ?: return
        val item = player.inventory.itemInMainHand

        if (item.type.isAir) return
        val meta = item.itemMeta ?: return

        // Validación atómica de la Vara de Edición mediante PDC
        if (meta.persistentDataContainer.has(editorKey, PersistentDataType.BOOLEAN)) {
            event.isCancelled = true
            playerEditingSessions[player.uniqueId] = target.uniqueId
            
            // Abrir la GUI nativa del editor
            ArmorStandEditorGui.open(plugin, player, target)
        }
    }

    @EventHandler
    fun onPlayerChat(event: AsyncChatEvent) {
        val player = event.player
        val targetStandUuid = renamingSessions.remove(player.uniqueId) ?: return
        
        event.isCancelled = true
        
        val rawText = PlainTextComponentSerializer.plainText().serialize(event.message())
        val deserialized = LegacyComponentSerializer.legacyAmpersand().deserialize(rawText)
        
        val stand = Bukkit.getEntity(targetStandUuid) as? ArmorStand
        if (stand == null) {
            player.sendMessage(miniMessage.deserialize("<red>El ArmorStand ya no existe.</red>"))
            return
        }
        
        Bukkit.getRegionScheduler().execute(plugin, stand.location) {
            stand.customName(deserialized)
            stand.isCustomNameVisible = true
            player.sendMessage(miniMessage.deserialize("<green>¡Nombre asignado con éxito!</green>"))
        }
    }
}

enum class ScaleMode(val rotationAngle: Double, val moveDistance: Double) {
    COARSE(15.0, 0.5),
    FINE(1.0, 0.05)
}

data class CopiedSettings(
    val headPose: org.bukkit.util.EulerAngle,
    val bodyPose: org.bukkit.util.EulerAngle,
    val leftArmPose: org.bukkit.util.EulerAngle,
    val rightArmPose: org.bukkit.util.EulerAngle,
    val leftLegPose: org.bukkit.util.EulerAngle,
    val rightLegPose: org.bukkit.util.EulerAngle,
    val isSmall: Boolean,
    val hasArms: Boolean,
    val hasBasePlate: Boolean,
    val isVisible: Boolean,
    val hasGravity: Boolean,
    val isInvulnerable: Boolean,
    val equipment: Map<org.bukkit.inventory.EquipmentSlot, org.bukkit.inventory.ItemStack?>? = null
)

