package com.github.berserkr2k.coreplugin.infrastructure.hologram

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.wrappers.WrappedDataWatcher
import com.comphenix.protocol.wrappers.WrappedChatComponent
import com.comphenix.protocol.wrappers.WrappedDataValue
import com.github.berserkr2k.coreplugin.common.LegacyPlaceholderBridge
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.Bukkit
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.util.UUID
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class ModernHologram(
    val id: String,
    var location: Location,
    private val plugin: Plugin,
    private val placeholderBridge: LegacyPlaceholderBridge
) {
    var textLines = CopyOnWriteArrayList<String>()
    var clickCommand: String? = null
    
    val viewers = ConcurrentHashMap.newKeySet<UUID>()
    
    // Virtual entity IDs and UUIDs
    val textEntityIds = CopyOnWriteArrayList<Int>()
    val textUuids = CopyOnWriteArrayList<UUID>()
    var interactionEntityId: Int = -1
        private set
    var interactionUuid: UUID? = null
        private set
    // Parámetros dinámicos y configurables
    var lineSpacing: Double = 0.28
    var backgroundColor: Int = 1073741824
    var renderDistance: Int = 48

    companion object {
        private val idCounter = java.util.concurrent.atomic.AtomicInteger(200000000)
        fun nextEntityId(): Int = idCounter.incrementAndGet()
    }

    private fun parseText(text: String, player: Player): net.kyori.adventure.text.Component {
        return placeholderBridge.parseLegacyStringSecurely(player, text)
    }

    /**
     * Define las líneas de texto del holograma y genera los IDs de entidad virtuales.
     */
    fun setup(textLines: List<String>) {
        this.textLines.clear()
        this.textLines.addAll(textLines)
        
        // Generate new entity IDs and UUIDs for text lines
        textEntityIds.clear()
        textUuids.clear()
        for (i in textLines.indices) {
            textEntityIds.add(nextEntityId())
            textUuids.add(UUID.randomUUID())
        }
        
        // Generate ID for interaction entity
        interactionEntityId = nextEntityId()
        interactionUuid = UUID.randomUUID()
    }

    /**
     * Muestra el holograma en paquetes para un jugador específico.
     */
    fun showTo(player: Player) {
        if (!viewers.add(player.uniqueId)) return
        
        val protocolManager = ProtocolLibrary.getProtocolManager()
        var currentLoc = location.clone()
        
        // Spawn each TextDisplay line
        for (i in textLines.indices) {
            val entityId = textEntityIds[i]
            val uuid = textUuids[i]
            val lineText = textLines[i]
            
            // 1. Send SPAWN_ENTITY packet
            val spawnPacket = PacketContainer(PacketType.Play.Server.SPAWN_ENTITY)
            spawnPacket.getIntegers().write(0, entityId)
            spawnPacket.getUUIDs().write(0, uuid)
            spawnPacket.getEntityTypeModifier().write(0, EntityType.TEXT_DISPLAY)
            
            val doubles = spawnPacket.getDoubles()
            doubles.write(0, currentLoc.x)
            doubles.write(1, currentLoc.y)
            doubles.write(2, currentLoc.z)
            
            // Yaw/Pitch: Center billboard
            val bytes = spawnPacket.getBytes()
            bytes.write(0, 0.toByte())
            bytes.write(1, 0.toByte())
                
            protocolManager.sendServerPacket(player, spawnPacket)
            
            // 2. Send ENTITY_METADATA packet with WrappedDataValue list (1.19.3+ compatible)
            val metadataPacket = PacketContainer(PacketType.Play.Server.ENTITY_METADATA)
            metadataPacket.getIntegers().write(0, entityId)
            
            val textComp = parseText(lineText, player)
            val jsonText = GsonComponentSerializer.gson().serialize(textComp)
            val chatComp = WrappedChatComponent.fromJson(jsonText)
            
            // Serializers de ProtocolLib
            val textSerializer = WrappedDataWatcher.Registry.getChatComponentSerializer(false)
            val byteSerializer = WrappedDataWatcher.Registry.get(java.lang.Byte::class.java)
            val intSerializer = WrappedDataWatcher.Registry.get(java.lang.Integer::class.java)
            
            val dataValues = listOf(
                WrappedDataValue(23, textSerializer, chatComp.handle), // Text (Non-optional Component)
                WrappedDataValue(15, byteSerializer, 3.toByte()),       // Billboard: CENTER (3)
                WrappedDataValue(25, intSerializer, backgroundColor),   // Background Color (Configurable)
                WrappedDataValue(24, intSerializer, 3000),              // Line Width: 3000 (Prevents wrapping)
                WrappedDataValue(27, byteSerializer, 1.toByte())        // Options: 1 (Has Shadow, disables default background)
            )
            
            metadataPacket.getDataValueCollectionModifier().write(0, dataValues)
            protocolManager.sendServerPacket(player, metadataPacket)
            
            currentLoc = currentLoc.add(0.0, -lineSpacing, 0.0)
        }
        
        // Spawn interaction entity for clicks
        val interactionHeight = (textLines.size * lineSpacing).toFloat() + 0.2f
        val interactLoc = location.clone().add(0.0, -interactionHeight / 2.0, 0.0)
        
        val spawnInteract = PacketContainer(PacketType.Play.Server.SPAWN_ENTITY)
        spawnInteract.getIntegers().write(0, interactionEntityId)
        spawnInteract.getUUIDs().write(0, interactionUuid)
        spawnInteract.getEntityTypeModifier().write(0, EntityType.INTERACTION)
        
        val doublesInteract = spawnInteract.getDoubles()
        doublesInteract.write(0, interactLoc.x)
        doublesInteract.write(1, interactLoc.y)
        doublesInteract.write(2, interactLoc.z)
            
        protocolManager.sendServerPacket(player, spawnInteract)
        
        // Send interaction metadata
        val metadataInteract = PacketContainer(PacketType.Play.Server.ENTITY_METADATA)
        metadataInteract.getIntegers().write(0, interactionEntityId)
        
        val floatSerializer = WrappedDataWatcher.Registry.get(java.lang.Float::class.java)
        val boolSerializer = WrappedDataWatcher.Registry.get(java.lang.Boolean::class.java)
        
        val interactValues = listOf(
            WrappedDataValue(8, floatSerializer, 1.6f),                  // Width
            WrappedDataValue(9, floatSerializer, interactionHeight),     // Height
            WrappedDataValue(10, boolSerializer, true)                   // Responsive
        )
        
        metadataInteract.getDataValueCollectionModifier().write(0, interactValues)
        protocolManager.sendServerPacket(player, metadataInteract)
    }

    /**
     * Oculta el holograma en paquetes para un jugador específico.
     */
    fun hideFrom(player: Player) {
        if (!viewers.remove(player.uniqueId)) return
        
        val protocolManager = ProtocolLibrary.getProtocolManager()
        val entityIdsToDestroy = mutableListOf<Int>()
        entityIdsToDestroy.addAll(textEntityIds)
        if (interactionEntityId != -1) {
            entityIdsToDestroy.add(interactionEntityId)
        }
        
        val destroyPacket = PacketContainer(PacketType.Play.Server.ENTITY_DESTROY)
        destroyPacket.getIntLists().write(0, entityIdsToDestroy)
        try {
            protocolManager.sendServerPacket(player, destroyPacket)
        } catch (e: Exception) {
            // Player might have disconnected
        }
    }

    /**
     * Refresca las líneas del holograma para todos los espectadores activos (actualizando Placeholders sin parpadeo).
     */
    fun refreshForViewers() {
        val protocolManager = ProtocolLibrary.getProtocolManager()
        val textSerializer = WrappedDataWatcher.Registry.getChatComponentSerializer(false)
        val byteSerializer = WrappedDataWatcher.Registry.get(java.lang.Byte::class.java)
        val intSerializer = WrappedDataWatcher.Registry.get(java.lang.Integer::class.java)

        for (viewerUuid in viewers) {
            val player = Bukkit.getPlayer(viewerUuid) ?: continue
            if (!player.isOnline) continue

            for (i in textLines.indices) {
                if (i >= textEntityIds.size) break
                val entityId = textEntityIds[i]
                val lineText = textLines[i]

                val metadataPacket = PacketContainer(PacketType.Play.Server.ENTITY_METADATA)
                metadataPacket.getIntegers().write(0, entityId)

                val textComp = parseText(lineText, player)
                val jsonText = GsonComponentSerializer.gson().serialize(textComp)
                val chatComp = WrappedChatComponent.fromJson(jsonText)

                val dataValues = listOf(
                    WrappedDataValue(23, textSerializer, chatComp.handle), // Text (Non-optional Component)
                    WrappedDataValue(15, byteSerializer, 3.toByte()),       // Billboard: CENTER (3)
                    WrappedDataValue(25, intSerializer, backgroundColor),   // Background Color (Configurable)
                    WrappedDataValue(24, intSerializer, 3000),              // Line Width: 3000 (Prevents wrapping)
                    WrappedDataValue(27, byteSerializer, 1.toByte())        // Options: 1 (Has Shadow, disables default background)
                )

                metadataPacket.getDataValueCollectionModifier().write(0, dataValues)
                try {
                    protocolManager.sendServerPacket(player, metadataPacket)
                } catch (e: Exception) {
                    // Ignore client disconnection
                }
            }
        }
    }

    /**
     * Actualiza el texto del holograma de forma dinámica.
     */
    fun updateText(newLines: List<String>) {
        // Destroy existing for current viewers
        val activeViewers = viewers.mapNotNull { Bukkit.getPlayer(it) }
        activeViewers.forEach { hideFrom(it) }
        
        setup(newLines)
        
        // Re-spawn for active viewers
        activeViewers.forEach { showTo(it) }
    }

    /**
     * Desintegra el holograma destruyendo los paquetes en todos los clientes.
     */
    fun delete(forceSync: Boolean = false) {
        val activeViewers = viewers.mapNotNull { Bukkit.getPlayer(it) }
        activeViewers.forEach { hideFrom(it) }
        viewers.clear()
        textEntityIds.clear()
        textUuids.clear()
        interactionEntityId = -1
        interactionUuid = null
    }

    fun isInteractionEntity(entityId: Int): Boolean {
        return interactionEntityId == entityId
    }
}
