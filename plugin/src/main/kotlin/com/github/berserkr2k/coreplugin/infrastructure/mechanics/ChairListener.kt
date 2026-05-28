package com.github.berserkr2k.coreplugin.infrastructure.mechanics

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.block.data.type.Stairs
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ChairListener(private val plugin: Plugin) : Listener {

    private val chairKey = NamespacedKey(plugin, "chair_entity")
    val activeChairs = ConcurrentHashMap.newKeySet<UUID>()

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        val stairsData = block.blockData as? Stairs ?: return
        
        // Evitamos doble interacción por la mano secundaria
        if (event.hand != org.bukkit.inventory.EquipmentSlot.HAND) return
        
        val player = event.player

        // No permitimos sentarse si ya está en un vehículo o si está agachado
        if (player.vehicle != null) return
        if (player.isSneaking) return

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
        Bukkit.getRegionScheduler().execute(plugin, block.location) {
            // Verificar si el bloque ya está ocupado por una silla activa
            val alreadyOccupied = block.world.getNearbyEntities(loc, 0.5, 0.5, 0.5) { entity ->
                entity is ArmorStand && entity.persistentDataContainer.has(chairKey, PersistentDataType.BOOLEAN)
            }.isNotEmpty()

            if (alreadyOccupied) {
                player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>¡Esta silla ya está ocupada!</red>"))
                return@execute
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
        // Ejecutar de forma Folia-safe en la región del ArmorStand
        Bukkit.getRegionScheduler().execute(plugin, loc) {
            activeChairs.remove(chair.uniqueId)
            chair.remove()
            
            // Teletransportar al jugador 1 tick después para que el dismount por defecto de Minecraft no sobrescriba su posición
            // Elevación del exitLoc a 1.2 bloques por encima de la base real de la escalera para asegurar que el jugador aparezca libremente y caiga de pie
            Bukkit.getRegionScheduler().runDelayed(plugin, loc, { _ ->
                val exitLoc = loc.clone()
                exitLoc.x = Math.floor(loc.x) + 0.5
                exitLoc.y = Math.floor(loc.y - (-0.55)) + 1.2
                exitLoc.z = Math.floor(loc.z) + 0.5
                exitLoc.yaw = player.location.yaw
                exitLoc.pitch = player.location.pitch
                player.teleport(exitLoc)
            }, 1L)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val chair = player.vehicle as? ArmorStand ?: return
        if (chair.persistentDataContainer.has(chairKey, PersistentDataType.BOOLEAN)) {
            Bukkit.getRegionScheduler().execute(plugin, chair.location) {
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
