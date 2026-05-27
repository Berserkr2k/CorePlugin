package com.github.berserkr2k.coreplugin.listener

import com.github.berserkr2k.coreplugin.api.gui.CustomMenu
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class MenuListener : Listener {

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val inventory = event.inventory
        // Magia pura: Preguntamos si el inventario que clickeó es uno de nuestros CustomMenu
        val holder = inventory.holder

        if (holder is CustomMenu) {
            // Cancelamos el evento al instante para que no se puedan llevar el ítem a su inventario
            event.isCancelled = true

            // Verificamos que haya clickeado en el menú de arriba y no en su propio inventario
            if (event.clickedInventory == inventory) {
                // Buscamos si hay un botón configurado en ese slot
                val guiItem = holder.getItem(event.slot)

                // Si el botón existe y tiene una acción, la ejecutamos
                guiItem?.action?.invoke(event)
            }
        }
    }
}