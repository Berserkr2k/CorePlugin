package com.github.berserkr2k.coreplugin.api.framework.menu

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

interface Menu {
    fun open(player: Player)
    fun update(player: Player)
    fun getItem(slot: Int): ItemStack?
}
