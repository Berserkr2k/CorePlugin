package com.github.berserkr2k.coreplugin.infrastructure.mechanics

import org.bukkit.Bukkit
import org.bukkit.Location
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry
import com.github.berserkr2k.coreplugin.api.core.scheduler.RegionTaskScheduler
import com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler
import org.bukkit.NamespacedKey
import org.bukkit.block.data.type.Stairs
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.entity.EntityDismountEvent
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.CoreMessages
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ChairListener(
    private val plugin: Plugin,
    private val messageService: MessageService,
    private val serviceRegistry: ServiceRegistry
) : Listener {

    private val chairKey = NamespacedKey(plugin, "chair_entity")
    val activeChairs = ConcurrentHashMap.newKeySet<UUID>()
    private val regionTaskScheduler = serviceRegistry.get(RegionTaskScheduler::class.java)
    private val taskScheduler = serviceRegistry.get(TaskScheduler::class.java)

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        val stairsData = block.blockData as? Stairs ?: return
        
        // Evitamos doble interacción por la mano secundaria
        if (event.hand != org.bukkit.inventory.EquipmentSlot.HAND) return
        
        val player = event.player

        // No permitimos sentarse en escaleras al revés (upside-down)
        if (stairsData.half == org.bukkit.block.data.Bisected.Half.TOP) return

        // Comprobaciones de seguridad y estado
        if (player.vehicle != null || player.isSneaking || player.isDead) return
        if (player.gameMode == org.bukkit.GameMode.SPECTATOR) return
        if (player.fireTicks > 0) return

        // Evitar que interactúen a través de paredes o desde muy lejos (reach limit)
        if (player.location.distanceSquared(block.location) > 16.0) return

        // Comprobación de obstrucción física / espacio de aire sobre la silla para prevenir X-ray/clipping
        val up1 = block.getRelative(org.bukkit.block.BlockFace.UP)
        val up2 = up1.getRelative(org.bukkit.block.BlockFace.UP)
        if (up1.type.isSolid || up2.type.isSolid || up1.isLiquid) {
            messageService.send(player, CoreMessages.CHAIR_UNSAFE)
            return
        }

        // Posicionamiento de la silla: Y ajustado a -0.55
        val loc = block.location.clone().add(0.5, -0.55, 0.5)

        // Asignación de orientación (Yaw) según la rotación de la escalera (rotado 180 grados para alinear el cuerpo)
        val yaw = when (stairsData.facing) {
            org.bukkit.block.BlockFace.NORTH -> 0f
            org.bukkit.block.BlockFace.EAST -> 90f
            org.bukkit.block.BlockFace.SOUTH -> 180f
            org.bukkit.block.BlockFace.WEST -> -90f
            else -> 0f
        }
        loc.yaw = yaw

        // Ejecutar de forma segura en el programador de la región de Folia
        regionTaskScheduler.runAtLocation(block.location) {
            // Verificar si el bloque ya está ocupado por una silla activa
            val alreadyOccupied = block.world.getNearbyEntities(loc, 0.5, 0.5, 0.5) { entity ->
                entity is ArmorStand && entity.persistentDataContainer.has(chairKey, PersistentDataType.BOOLEAN)
            }.isNotEmpty()

            if (alreadyOccupied) {
                messageService.send(player, CoreMessages.CHAIR_OCCUPIED)
                return@runAtLocation
            }

            // Spawnear el ArmorStand silla
            val chair = block.world.spawnEntity(loc, EntityType.ARMOR_STAND) as ArmorStand
            chair.isVisible = false
            chair.setGravity(false)
            chair.setArms(false)
            chair.setBasePlate(false)
            chair.isSmall = true
            chair.isMarker = false
            chair.isPersistent = false // Evita guardar la silla en archivos del mundo
            chair.persistentDataContainer.set(chairKey, PersistentDataType.BOOLEAN, true)

            // Registrar y montar
            activeChairs.add(chair.uniqueId)
            chair.addPassenger(player)
        }
    }

    @EventHandler
    fun onEntityDismount(event: EntityDismountEvent) {
        val player = event.entity as? Player ?: return
        val chair = event.dismounted as? ArmorStand ?: return

        if (!chair.persistentDataContainer.has(chairKey, PersistentDataType.BOOLEAN)) return

        val loc = chair.location
        regionTaskScheduler.runAtLocation(loc) {
            activeChairs.remove(chair.uniqueId)
            chair.remove()
            
            // Teletransportar al jugador 1 tick después para que el dismount por defecto de Minecraft no sobrescriba su posición
            // Elevación del exitLoc a 1.2 bloques por encima de la base real de la escalera para asegurar que el jugador aparezca libremente y caiga de pie
            taskScheduler.runSyncLater(Runnable {
                val exitLoc = loc.clone()
                exitLoc.x = Math.floor(loc.x) + 0.5
                exitLoc.y = Math.floor(loc.y - (-0.55)) + 1.2
                exitLoc.z = Math.floor(loc.z) + 0.5
                exitLoc.yaw = player.location.yaw
                exitLoc.pitch = player.location.pitch
                player.teleportAsync(exitLoc)
            }, 1L)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val chair = player.vehicle as? ArmorStand ?: return
        if (chair.persistentDataContainer.has(chairKey, PersistentDataType.BOOLEAN)) {
            regionTaskScheduler.runAtLocation(chair.location) {
                activeChairs.remove(chair.uniqueId)
                chair.remove()
            }
        }
    }

    /**
     * Limpieza completa de todas las sillas activas al deshabilitar el plugin.
     */
    fun shutdown() {
        for (uuid in activeChairs) {
            val entity = Bukkit.getEntity(uuid)
            entity?.remove()
        }
        activeChairs.clear()
    }
}
