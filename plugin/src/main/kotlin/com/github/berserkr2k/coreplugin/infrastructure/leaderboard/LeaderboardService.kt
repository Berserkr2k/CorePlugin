package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.TextDisplay
import org.bukkit.entity.Display
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin
import org.bukkit.util.EulerAngle
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.profile.PlayerProfile
import net.kyori.adventure.text.minimessage.MiniMessage
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture

class LeaderboardService(
    private val plugin: Plugin,
    private val configManager: com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager,
    private val messagesConfig: MessagesConfig
) {
    private val leaderboardKey = NamespacedKey(plugin, "leaderboard_id")
    private val miniMessage = MiniMessage.miniMessage()
    private val activeArmorStands = ConcurrentHashMap<String, UUID>()

    /**
     * Hace aparecer una armadura (ArmorStand) de clasificación en una coordenada específica,
     * añadiendo una entidad TextDisplay de acompañamiento (como pasajero).
     */
    fun spawnLeaderboardEntity(location: Location, leaderboardId: String) {
        Bukkit.getRegionScheduler().execute(plugin, location) {
            val stand = location.world.spawnEntity(location, EntityType.ARMOR_STAND) as ArmorStand
            stand.setArms(true)
            stand.setBasePlate(true)
            stand.setGravity(false)
            stand.isCustomNameVisible = false
            stand.persistentDataContainer.set(leaderboardKey, PersistentDataType.STRING, leaderboardId)

            stand.rightArmPose = EulerAngle(Math.toRadians(-15.0), 0.0, Math.toRadians(10.0))
            stand.leftArmPose = EulerAngle(Math.toRadians(-15.0), 0.0, Math.toRadians(-10.0))

            val display = location.world.spawnEntity(location.clone().add(0.0, 2.1, 0.0), EntityType.TEXT_DISPLAY) as TextDisplay
            display.setGravity(false)
            display.billboard = Display.Billboard.CENTER
            display.text(miniMessage.deserialize(messagesConfig.leaderboards["loading"] ?: "<gold>Cargando...</gold>"))

            stand.addPassenger(display)
            activeArmorStands[leaderboardId] = stand.uniqueId
        }
    }

    /**
     * Actualiza asíncronamente el podio físico: descarga la skin del jugador en primer lugar
     * y formatea el holograma de clasificación.
     */
    fun refreshLeaderboard(leaderboardId: String, topData: List<Map.Entry<String, Double>>): CompletableFuture<Void> {
        if (topData.isEmpty()) return CompletableFuture.completedFuture(null)

        val firstPlace = topData.first()
        val topPlayerName = firstPlace.key

        return CompletableFuture.supplyAsync({
            val profile = Bukkit.createProfile(topPlayerName)
            profile.complete()
            profile
        }).thenAcceptAsync({ profile ->
            
            val textBuilder = StringBuilder()
            val header = (messagesConfig.leaderboards["header"] ?: "").replace("<top_id>", leaderboardId.uppercase())
            textBuilder.append(header)

            for (i in topData.indices) {
                val entry = topData[i]
                val row = (messagesConfig.leaderboards["row-format"] ?: "")
                   .replace("<pos>", (i + 1).toString())
                   .replace("<player>", entry.key)
                   .replace("<balance>", String.format("%.2f", entry.value))
                textBuilder.append(row).append("\n")
            }
            val textComponent = miniMessage.deserialize(textBuilder.toString())

            val uuid = activeArmorStands[leaderboardId] ?: return@thenAcceptAsync
            val stand = Bukkit.getEntity(uuid) as? ArmorStand ?: return@thenAcceptAsync

            Bukkit.getRegionScheduler().execute(plugin, stand.location) {
                val skull = ItemStack(Material.PLAYER_HEAD)
                val skullMeta = skull.itemMeta as SkullMeta
                skullMeta.playerProfile = profile
                skull.itemMeta = skullMeta

                stand.setItem(EquipmentSlot.HEAD, skull)
                stand.setItem(EquipmentSlot.HAND, ItemStack(Material.GOLDEN_SWORD))

                for (passenger in stand.passengers) {
                    if (passenger is TextDisplay) {
                        passenger.text(textComponent)
                    }
                }
            }
        })
    }

    fun shutdown() {
        activeArmorStands.clear()
    }
}
