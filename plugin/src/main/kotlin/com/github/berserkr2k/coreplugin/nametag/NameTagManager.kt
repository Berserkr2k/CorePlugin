package com.github.berserkr2k.coreplugin.nametag

import com.github.berserkr2k.coreplugin.CorePlugin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class NameTagManager(private val plugin: CorePlugin) : Listener {

    private val groups = mutableListOf<NameTagGroup>()

    init {
        loadGroups()
        // Registramos los eventos al instanciar el manager
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    /**
     * Lee el YAML y carga los grupos en memoria.
     */
    fun loadGroups() {
        groups.clear()
        val config = plugin.nametagsConfig.get()
        val section = config.getConfigurationSection("Groups") ?: return

        for (key in section.getKeys(false)) {
            val path = "Groups.$key"
            groups.add(
                NameTagGroup(
                    id = key,
                    permission = config.getString("$path.Permission") ?: "",
                    prefix = config.getString("$path.Prefix") ?: "",
                    suffix = config.getString("$path.Suffix") ?: "",
                    priority = config.getInt("$path.SortPriority", 99),
                    nameColor = config.getString("$path.NameColor") ?: "WHITE" // <-- AÑADIDO
                )
            )
        }

        // ORDEN DE JERARQUÍA: Ordenamos la lista para que el 1 (Owner) esté primero,
        // 2 (Admin) segundo, etc.
        groups.sortBy { it.priority }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        applyTag(event.player)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        // Limpiamos su equipo al salir
        plugin.nameTagAdapter.remove(event.player)
    }

    /**
     * Busca el rango más alto del jugador y aplica el NameTag.
     * Ahora es pública para que el Interceptor pueda recargar los NameTags.
     */
    fun applyTag(player: Player) { // <-- ¡Asegúrate de que NO diga 'private' aquí!
        // Al estar la lista ordenada, el primer permiso que coincida será su rango más alto
        val group = groups.firstOrNull { player.hasPermission(it.permission) }
            ?: groups.firstOrNull { it.id.equals("Default", true) }
            ?: return

        // Delegamos el trabajo sucio al adaptador de la versión correcta
        plugin.nameTagAdapter.update(
            player = player,
            groupName = group.id,
            priority = group.priority,
            prefix = group.prefix,
            suffix = group.suffix,
            nameColor = group.nameColor
        )
    }
}