package com.github.berserkr2k.coreplugin.infrastructure.regions.state

import com.github.berserkr2k.coreplugin.api.regions.CompiledRegion
import com.github.berserkr2k.coreplugin.api.state.StateContainer
import com.github.berserkr2k.coreplugin.api.state.StateContainerType

class PlayerRegionStateContainer : StateContainer {
    @Volatile
    var currentRegions: Array<CompiledRegion> = emptyArray()
}

object PlayerRegionState {
    val STATE_TYPE = StateContainerType { PlayerRegionStateContainer() }
}
