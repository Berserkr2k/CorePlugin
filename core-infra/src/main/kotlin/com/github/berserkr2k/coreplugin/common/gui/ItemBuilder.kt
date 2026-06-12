package com.github.berserkr2k.coreplugin.common.gui

import com.github.berserkr2k.coreplugin.api.config.ItemConfig
import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilderFactory
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack

fun ItemConfig.toItemStack(): ItemStack {
    val factory = Bukkit.getServicesManager().load(ItemBuilderFactory::class.java)
        ?: throw IllegalStateException("ItemBuilderFactory service is not registered in Bukkit's ServiceManager!")
    return factory.builder(this).build()
}
