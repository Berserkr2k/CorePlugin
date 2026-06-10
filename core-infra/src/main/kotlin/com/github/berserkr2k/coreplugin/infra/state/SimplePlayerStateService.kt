package com.github.berserkr2k.coreplugin.infra.state

import com.github.berserkr2k.coreplugin.api.state.PlayerStateService
import com.github.berserkr2k.coreplugin.api.state.StateContainer
import com.github.berserkr2k.coreplugin.api.state.StateContainerType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SimplePlayerStateService : PlayerStateService {
    private val states = ConcurrentHashMap<UUID, ConcurrentHashMap<StateContainerType<*>, StateContainer>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : StateContainer> getContainer(uuid: UUID, type: StateContainerType<T>): T {
        val playerStates = states.computeIfAbsent(uuid) { ConcurrentHashMap() }
        return playerStates.computeIfAbsent(type) { type.defaultFactory() } as T
    }

    override fun removeContainers(uuid: UUID) {
        states.remove(uuid)
    }
}
