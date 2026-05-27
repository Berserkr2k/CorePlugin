package com.github.berserkr2k.coreplugin.v1_8_R3.item

import com.github.berserkr2k.coreplugin.api.item.ItemBuilder
import com.github.berserkr2k.coreplugin.api.item.ItemFactory
import org.bukkit.Material

/**
 * Fábrica que entrega instancias del ItemBuilder preparadas para la 1.8.8.
 */
class LegacyItemFactory : ItemFactory {

    override fun create(material: Material): ItemBuilder {
        return LegacyItemBuilder(material)
    }
}