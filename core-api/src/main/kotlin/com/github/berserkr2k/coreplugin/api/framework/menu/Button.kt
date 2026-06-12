package com.github.berserkr2k.coreplugin.api.framework.menu

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

interface Button {
    val icon: ItemStack
    fun onClick(player: Player)
    fun onClick(player: Player, clickType: org.bukkit.event.inventory.ClickType) {
        onClick(player)
    }

    companion object {
        private var factory: (() -> Builder)? = null

        fun setFactory(factory: () -> Builder) {
            this.factory = factory
        }

        fun builder(): Builder {
            return factory?.invoke() ?: throw IllegalStateException("Button Builder factory is not initialized yet!")
        }
    }

    interface Builder {
        fun icon(item: ItemStack): Builder
        fun onClick(action: (Player) -> Unit): Builder
        fun onClick(action: (Player, org.bukkit.event.inventory.ClickType) -> Unit): Builder
        fun build(): Button
    }
}
