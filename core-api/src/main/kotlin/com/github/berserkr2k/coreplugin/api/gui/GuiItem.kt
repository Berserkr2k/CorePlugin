package com.github.berserkr2k.coreplugin.api.gui

import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

/**
 * Representa un botón dentro de un menú.
 * Contiene el ítem visual y la acción que se ejecutará al hacer clic.
 */
class GuiItem(
    val itemStack: ItemStack,
    val action: ((InventoryClickEvent) -> Unit)? = null
)