package com.github.berserkr2k.coreplugin.v1_21_R3.item

import com.github.berserkr2k.coreplugin.api.item.ItemBuilder
import com.github.berserkr2k.coreplugin.api.item.ItemFactory
import org.bukkit.Material

class ModernItemFactory : ItemFactory {
    override fun create(material: Material): ItemBuilder {
        return ModernItemBuilder(material)
    }
}