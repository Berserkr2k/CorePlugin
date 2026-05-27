package com.github.berserkr2k.coreplugin.chat

import com.github.berserkr2k.coreplugin.CorePlugin
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ChatManager(private val plugin: CorePlugin) : Listener {

    private val dbNameColorCache = ConcurrentHashMap<UUID, String>()
    private val formats = mutableListOf<ChatFormat>()
    private val legacySerializer = LegacyComponentSerializer.legacyAmpersand()

    init {
        loadFormats()
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    // ---------------------------------------------------------
    // 1. CARGA DE FORMATOS DESDE CONFIG.YML
    // ---------------------------------------------------------
    fun loadFormats() {
        formats.clear()
        val config = plugin.mainConfig.get()
        val section = config.getConfigurationSection("formats") ?: return

        for (key in section.getKeys(false)) {
            val path = "formats.$key"
            formats.add(
                ChatFormat(
                    id = key,
                    priority = config.getInt("$path.priority", 99),
                    prefix = config.getString("$path.prefix") ?: "",
                    nameColor = config.getString("$path.name_color") ?: "",
                    name = config.getString("$path.name") ?: "%player_name%",
                    suffix = config.getString("$path.suffix") ?: "",
                    chatColor = config.getString("$path.chat_color") ?: "&f",
                    prefixTooltip = config.getStringList("$path.prefix_tooltip"),
                    nameTooltip = config.getStringList("$path.name_tooltip"),
                    suffixTooltip = config.getStringList("$path.suffix_tooltip"),
                    prefixClickCmd = config.getString("$path.prefix_click_command") ?: "",
                    nameClickCmd = config.getString("$path.name_click_command") ?: "",
                    suffixClickCmd = config.getString("$path.suffix_click_command") ?: ""
                )
            )
        }
        // Ordenamos: Menor número = Mayor prioridad (Ej: 1 gana sobre 100)
        formats.sortBy { it.priority }
        plugin.logger.info("¡Se han cargado ${formats.size} formatos de chat tipo DeluxeChat!")
    }

    // ---------------------------------------------------------
    // 2. CACHÉ DE LA BASE DE DATOS (Color del Nombre)
    // ---------------------------------------------------------
    fun loadPlayerColor(player: Player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            var color: String? = null
            try {
                plugin.databaseManager.getConnection().use { conn ->
                    val query = "SELECT chat_color FROM ${plugin.databaseManager.tablePrefix}player_stats WHERE uuid = ?"
                    conn.prepareStatement(query).use { ps ->
                        ps.setString(1, player.uniqueId.toString())
                        val rs = ps.executeQuery()
                        if (rs.next()) {
                            color = rs.getString("chat_color")
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }

            if (color != null && color != "<white>") {
                dbNameColorCache[player.uniqueId] = color!!
            }
        })
    }

    fun savePlayerColor(uuid: UUID, color: String) {
        dbNameColorCache[uuid] = color
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                plugin.databaseManager.getConnection().use { conn ->
                    val query = "UPDATE ${plugin.databaseManager.tablePrefix}player_stats SET chat_color = ? WHERE uuid = ?"
                    conn.prepareStatement(query).use { ps ->
                        ps.setString(1, color)
                        ps.setString(2, uuid.toString())
                        ps.executeUpdate()
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        })
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) = loadPlayerColor(event.player)

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) { dbNameColorCache.remove(event.player.uniqueId) }

    // ---------------------------------------------------------
    // 3. EL MOTOR DE CHAT INTERACTIVO (Reemplazo de DeluxeChat)
    // ---------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onChat(event: AsyncPlayerChatEvent) {
        // Cancelamos el evento de Bukkit. Nosotros tomamos el control absoluto.
        event.isCancelled = true
        val player = event.player

        // LÓGICA DE PRIORIDADES:
        // Buscamos el formato de mayor prioridad que el jugador tenga permiso para usar.
        // El permiso a otorgar en LuckPerms será: coreplugin.format.<id> (ej. coreplugin.format.vip)
        val defaultFormat = formats.firstOrNull { it.id.equals("default", true) }
        val format = formats.firstOrNull {
            !it.id.equals("default", true) && player.hasPermission("coreplugin.format.${it.id}")
        } ?: defaultFormat ?: return

        // Función interna (Helper) para construir cada pieza con su Hover y Click independientes
        // Helper mejorado para construir cada pieza
        fun buildPart(rawText: String, tooltip: List<String>, clickCmd: String, customColor: String = ""): Component {
            if (rawText.isBlank() && customColor.isBlank()) return Component.empty()

            val parsedText = PlaceholderAPI.setPlaceholders(player, customColor + rawText)
            var comp: Component = legacySerializer.deserialize(parsedText)

            // Aplicar menú emergente (Hover)
            val validTooltips = tooltip.filter { it.isNotBlank() }
            if (validTooltips.isNotEmpty()) {
                // Procesamos variables y unimos con el salto de línea nativo \n
                val parsedTooltip = validTooltips.joinToString("\n") {
                    PlaceholderAPI.setPlaceholders(player, "&r$it")
                }
                comp = comp.hoverEvent(HoverEvent.showText(legacySerializer.deserialize(parsedTooltip)))
            }

            // Aplicar clic (Click)
            if (clickCmd.isNotBlank()) {
                val parsedCmd = PlaceholderAPI.setPlaceholders(player, clickCmd.trim())
                comp = comp.clickEvent(ClickEvent.suggestCommand(parsedCmd))
            }
            return comp
        }

        // 1. Construir Prefijo
        val prefixComp = buildPart(format.prefix, format.prefixTooltip, format.prefixClickCmd)

        // 2. Construir Nombre (Inyectamos el color de SQLite si el jugador eligió uno en el menú /color)
        val dbColor = dbNameColorCache[player.uniqueId] ?: format.nameColor
        val nameComp = buildPart(format.name, format.nameTooltip, format.nameClickCmd, dbColor)

        // 3. Construir Sufijo
        val suffixComp = buildPart(format.suffix, format.suffixTooltip, format.suffixClickCmd)

        // 4. Construir Mensaje
        var rawMessage = event.message
        if (!player.hasPermission("coreplugin.chat.color")) {
            rawMessage = rawMessage.replace("&", "") // Quitamos colores si no tiene el permiso VIP
        }
        val messageComp: Component = legacySerializer.deserialize(format.chatColor + rawMessage)

        // Ensamblaje Final
        val finalChat = Component.empty()
            .append(prefixComp)
            .append(nameComp)
            .append(suffixComp)
            .append(messageComp)

        // Enviar vía Adventure
        val audiences = plugin.adventure
        audiences.all().sendMessage(finalChat)
        audiences.console().sendMessage(finalChat)
    }

    fun getPlayerColor(uuid: UUID): String {
        return dbNameColorCache[uuid] ?: "&7" // Devuelve el color, o gris por defecto
    }
}