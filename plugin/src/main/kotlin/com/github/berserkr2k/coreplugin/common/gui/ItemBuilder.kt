package com.github.berserkr2k.coreplugin.common.gui

import com.github.berserkr2k.coreplugin.common.ColorUtility
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Color
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionType
import java.util.UUID
import com.destroystokyo.paper.profile.PlayerProfile
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import net.kyori.adventure.text.Component

@ConfigSerializable
data class ItemConfig(
    val material: String = "AIR",
    val amount: Int = 1,
    val displayName: String? = null,
    val lore: List<String> = emptyList(),
    val enchantments: Map<String, Int> = emptyMap(),
    val itemFlags: List<String> = emptyList(),
    val unbreakable: Boolean = false,
    val customModelData: Int? = null,
    val skullTexture: String? = null, // base64
    val skullOwner: String? = null,   // player name
    val skullUuid: String? = null,    // UUID
    val glow: Boolean = false,
    val potionType: String? = null,
    val leatherColor: String? = null, // HEX #rrggbb
    val damage: Int? = null,          // durability damage
    val pdc: Map<String, String> = emptyMap() // key to value
) {
    fun toItemStack(): ItemStack {
        return ItemBuilder.fromConfig(this).build()
    }
}

class ItemBuilder(private var itemStack: ItemStack) {
    private var meta: ItemMeta? = itemStack.itemMeta

    constructor(material: Material, amount: Int = 1) : this(ItemStack(material, amount))

    fun amount(amount: Int): ItemBuilder {
        itemStack.amount = amount
        return this
    }

    fun displayName(name: String?): ItemBuilder {
        if (name == null) {
            meta?.displayName(null)
        } else {
            meta?.displayName(ColorUtility.parse(name))
        }
        return this
    }

    fun displayName(name: Component?): ItemBuilder {
        meta?.displayName(name)
        return this
    }

    fun lore(loreLines: List<String>): ItemBuilder {
        if (loreLines.isEmpty()) {
            meta?.lore(null)
        } else {
            meta?.lore(loreLines.map { ColorUtility.parse(it) })
        }
        return this
    }

    fun loreComponents(loreLines: List<Component>): ItemBuilder {
        meta?.lore(loreLines)
        return this
    }

    fun enchant(enchantment: Enchantment, level: Int): ItemBuilder {
        meta?.addEnchant(enchantment, level, true)
        return this
    }

    fun enchant(enchantments: Map<Enchantment, Int>): ItemBuilder {
        enchantments.forEach { (enchant, level) ->
            meta?.addEnchant(enchant, level, true)
        }
        return this
    }

    fun clearEnchants(): ItemBuilder {
        meta?.enchants?.keys?.forEach { meta?.removeEnchant(it) }
        return this
    }

    fun flags(vararg flags: ItemFlag): ItemBuilder {
        meta?.addItemFlags(*flags)
        return this
    }

    fun flags(flags: List<ItemFlag>): ItemBuilder {
        meta?.addItemFlags(*flags.toTypedArray())
        return this
    }

    fun unbreakable(unbreakable: Boolean): ItemBuilder {
        meta?.isUnbreakable = unbreakable
        return this
    }

    fun customModelData(data: Int?): ItemBuilder {
        meta?.setCustomModelData(data)
        return this
    }

    fun glow(glow: Boolean): ItemBuilder {
        if (glow) {
            meta?.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta?.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        }
        return this
    }

    fun skullTexture(base64: String?): ItemBuilder {
        if (base64 != null && meta is SkullMeta) {
            val skullMeta = meta as SkullMeta
            applyBase64Texture(skullMeta, base64)
        }
        return this
    }

    fun skullOwner(playerName: String?): ItemBuilder {
        if (playerName != null && meta is SkullMeta) {
            val skullMeta = meta as SkullMeta
            try {
                val profile = Bukkit.createProfile(playerName)
                profile.complete()
                skullMeta.playerProfile = profile
            } catch (e: Exception) {
                skullMeta.owningPlayer = Bukkit.getOfflinePlayer(playerName)
            }
        }
        return this
    }

    fun skullProfile(profile: PlayerProfile?): ItemBuilder {
        if (profile != null && meta is SkullMeta) {
            val skullMeta = meta as SkullMeta
            skullMeta.playerProfile = profile
        }
        return this
    }

    fun skullUuid(uuid: UUID?): ItemBuilder {
        if (uuid != null && meta is SkullMeta) {
            val skullMeta = meta as SkullMeta
            skullMeta.owningPlayer = Bukkit.getOfflinePlayer(uuid)
        }
        return this
    }

    fun potionType(type: String?): ItemBuilder {
        if (type != null && meta is PotionMeta) {
            val potionMeta = meta as PotionMeta
            try {
                potionMeta.basePotionType = PotionType.valueOf(type.uppercase())
            } catch (e: Exception) {
                // Fallback / legacy support
            }
        }
        return this
    }

