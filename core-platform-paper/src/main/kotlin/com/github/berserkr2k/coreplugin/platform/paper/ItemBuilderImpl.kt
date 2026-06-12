package com.github.berserkr2k.coreplugin.platform.paper

import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilder
import com.github.berserkr2k.coreplugin.api.framework.item.ItemFlagSelector
import com.github.berserkr2k.coreplugin.common.ColorUtility
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
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
import org.bukkit.Color
import java.util.UUID

@Suppress("DEPRECATION")
class ItemBuilderImpl(private val itemStack: ItemStack) : ItemBuilder, ItemFlagSelector {

    private inline fun updateMeta(block: (ItemMeta) -> Unit) {
        val meta = itemStack.itemMeta ?: return
        block(meta)
        itemStack.itemMeta = meta
    }

    override fun type(material: Material): ItemBuilder {
        itemStack.type = material
        return this
    }

    override fun amount(amount: Int): ItemBuilder {
        itemStack.amount = amount
        return this
    }

    override fun name(name: Component): ItemBuilder {
        updateMeta { meta ->
            var comp = name
            if (comp.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET) {
                comp = comp.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
            }
            meta.displayName(comp)
        }
        return this
    }

    override fun lore(lines: List<Component>): ItemBuilder {
        updateMeta { meta ->
            meta.lore(lines.map { line ->
                var comp = line
                if (comp.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET) {
                    comp = comp.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                }
                comp
            })
        }
        return this
    }

    override fun lore(vararg lines: Component): ItemBuilder {
        return lore(lines.toList())
    }

    override fun enchant(enchantment: Enchantment, level: Int): ItemBuilder {
        updateMeta { meta ->
            meta.addEnchant(enchantment, level, true)
        }
        return this
    }

    override fun flag(vararg flags: ItemFlag): ItemFlagSelector {
        updateMeta { meta ->
            meta.addItemFlags(*flags)
        }
        return this
    }

    override fun customModelData(data: Int): ItemBuilder {
        updateMeta { meta ->
            meta.setCustomModelData(data)
        }
        return this
    }

    override fun skullTexture(base64: String): ItemBuilder {
        updateMeta { meta ->
            if (meta is SkullMeta) {
                try {
                    val profile = Bukkit.createProfile(UUID.randomUUID(), null)
                    val paperProfile = profile as? com.destroystokyo.paper.profile.PlayerProfile
                    if (paperProfile != null) {
                        paperProfile.setProperty(com.destroystokyo.paper.profile.ProfileProperty("textures", base64))
                        meta.playerProfile = paperProfile
                    }
                } catch (e: Exception) {
                    // Fallback using reflection if paper profile is not castable
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
        return this
    }

    override fun <T, Z : Any> writeNBT(key: NamespacedKey, type: PersistentDataType<T, Z>, value: Z): ItemBuilder {
        updateMeta { meta ->
            meta.persistentDataContainer.set(key, type, value)
        }
        return this
    }

    override fun build(): ItemStack {
        return itemStack
    }

    // --- Backward Compatibility Methods ---

    override fun displayName(name: String?): ItemBuilder {
        if (name == null) {
            updateMeta { it.displayName(null) }
        } else {
            name(ColorUtility.parse(name))
        }
        return this
    }

    override fun displayName(name: Component?): ItemBuilder {
        if (name == null) {
            updateMeta { it.displayName(null) }
        } else {
            name(name)
        }
        return this
    }

    override fun loreComponents(loreLines: List<Component>): ItemBuilder {
        return lore(loreLines)
    }

    override fun glow(glow: Boolean): ItemBuilder {
        if (glow) {
            updateMeta { meta ->
                meta.addEnchant(Enchantment.UNBREAKING, 1, true)
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            }
        }
        return this
    }

    override fun skull(texture: String): ItemBuilder {
        return skullTexture(texture)
    }

    override fun skullOwner(playerName: String): ItemBuilder {
        updateMeta { meta ->
            if (meta is SkullMeta) {
                try {
                    val profile = Bukkit.createProfile(playerName)
                    profile.complete()
                    meta.playerProfile = profile
                } catch (e: Exception) {
                    meta.owningPlayer = Bukkit.getOfflinePlayer(playerName)
                }
            }
        }
        return this
    }

    override fun skullProfile(profile: org.bukkit.profile.PlayerProfile?): ItemBuilder {
        if (profile != null) {
            updateMeta { meta ->
                if (meta is SkullMeta) {
                    meta.playerProfile = profile as? com.destroystokyo.paper.profile.PlayerProfile
                }
            }
        }
        return this
    }

    fun unbreakable(unbreakable: Boolean): ItemBuilder {
        updateMeta { it.isUnbreakable = unbreakable }
        return this
    }

    fun customModelData(data: Int?): ItemBuilder {
        updateMeta { it.setCustomModelData(data) }
        return this
    }

    fun potionType(type: String?): ItemBuilder {
        if (type != null) {
            updateMeta { meta ->
                if (meta is PotionMeta) {
                    try {
                        meta.basePotionType = PotionType.valueOf(type.uppercase())
                    } catch (e: Exception) {}
                }
            }
        }
        return this
    }

    fun leatherColor(hexColor: String?): ItemBuilder {
        if (hexColor != null) {
            updateMeta { meta ->
                if (meta is LeatherArmorMeta) {
                    try {
                        val colorStr = hexColor.replace("#", "")
                        val rgb = colorStr.toInt(16)
                        meta.setColor(Color.fromRGB(rgb))
                    } catch (e: Exception) {}
                }
            }
        }
        return this
    }

    fun leatherColor(color: Color?): ItemBuilder {
        if (color != null) {
            updateMeta { meta ->
                if (meta is LeatherArmorMeta) {
                    meta.setColor(color)
                }
            }
        }
        return this
    }

    fun damage(dmg: Int?): ItemBuilder {
        if (dmg != null) {
            updateMeta { meta ->
                if (meta is Damageable) {
                    meta.damage = dmg
                }
            }
        }
        return this
    }
}
