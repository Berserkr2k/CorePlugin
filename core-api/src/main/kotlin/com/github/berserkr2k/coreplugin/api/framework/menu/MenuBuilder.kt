package com.github.berserkr2k.coreplugin.api.framework.menu

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import net.kyori.adventure.text.Component

interface MenuBuilder {
    fun title(title: Component): MenuBuilder
    fun slots(slots: Int): MenuBuilder
    fun button(slot: Int, button: Button): MenuBuilder
    fun fill(button: Button): MenuBuilder
    fun closeAction(action: (Player) -> Unit): MenuBuilder
    fun interactableSlots(slots: Collection<Int>): MenuBuilder
    fun registerAction(actionName: String, handler: (Player) -> Unit): MenuBuilder
    fun loadFromConfig(config: MenuConfig, placeholders: Map<String, String> = emptyMap(), ignoreSlots: List<Int> = emptyList()): MenuBuilder
    fun <T> placePaginatedItems(
        config: MenuConfig,
        items: List<T>,
        previousPageItem: ItemStack,
        nextPageItem: ItemStack,
        render: (T, Int) -> Unit
    ): MenuBuilder
    fun <T> placeDynamicItems(
        config: MenuConfig,
        items: List<T>,
        getGuiSlot: (T) -> Int,
        startSlot: Int = 10,
        render: (T, Int) -> Unit
    ): MenuBuilder
    fun build(): Menu
}
