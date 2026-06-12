package com.github.berserkr2k.coreplugin.infrastructure.regions.state

import com.github.berserkr2k.coreplugin.infrastructure.regions.CompiledRegion
import com.github.berserkr2k.coreplugin.api.core.state.StateContainer
import com.github.berserkr2k.coreplugin.api.core.state.StateContainerType

class PlayerRegionStateContainer : StateContainer {
    @Volatile
    var currentRegions: Array<CompiledRegion> = emptyArray()
}

object PlayerRegionState {
    val STATE_TYPE = StateContainerType { PlayerRegionStateContainer() }
}
