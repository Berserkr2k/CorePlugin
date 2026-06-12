package com.github.berserkr2k.coreplugin.platform.paper

import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilder
import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilderFactory
import com.github.berserkr2k.coreplugin.api.config.ItemConfig
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

@Suppress("DEPRECATION")
class ItemBuilderFactoryImpl : ItemBuilderFactory {

    override fun create(material: Material): ItemBuilder {
        return ItemBuilderImpl(ItemStack(material))
    }

    override fun createSkull(): ItemBuilder {
        return ItemBuilderImpl(ItemStack(Material.PLAYER_HEAD))
    }

    override fun fromItemStack(item: ItemStack): ItemBuilder {
        return ItemBuilderImpl(item.clone())
    }

    override fun builder(material: Material): ItemBuilder {
        return create(material)
    }

    override fun builder(itemStack: ItemStack): ItemBuilder {
        return fromItemStack(itemStack)
    }

    override fun builder(config: ItemConfig): ItemBuilder {
        val material = Material.matchMaterial(config.material) ?: Material.STONE
        val builder = if (material == Material.PLAYER_HEAD) createSkull() else create(material)

        builder.amount(config.amount)

        val displayName = config.displayName
        if (displayName != null) {
            builder.name(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(displayName))
        }
        if (config.lore.isNotEmpty()) {
            builder.lore(config.lore.map { net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(it) })
        }

        config.enchantments.forEach { (name, level) ->
            @Suppress("DEPRECATION")
            val enchant = Enchantment.getByName(name.uppercase()) 
                ?: Enchantment.getByKey(NamespacedKey.minecraft(name.lowercase()))
            if (enchant != null) {
                builder.enchant(enchant, level)
            }
        }

        config.itemFlags.forEach { flagName ->
            try {
                val flag = ItemFlag.valueOf(flagName.uppercase())
                builder.flag(flag)
            } catch (e: Exception) {}
        }

        val customModelData = config.customModelData
        if (customModelData != null) {
            builder.customModelData(customModelData)
        }

        val skullTexture = config.skullTexture
        if (skullTexture != null) {
            builder.skullTexture(skullTexture)
        }

        // Apply remaining properties for backward compatibility
        if (builder is ItemBuilderImpl) {
            builder.unbreakable(config.unbreakable)
            
            if (config.glow) {
                builder.glow(true)
            }
            val potionType = config.potionType
            if (potionType != null) {
                builder.potionType(potionType)
            }
            val leatherColor = config.leatherColor
            if (leatherColor != null) {
                builder.leatherColor(leatherColor)
            }
            val damage = config.damage
            if (damage != null) {
                builder.damage(damage)
            }
            
            val skullOwner = config.skullOwner
            val skullUuid = config.skullUuid
            if (skullOwner != null && config.skullTexture == null) {
                builder.skullOwner(skullOwner)
            } else if (skullUuid != null && config.skullTexture == null) {
                try {
                    val uuid = java.util.UUID.fromString(skullUuid)
                    builder.writeNBT(
                        NamespacedKey("coreplugin", "skull_uuid"),
                        PersistentDataType.STRING,
                        uuid.toString()
                    )
                } catch (e: Exception) {}
            }
        }

        config.pdc.forEach { (key, value) ->
            val namespacedKey = if (key.contains(":")) {
                val parts = key.split(":", limit = 2)
                NamespacedKey(parts[0].lowercase(), parts[1].lowercase())
            } else {
                NamespacedKey("coreplugin", key.lowercase())
            }

            when {
                value.equals("true", ignoreCase = true) -> {
                    builder.writeNBT(namespacedKey, PersistentDataType.BOOLEAN, true)
                }
                value.equals("false", ignoreCase = true) -> {
                    builder.writeNBT(namespacedKey, PersistentDataType.BOOLEAN, false)
                }
                value.toIntOrNull() != null -> {
                    builder.writeNBT(namespacedKey, PersistentDataType.INTEGER, value.toInt())
                }
                value.toDoubleOrNull() != null -> {
                    builder.writeNBT(namespacedKey, PersistentDataType.DOUBLE, value.toDouble())
                }
                else -> {
                    builder.writeNBT(namespacedKey, PersistentDataType.STRING, value)
                }
            }
        }

        return builder
    }
}
