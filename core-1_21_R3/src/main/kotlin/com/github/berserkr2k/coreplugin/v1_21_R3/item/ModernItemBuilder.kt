package com.github.berserkr2k.coreplugin.v1_21_R3.item

import com.github.berserkr2k.coreplugin.api.item.ItemBuilder
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

/**
 * Implementación de ItemBuilder para Minecraft 1.21.X.
 * Aplica colores Hexadecimales de forma nativa y soporta CustomModelData.
 */
class ModernItemBuilder(material: Material) : ItemBuilder {

    private val item = ItemStack(material)
    private val meta: ItemMeta? = item.itemMeta

    private val miniMessage = MiniMessage.miniMessage()

    // MAGIA SPIGOT: El Spigot moderno lee colores hex si están en un formato específico (§x§f§f...).
    // Este serializador convierte tu <red> o <#FF0000> exactamente a ese formato nativo.
    private val modernSerializer = LegacyComponentSerializer.builder()
        .character('§')
        .hexColors()
        .useUnusualXRepeatedCharacterHexFormat()
        .build()

    private fun translate(text: String): String {
        val component = miniMessage.deserialize(text)
        return modernSerializer.serialize(component)
    }

    override fun name(name: String): ItemBuilder {
        meta?.setDisplayName(translate(name))
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
        // La 1.21 soporta stacks mayores a 64 en el código (hasta 99 normalmente)
        item.amount = amount.coerceIn(1, 99)
        return this
    }

    override fun durability(durability: Short): ItemBuilder {
        // Las durabilidades antiguas no existen en 1.21. Si quieres aplicarlo a herramientas,
        // requeriría un Damageable. Por ahora lo ignoramos.
        return this
    }

    override fun customModelData(data: Int): ItemBuilder {
        // En la 1.21, este método es 100% nativo.
        meta?.setCustomModelData(data)
        return this
    }

    override fun glow(glow: Boolean): ItemBuilder {
        if (glow) {
            meta?.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta?.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        } else {
            meta?.removeEnchant(Enchantment.UNBREAKING)
            meta?.removeItemFlags(ItemFlag.HIDE_ENCHANTS)
        }
        return this
    }

    override fun build(): ItemStack {
        item.itemMeta = meta
        return item
    }
}