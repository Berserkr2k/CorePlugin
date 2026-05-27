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
    val interactableSlots = ConcurrentHashMap.newKeySet<Int>()
    var onClose: ((Player) -> Unit)? = null

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

        // Si el click ocurre fuera de la ventana del menú, no hacemos nada
        if (event.rawSlot < 0) return

        // Procesamos la lógica de click
        if (event.rawSlot < event.inventory.size) {
            if (menu.interactableSlots.contains(event.rawSlot)) {
                // Permitimos la interacción estándar para colocar/quitar ítems en estos slots
                event.isCancelled = false
            } else {
                event.isCancelled = true
                menu.handleInventoryClick(event)
            }
        } else {
            // Permitimos clicks en el propio inventario del jugador para mover ítems,
            // excepto si están intentando hacer Shift-Click para meter un ítem en un slot no permitido
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
        
        // Si arrastran en cualquier slot del menú que no sea interactuable, cancelar
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
