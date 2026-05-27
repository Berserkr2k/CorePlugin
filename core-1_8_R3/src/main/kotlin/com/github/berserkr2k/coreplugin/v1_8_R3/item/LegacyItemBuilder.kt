package com.github.berserkr2k.coreplugin.v1_8_R3.item

import com.github.berserkr2k.coreplugin.api.item.ItemBuilder
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

/**
 * Implementación de ItemBuilder para Minecraft 1.8.8.
 * Traduce los colores modernos (MiniMessage) al formato legacy (§)
 * y emula mecánicas modernas de la mejor forma posible.
 */
class LegacyItemBuilder(material: Material) : ItemBuilder {

    private val item = ItemStack(material)
    // En Bukkit, la "meta" de un ítem es donde se guarda el nombre, lore, etc.
    private val meta: ItemMeta? = item.itemMeta

    private val miniMessage = MiniMessage.miniMessage()
    // Serializador mágico: convierte <red> en §c y adapta los colores Hexadecimales al color clásico más cercano.
    private val legacySerializer = LegacyComponentSerializer.legacySection()

    /**
     * Función privada para evitar repetir la conversión de texto.
     */
    private fun translate(text: String): String {
        val component = miniMessage.deserialize(text)
        return legacySerializer.serialize(component)
    }

    override fun name(name: String): ItemBuilder {
        meta?.displayName = translate(name)
        return this
    }

    override fun lore(vararg lines: String): ItemBuilder {
        meta?.lore = lines.map { translate(it) }
        return this
    }

    override fun lore(lines: List<String>): ItemBuilder {
        meta?.lore = lines.map { translate(it) }
        return this
    }

    override fun amount(amount: Int): ItemBuilder {
        // coerceIn asegura que la cantidad nunca sea menor a 1 ni mayor a 64
        item.amount = amount.coerceIn(1, 64)
        return this
    }

    override fun durability(durability: Short): ItemBuilder {
        // En la 1.8.8 la durabilidad también se usa para los colores (ej. Lanas, Cristales)
        item.durability = durability
        return this
    }

    override fun customModelData(data: Int): ItemBuilder {
        // ¡Esta es la magia del adaptador! Como la 1.8 no soporta esto, 
        // simplemente lo ignoramos sin romper el servidor.
        return this
    }

    override fun glow(glow: Boolean): ItemBuilder {
        if (glow) {
            // Le ponemos un encantamiento inútil y ocultamos el texto del encantamiento
            meta?.addEnchant(Enchantment.DURABILITY, 1, true)
            meta?.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        } else {
            meta?.removeEnchant(Enchantment.DURABILITY)
            meta?.removeItemFlags(ItemFlag.HIDE_ENCHANTS)
        }
        return this
    }

    override fun build(): ItemStack {
        // Aplicamos la meta modificada de vuelta al ItemStack y lo entregamos
        item.itemMeta = meta
        return item
    }
}