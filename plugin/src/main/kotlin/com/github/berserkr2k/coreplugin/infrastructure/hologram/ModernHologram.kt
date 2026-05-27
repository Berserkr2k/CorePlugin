package com.github.berserkr2k.coreplugin.infrastructure.hologram

import org.bukkit.Location
import org.bukkit.entity.TextDisplay
import org.bukkit.entity.Interaction
import org.bukkit.entity.Display
import org.bukkit.plugin.Plugin
import org.bukkit.Bukkit
import net.kyori.adventure.text.minimessage.MiniMessage
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class ModernHologram(
    val id: String,
    val location: Location,
    private val plugin: Plugin
) {
    private val lines = CopyOnWriteArrayList<Display>()
    private var interactionEntity: Interaction? = null
    private val miniMessage = MiniMessage.miniMessage()
    private val lineSpacing = 0.35
    
    var clickCommand: String? = null

    /**
     * Construye y renderiza el holograma en la región correspondiente al bloque tridimensional.
     */
    fun spawn(textLines: List<String>) {
        Bukkit.getRegionScheduler().execute(plugin, location) {
            var currentLoc = location.clone()
            
            for (lineText in textLines) {
                val textDisplay = location.world.spawn(currentLoc, TextDisplay::class.java) { entity ->
                    entity.text(miniMessage.deserialize(lineText))
                    entity.billboard = Display.Billboard.CENTER
                    entity.isShadowed = true
                    entity.isPersistent = true
                }
                lines.add(textDisplay)
                currentLoc = currentLoc.add(0.0, -lineSpacing, 0.0)
            }

            val interactionHeight = (lines.size * lineSpacing).toFloat() + 0.2f
            interactionEntity = location.world.spawn(
                location.clone().add(0.0, -interactionHeight / 2.0, 0.0), 
                Interaction::class.java
            ) { entity ->
                entity.interactionWidth = 1.6f
                entity.interactionHeight = interactionHeight
                entity.isResponsive = true
                entity.isPersistent = true
            }
        }
    }

    /**
     * Elimina físicamente las entidades asociadas al holograma.
     */
    fun delete() {
        Bukkit.getRegionScheduler().execute(plugin, location) {
            lines.forEach { it.remove() }
            lines.clear()
            interactionEntity?.remove()
            interactionEntity = null
        }
    }

    fun isInteractionEntity(uuid: UUID): Boolean {
        return interactionEntity?.uniqueId == uuid
    }
}
