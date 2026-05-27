package com.github.berserkr2k.coreplugin.chat

import com.github.berserkr2k.coreplugin.CorePlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PrivateMessageManager(private val plugin: CorePlugin) {

    // Caché súper rápida: Guarda [UUID del que envía] -> [UUID del que recibe]
    private val replyCache = ConcurrentHashMap<UUID, UUID>()

    // Lista de administradores que están espiando el chat
    private val spyPlayers = ConcurrentHashMap.newKeySet<UUID>()

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    /**
     * Procesa y envía un mensaje privado con interactividad.
     */
    fun sendMessage(sender: Player, target: Player, message: String) {
        // 1. Actualizamos el caché de ambos para que puedan hacerse /reply mutuamente
        replyCache[sender.uniqueId] = target.uniqueId
        replyCache[target.uniqueId] = sender.uniqueId

        // 2. Quitamos colores si el jugador no es VIP
        val parsedMessage = if (sender.hasPermission("coreplugin.chat.color")) message else message.replace("&", "")
        val msgComp: Component = serializer.deserialize(parsedMessage)

        // 3. Formato para el que ENVÍA [Yo -> Target]
        val senderFormat = serializer.deserialize("&d[&dYo &8-> &d${target.name}&d] &f")
            .hoverEvent(HoverEvent.showText(serializer.deserialize("&eClic para enviar otro mensaje a ${target.name}")))
            .clickEvent(ClickEvent.suggestCommand("/msg ${target.name} "))
            .append(msgComp)

        // 4. Formato para el que RECIBE [Sender -> Yo]
        val targetFormat = serializer.deserialize("&d[&d${sender.name} &8-> &dYo&d] &f")
            .hoverEvent(HoverEvent.showText(serializer.deserialize("&eClic para responder a ${sender.name}")))
            .clickEvent(ClickEvent.suggestCommand("/msg ${sender.name} "))
            .append(msgComp)

        // 5. Formato para los ESPIAS [Spy] Sender -> Target
        val spyFormat = serializer.deserialize("&8[&cSpy&8] &7${sender.name} &8-> &7${target.name}: &f")
            .append(msgComp)

        // 6. Enviamos los mensajes usando nuestra API Adventure
        val audiences = plugin.adventure
        audiences.player(sender).sendMessage(senderFormat)
        audiences.player(target).sendMessage(targetFormat)

        // 7. Repartimos a los espías
        for (spyUuid in spyPlayers) {
            // Evitamos que el espía vea su propio mensaje duplicado si está hablando con alguien
            if (spyUuid != sender.uniqueId && spyUuid != target.uniqueId) {
                val spyPlayer = Bukkit.getPlayer(spyUuid)
                if (spyPlayer != null && spyPlayer.isOnline) {
                    audiences.player(spyPlayer).sendMessage(spyFormat)
                } else {
                    spyPlayers.remove(spyUuid) // Limpiamos la memoria si el espía se desconectó
                }
            }
        }
    }

    /** Obtiene el jugador al que se le debe responder */
    fun getReplyTarget(sender: Player): Player? {
        val targetUuid = replyCache[sender.uniqueId] ?: return null
        return Bukkit.getPlayer(targetUuid)
    }

    /** Activa o desactiva el modo espía. Retorna el nuevo estado. */
    fun toggleSpy(player: Player): Boolean {
        return if (spyPlayers.contains(player.uniqueId)) {
            spyPlayers.remove(player.uniqueId)
            false
        } else {
            spyPlayers.add(player.uniqueId)
            true
        }
    }
}