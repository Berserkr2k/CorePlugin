package com.github.berserkr2k.coreplugin.api.feature.trails

import java.util.UUID
import java.util.concurrent.CompletableFuture

interface ProjectileTrailService {
    fun getActiveTrail(uuid: UUID): String?
    fun savePlayerTrail(uuid: UUID, trailId: String?): CompletableFuture<Void>
    fun loadPlayerTrail(uuid: UUID): CompletableFuture<String?>
    fun loadAllTrails()
}
