package com.github.berserkr2k.coreplugin.common.gui

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
import java.util.concurrent.ConcurrentHashMap

class CustomMenu(
    val title: Component,
    val slots: Int,
    private val plugin: Plugin
) {
    val inventory: Inventory = Bukkit.createInventory(null, slots, title)
    private val clickHandlers = ConcurrentHashMap<Int, (Player, InventoryClickEvent) -> Unit>()

    /**
     * Define un ítem en un slot y su manejador de evento al hacer clic.
     */
    fun setItem(slot: Int, item: ItemStack, onClick: ((Player, InventoryClickEvent) -> Unit)? = null) {
        inventory.setItem(slot, item)
        if (onClick != null) {
            clickHandlers[slot] = onClick
        } else {
            clickHandlers.remove(slot)
        }
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

        // Cancelamos todo evento de click dentro de menús personalizados para prevenir robo (antisteal)
        event.isCancelled = true

        // Si el click ocurre fuera de la ventana del menú, no hacemos nada
        if (event.rawSlot < 0) return

        // Procesamos la lógica de click solo si ocurre dentro de los slots del menú superior
        if (event.rawSlot < event.inventory.size) {
            menu.handleInventoryClick(event)
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val inventory = event.inventory
        if (activeMenus.containsKey(inventory)) {
            event.isCancelled = true // Antisteal absoluto al arrastrar ítems (anti-dupe)
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        activeMenus.remove(event.inventory)
    }
}