    fun leatherColor(hexColor: String?): ItemBuilder {
        if (hexColor != null && meta is LeatherArmorMeta) {
            val leatherMeta = meta as LeatherArmorMeta
            try {
                val colorStr = hexColor.replace("#", "")
                val rgb = colorStr.toInt(16)
                leatherMeta.setColor(Color.fromRGB(rgb))
            } catch (e: Exception) {
                // Ignore invalid
            }
        }
        return this
    }

    fun leatherColor(color: Color?): ItemBuilder {
        if (color != null && meta is LeatherArmorMeta) {
            val leatherMeta = meta as LeatherArmorMeta
            leatherMeta.setColor(color)
        }
        return this
    }

    fun damage(dmg: Int?): ItemBuilder {
        if (dmg != null && meta is Damageable) {
            val damageable = meta as Damageable
            damageable.damage = dmg
        }
        return this
    }

    fun pdc(key: String, value: String): ItemBuilder {
        meta?.let {
            val namespacedKey = NamespacedKey("coreplugin", key.lowercase())
            it.persistentDataContainer.set(namespacedKey, PersistentDataType.STRING, value)
        }
        return this
    }

    fun pdc(key: String, value: Boolean): ItemBuilder {
        meta?.let {
            val namespacedKey = NamespacedKey("coreplugin", key.lowercase())
            it.persistentDataContainer.set(namespacedKey, PersistentDataType.BOOLEAN, value)
        }
        return this
    }

    fun pdc(tags: Map<String, String>): ItemBuilder {
        tags.forEach { (k, v) ->
            pdc(k, v)
        }
        return this
    }

    fun build(): ItemStack {
        if (meta != null) {
            itemStack.itemMeta = meta
        }
        return itemStack
    }

    companion object {
        fun fromConfig(config: ItemConfig): ItemBuilder {
            val material = Material.matchMaterial(config.material) ?: Material.STONE
            val builder = ItemBuilder(material, config.amount)
            
            if (config.displayName != null) {
                builder.displayName(config.displayName)
            }
            if (config.lore.isNotEmpty()) {
                builder.lore(config.lore)
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
                    builder.flags(flag)
                } catch (e: Exception) {}
            }

            builder.unbreakable(config.unbreakable)
            if (config.customModelData != null) {
                builder.customModelData(config.customModelData)
            }
            
            if (config.skullTexture != null) {
                builder.skullTexture(config.skullTexture)
            } else if (config.skullOwner != null) {
                builder.skullOwner(config.skullOwner)
            } else if (config.skullUuid != null) {
                try {
                    builder.skullUuid(UUID.fromString(config.skullUuid))
                } catch (e: Exception) {}
            }

            if (config.glow) {
                builder.glow(true)
            }
            if (config.potionType != null) {
                builder.potionType(config.potionType)
            }
            if (config.leatherColor != null) {
                builder.leatherColor(config.leatherColor)
            }
            if (config.damage != null) {
                builder.damage(config.damage)
            }
            if (config.pdc.isNotEmpty()) {
                builder.pdc(config.pdc)
            }

            return builder
        }

        private fun applyBase64Texture(meta: SkullMeta, base64: String) {
            try {
                val profile = Bukkit.createProfile(UUID.randomUUID(), null)
                val paperProfile = profile as? com.destroystokyo.paper.profile.PlayerProfile ?: return
                paperProfile.setProperty(com.destroystokyo.paper.profile.ProfileProperty("textures", base64))
                meta.playerProfile = paperProfile
            } catch (e: Exception) {
                try {
                    val gameProfileClass = Class.forName("com.mojang.authlib.GameProfile")
                    val propertyClass = Class.forName("com.mojang.authlib.properties.Property")
                    val propertyMapClass = Class.forName("com.mojang.authlib.properties.PropertyMap")
                    
                    val profileConstructor = gameProfileClass.getConstructor(UUID::class.java, String::class.java)
                    val profile = profileConstructor.newInstance(UUID.randomUUID(), null)
                    
                    val propertyConstructor = propertyClass.getConstructor(String::class.java, String::class.java)
                    val property = propertyConstructor.newInstance("textures", base64)
                    
                    val getPropertiesMethod = gameProfileClass.getMethod("getProperties")
                    val properties = getPropertiesMethod.invoke(profile)
                    
                    val putMethod = propertyMapClass.getMethod("put", Any::class.java, Any::class.java)
                    putMethod.invoke(properties, "textures", property)
                    
                    val profileField = meta.javaClass.getDeclaredField("profile")
                    profileField.isAccessible = true
                    profileField.set(meta, profile)
                } catch (ex: Exception) {
                    // Fallback fails silently
                }
            }
        }
    }
}
