package com.github.berserkr2k.coreplugin.infrastructure.regions.command

import com.github.berserkr2k.coreplugin.api.core.state.PlayerStateService
import com.github.berserkr2k.coreplugin.api.core.state.StateContainer
import com.github.berserkr2k.coreplugin.api.core.state.StateContainerType
import org.bukkit.Location
import java.util.UUID

class PlayerSelectionSession(private val stateService: PlayerStateService) {

    data class Selection(
        var pos1: Location? = null,
        var pos2: Location? = null
    )

    class SelectionStateContainer : StateContainer {
        val selection = Selection()
    }

    companion object {
        val STATE_TYPE = StateContainerType { SelectionStateContainer() }
    }

    fun getSelection(playerUuid: UUID): Selection {
        return stateService.getContainer(playerUuid, STATE_TYPE).selection
    }
}
