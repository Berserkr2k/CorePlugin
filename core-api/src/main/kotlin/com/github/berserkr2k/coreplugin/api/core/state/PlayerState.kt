package com.github.berserkr2k.coreplugin.api.core.state

import java.util.UUID

interface StateContainer

class StateContainerType<T : StateContainer>(
    val defaultFactory: () -> T
)

interface PlayerStateService {
    fun <T : StateContainer> getContainer(uuid: UUID, type: StateContainerType<T>): T
    fun removeContainers(uuid: UUID)
}
