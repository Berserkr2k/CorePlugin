package com.github.berserkr2k.coreplugin.api.core.user

import java.util.UUID
import java.util.concurrent.CompletableFuture

interface ProfileRegistry {
    fun getProfile(uuid: UUID): UserProfile?
    fun getActiveProfiles(): Collection<UserProfile>
    fun loadProfile(uuid: UUID, username: String): CompletableFuture<UserProfile>
    fun acquireSyncLock(uuid: UUID)
    fun releaseSyncLock(uuid: UUID)
    fun isSyncLocked(uuid: UUID): Boolean
    fun unloadAndSave(uuid: UUID): CompletableFuture<Void>
    fun flushAllActive(): CompletableFuture<Void>
}
