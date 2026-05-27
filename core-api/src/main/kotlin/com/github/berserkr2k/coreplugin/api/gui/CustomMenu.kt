package com.github.berserkr2k.coreplugin.api.gui

import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

/**
 * Constructor fluido para crear inventarios interactivos.
 * Implementa InventoryHolder para identificar el menú de forma segura en los eventos.
 */
class CustomMenu(
    title: String,
    rows: Int
) : InventoryHolder {

    private val inventory: Inventory
    // Mapa que guarda la posición (slot) y el botón correspondiente
    private val items = mutableMapOf<Int, GuiItem>()

    init {
        // Traducimos el título de MiniMessage al formato legacy que requieren los inventarios
        val mm = MiniMessage.miniMessage()
        val serializer = LegacyComponentSerializer.legacySection()
        val legacyTitle = serializer.serialize(mm.deserialize(title))

        // Multiplicamos las filas por 9 (tamaño estándar de Bukkit)
        val size = (rows.coerceIn(1, 6)) * 9
        inventory = Bukkit.createInventory(this, size, legacyTitle)
    }

    /**
     * Coloca un botón en una ranura específica.
     * @param slot Posición (0 a size-1).
     * @param item El GuiItem con su ítem y acción.
     */
    fun setItem(slot: Int, item: GuiItem): CustomMenu {
        if (slot in 0 until inventory.size) {
            items[slot] = item
            inventory.setItem(slot, item.itemStack)
        }
        return this
    }

    /**
     * Obtiene el botón configurado en un slot específico.
     */
    fun getItem(slot: Int): GuiItem? = items[slot]

    /**
     * Abre este menú para un jugador.
     */
    fun open(player: Player) {
        player.openInventory(inventory)
    }

    // Requisito de la interfaz InventoryHolder
    override fun getInventory(): Inventory = inventory
}