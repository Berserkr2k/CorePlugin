package com.github.berserkr2k.coreplugin.api.framework.item

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import net.kyori.adventure.text.Component
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType

interface ItemBuilder {
    fun type(material: Material): ItemBuilder
    fun amount(amount: Int): ItemBuilder
    fun name(name: Component): ItemBuilder
    fun lore(lines: List<Component>): ItemBuilder
    fun lore(vararg lines: Component): ItemBuilder
    fun enchant(enchantment: Enchantment, level: Int): ItemBuilder
    fun flag(vararg flags: ItemFlag): ItemFlagSelector
    fun customModelData(data: Int): ItemBuilder
    fun skullTexture(base64: String): ItemBuilder

    // Soporte nativo enterprise para metadatos (PDC)
    fun <T, Z : Any> writeNBT(key: NamespacedKey, type: PersistentDataType<T, Z>, value: Z): ItemBuilder

    fun build(): ItemStack

    // --- Backward Compatibility Methods ---
    @Deprecated("Use name instead", ReplaceWith("name(net.kyori.adventure.text.Component.text(name ?: \"\"))"))
    fun displayName(name: String?): ItemBuilder
    @Deprecated("Use name instead", ReplaceWith("name(name ?: net.kyori.adventure.text.Component.empty())"))
    fun displayName(name: Component?): ItemBuilder
    @Deprecated("Use lore instead", ReplaceWith("lore(loreLines)"))
    fun loreComponents(loreLines: List<Component>): ItemBuilder
    @Deprecated("No longer supported natively, use enchant/flag/etc.")
    fun glow(glow: Boolean): ItemBuilder
    @Deprecated("Use skullTexture instead", ReplaceWith("skullTexture(texture)"))
    fun skull(texture: String): ItemBuilder
    @Deprecated("No longer supported natively")
    fun skullOwner(playerName: String): ItemBuilder
    @Deprecated("No longer supported natively")
    fun skullProfile(profile: org.bukkit.profile.PlayerProfile?): ItemBuilder
}

@Deprecated("Use lore instead", ReplaceWith("lore(loreLines.map { net.kyori.adventure.text.Component.text(it) })"))
fun ItemBuilder.lore(loreLines: List<String>): ItemBuilder {
    return this.lore(loreLines.map { Component.text(it) })
}

interface ItemFlagSelector : ItemBuilder

interface ItemBuilderFactory {
    fun create(material: Material): ItemBuilder
    fun createSkull(): ItemBuilder
    fun fromItemStack(item: ItemStack): ItemBuilder

    // --- Backward Compatibility Methods ---
    @Deprecated("Use create instead", ReplaceWith("create(material)"))
    fun builder(material: Material): ItemBuilder
    @Deprecated("Use fromItemStack instead", ReplaceWith("fromItemStack(itemStack)"))
    fun builder(itemStack: ItemStack): ItemBuilder
    @Deprecated("Use builder(config) in the factory implementation")
    fun builder(config: com.github.berserkr2k.coreplugin.api.config.ItemConfig): ItemBuilder
}
