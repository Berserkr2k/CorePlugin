package com.github.berserkr2k.coreplugin.api.framework.regions

import java.util.UUID

interface Region {
    val id: String
    val worldUniqueId: UUID?
    val priority: Int
    val tags: Map<String, String>
    val type: RegionType
    fun contains(x: Int, y: Int, z: Int): Boolean
    fun hasFlag(flag: RegionFlag): Boolean
    fun isAllowed(flag: RegionFlag): Boolean
}
