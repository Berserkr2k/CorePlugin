package com.github.berserkr2k.coreplugin.common.gui

import com.github.berserkr2k.coreplugin.common.ColorUtility
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.util.concurrent.ConcurrentHashMap

@ConfigSerializable
data class FillerConfig(
    val enabled: Boolean = true,
    val item: ItemConfig = ItemConfig(
        material = "GRAY_STAINED_GLASS_PANE",
        displayName = " "
    )
)

@ConfigSerializable
data class MenuItemConfig(
    val slots: List<Int> = emptyList(),
    val item: ItemConfig = ItemConfig(),
    val action: String? = null,
    val sound: String? = null,
    val permission: String? = null
)

@ConfigSerializable
data class MenuConfig(
    val title: String = "<gold>Menú</gold>",
    val size: Int = 27,
    val filler: FillerConfig = FillerConfig(),
    val items: Map<String, MenuItemConfig> = emptyMap()
)

object MenuActionRegistry {
    private val globalActions = ConcurrentHashMap<String, (Player, InventoryClickEvent) -> Unit>()

    fun register(actionName: String, handler: (Player, InventoryClickEvent) -> Unit) {
        globalActions[actionName.lowercase()] = handler
    }

    fun getAction(actionName: String): ((Player, InventoryClickEvent) -> Unit)? {
        return globalActions[actionName.lowercase()]
    }

    fun unregister(actionName: String) {
        globalActions.remove(actionName.lowercase())
    }
}

class CustomMenu(
    val title: Component,
    val slots: Int,
    private val plugin: Plugin
) {
    val inventory: Inventory = Bukkit.createInventory(null, slots, title)
    private val clickHandlers = ConcurrentHashMap<Int, (Player, InventoryClickEvent) -> Unit>()
    val interactableSlots = ConcurrentHashMap.newKeySet<Int>()
    var onClose: ((Player) -> Unit)? = null
    
    private val localActions = ConcurrentHashMap<String, (Player, InventoryClickEvent) -> Unit>()

    fun registerLocalAction(actionName: String, handler: (Player, InventoryClickEvent) -> Unit) {
        localActions[actionName.lowercase()] = handler
    }

    fun getActionHandler(actionName: String): ((Player, InventoryClickEvent) -> Unit)? {
        return localActions[actionName.lowercase()] ?: MenuActionRegistry.getAction(actionName)
    }

    /**
     * Define un ítem en un slot y su manejador de evento al hacer clic.
     */
    fun setItem(slot: Int, item: ItemStack, onClick: ((Player, InventoryClickEvent) -> Unit)? = null) {
        if (slot in 0 until slots) {
            inventory.setItem(slot, item)
            if (onClick != null) {
                clickHandlers[slot] = onClick
            } else {
                clickHandlers.remove(slot)
            }
        }
    }

    /**
     * Carga el layout y comportamiento desde una MenuConfig, soportando placeholders locales opcionales.
     */
    fun loadFromConfig(config: MenuConfig, placeholders: Map<String, String> = emptyMap()) {
        // 1. Relleno de fondo si está activo
        if (config.filler.enabled) {
            val fillerItem = config.filler.item.toItemStack()
            for (i in 0 until slots) {
                setItem(i, fillerItem.clone())
            }
        }

        // 2. Cargar ítems configurados
        config.items.forEach { (_, menuItemConfig) ->
            val processedItemConfig = applyPlaceholders(menuItemConfig.item, placeholders)
            val itemStack = processedItemConfig.toItemStack()

            menuItemConfig.slots.forEach { slot ->
                if (slot in 0 until slots) {
                    setItem(slot, itemStack.clone()) { player, event ->
                        // Verificar permiso
                        if (menuItemConfig.permission != null && !player.hasPermission(menuItemConfig.permission)) {
                            player.sendMessage(ColorUtility.parse("<red>❌ No tienes permiso para usar esto.</red>"))
                            return@setItem
                        }

                        // Sonido
                        if (menuItemConfig.sound != null) {
                            try {
                                val soundEnum = org.bukkit.Sound.valueOf(menuItemConfig.sound.uppercase())
                                player.playSound(player.location, soundEnum, 1.0f, 1.0f)
                            } catch (e: Exception) {}
                        }

                        // Acción
                        if (menuItemConfig.action != null) {
                            val handler = getActionHandler(menuItemConfig.action)
                            if (handler != null) {
                                handler(player, event)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun applyPlaceholders(config: ItemConfig, placeholders: Map<String, String>): ItemConfig {
        if (placeholders.isEmpty()) return config
        
        var display = config.displayName
        if (display != null) {
            placeholders.forEach { (k, v) ->
                display = display!!.replace(k, v)
            }
        }
        
        val processedLore = config.lore.map { line ->
            var temp = line
            placeholders.forEach { (k, v) ->
                temp = temp.replace(k, v)
            }
            temp
        }

        return config.copy(
            displayName = display,
            lore = processedLore
        )
    }

    fun open(player: Player) {
        player.openInventory(inventory)
        MenuManager.registerActiveMenu(inventory, this)
    }

    /**
     * Procesa de forma segura el evento de clic en el inventario del menú.
     */
    fun handleInventoryClick(event: InventoryClickEvent) {
        event.isCancelled = true // Antisteal estricto por defecto
        val player = event.whoClicked as? Player ?: return
        val slot = event.rawSlot
        val handler = clickHandlers[slot]
        if (handler != null) {
            handler(player, event)
        }
    }
}

object MenuManager : Listener {
    private val activeMenus = ConcurrentHashMap<Inventory, CustomMenu>()

    fun registerActiveMenu(inventory: Inventory, menu: CustomMenu) {
        activeMenus[inventory] = menu
    }

    /**
     * Inicializa y registra el MenuManager en el servidor.
     */
    fun init(plugin: Plugin) {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val inventory = event.inventory
        val menu = activeMenus[inventory] ?: return

        if (event.rawSlot < 0) return

        if (event.rawSlot < event.inventory.size) {
            if (menu.interactableSlots.contains(event.rawSlot)) {
                event.isCancelled = false
            } else {
                event.isCancelled = true
                menu.handleInventoryClick(event)
            }
        } else {
            if (event.isShiftClick) {
                event.isCancelled = true // Previene bypasses mediante Shift-Click
            } else {
                event.isCancelled = false
            }
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val inventory = event.inventory
        val menu = activeMenus[inventory] ?: return
        
        val hasNonInteractableDrag = event.rawSlots.any { it < inventory.size && !menu.interactableSlots.contains(it) }
        if (hasNonInteractableDrag) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val menu = activeMenus.remove(event.inventory)
        menu?.onClose?.invoke(player)
    }
}
