package com.github.berserkr2k.coreplugin.common.gui

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import java.util.concurrent.ConcurrentHashMap

object MenuActionRegistry {
    private val globalActions = ConcurrentHashMap<String, (Player, InventoryClickEvent) -> Unit>()

    fun register(actionName: String, handler: (Player, InventoryClickEvent) -> Unit) {
        globalActions[actionName.lowercase()] = handler
    }

    fun getAction(actionName: String): ((Player, InventoryClickEvent) -> Unit)? {
        return globalActions[actionName.lowercase()]
    }

    fun unregister(actionName: String) {
        globalActions.remove(actionName.lowercase())
    }
}
