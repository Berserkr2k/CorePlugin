package com.github.berserkr2k.coreplugin.api.action

import org.bukkit.entity.Player
import org.bukkit.Location

interface ExecutionContext {
    val player: Player?
    val location: Location?
    val parameters: Map<String, Any>
}

interface Action {
    fun execute(context: ExecutionContext)
}

interface ActionExecutor {
    fun execute(player: Player, action: Action)
    fun execute(player: Player, actions: List<Action>)
    fun parse(actionString: String): Action
}
