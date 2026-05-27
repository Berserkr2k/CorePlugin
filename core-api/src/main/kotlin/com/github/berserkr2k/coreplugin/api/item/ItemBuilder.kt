package com.github.berserkr2k.coreplugin.api.item

import org.bukkit.inventory.ItemStack

/**
 * Contrato base para la construcción fluida de ítems.
 * Permite encadenar métodos para crear un ItemStack de forma limpia.
 */
interface ItemBuilder {

    /**
     * Establece el nombre del ítem.
     * @param name Texto en formato MiniMessage (ej: "<red><bold>Espada").
     */
    fun name(name: String): ItemBuilder

    /**
     * Establece la descripción (lore) del ítem usando múltiples líneas.
     * @param lines Textos en formato MiniMessage.
     */
    fun lore(vararg lines: String): ItemBuilder

    /**
     * Establece la descripción (lore) del ítem a partir de una lista.
     */
    fun lore(lines: List<String>): ItemBuilder

    /**
     * Establece la cantidad de ítems en el stack (1-64).
     */
    fun amount(amount: Int): ItemBuilder

    /**
     * Define la durabilidad o el "data value" (vital para colores en la 1.8.8).
     */
    fun durability(durability: Short): ItemBuilder

    /**
     * Aplica un CustomModelData para texturas de Resource Packs.
     * Nota: En la versión 1.8.8 este método será ignorado silenciosamente.
     */
    fun customModelData(data: Int): ItemBuilder

    /**
     * Hace que el ítem brille como si estuviera encantado, pero ocultando el texto del encantamiento.
     */
    fun glow(glow: Boolean = true): ItemBuilder

    /**
     * Finaliza la construcción y devuelve el ítem nativo de Bukkit.
     */
    fun build(): ItemStack
}