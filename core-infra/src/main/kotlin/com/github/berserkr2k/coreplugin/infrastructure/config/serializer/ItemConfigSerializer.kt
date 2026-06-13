package com.github.berserkr2k.coreplugin.infrastructure.config.serializer

import com.github.berserkr2k.coreplugin.api.config.ItemConfig
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

object ItemConfigSerializer : TypeSerializer<ItemConfig> {
    override fun deserialize(type: Type, node: ConfigurationNode): ItemConfig {
        if (node.virtual()) return ItemConfig()

        // Manual deserialization of enchantments (Map<String, Int>)
        val enchantmentsNode = node.node("enchantments")
        val enchantments = mutableMapOf<String, Int>()
        if (!enchantmentsNode.virtual() && enchantmentsNode.isMap) {
            for (entry in enchantmentsNode.childrenMap().entries) {
                val key = entry.key.toString()
                val value = entry.value.getInt(0)
                enchantments[key] = value
            }
        }

        // Manual deserialization of pdc (Map<String, String>)
        val pdcNode = node.node("pdc")
        val pdc = mutableMapOf<String, String>()
        if (!pdcNode.virtual() && pdcNode.isMap) {
            for (entry in pdcNode.childrenMap().entries) {
                val key = entry.key.toString()
                val value = entry.value.string ?: ""
                pdc[key] = value
            }
        }

        return ItemConfig(
            material = node.node("material").getString("AIR"),
            skullTexture = node.node("skullTexture").string,
            skullOwner = node.node("skullOwner").string,
            skullUuid = node.node("skullUuid").string,
            amount = node.node("amount").getInt(1),
            displayName = node.node("displayName").string,
            lore = node.node("lore").getList(String::class.java) ?: emptyList(),
            enchantments = enchantments,
            itemFlags = node.node("itemFlags").getList(String::class.java) ?: emptyList(),
            unbreakable = node.node("unbreakable").getBoolean(false),
            customModelData = if (node.node("customModelData").virtual()) null else node.node("customModelData").getInt(),
            glow = node.node("glow").getBoolean(false),
            potionType = node.node("potionType").string,
            leatherColor = node.node("leatherColor").string,
            damage = if (node.node("damage").virtual()) null else node.node("damage").getInt(),
            pdc = pdc
        )
    }

    override fun serialize(type: Type, obj: ItemConfig?, node: ConfigurationNode) {
        if (obj == null) {
            node.raw(null)
            return
        }
        node.node("material").set(obj.material)
        node.node("skullTexture").set(obj.skullTexture)
        node.node("skullOwner").set(obj.skullOwner)
        node.node("skullUuid").set(obj.skullUuid)
        node.node("amount").set(obj.amount)
        node.node("displayName").set(obj.displayName)
        node.node("lore").set(obj.lore)

        // Manual serialization of enchantments
        val enchantmentsNode = node.node("enchantments")
        enchantmentsNode.raw(null)
        obj.enchantments.forEach { (key, value) ->
            enchantmentsNode.node(key).set(value)
        }

        node.node("itemFlags").set(obj.itemFlags)
        node.node("unbreakable").set(obj.unbreakable)
        node.node("customModelData").set(obj.customModelData)
        node.node("glow").set(obj.glow)
        node.node("potionType").set(obj.potionType)
        node.node("leatherColor").set(obj.leatherColor)
        node.node("damage").set(obj.damage)

        // Manual serialization of pdc
        val pdcNode = node.node("pdc")
        pdcNode.raw(null)
        obj.pdc.forEach { (key, value) ->
            pdcNode.node(key).set(value)
        }
    }
}
