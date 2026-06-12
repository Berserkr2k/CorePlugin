package com.github.berserkr2k.coreplugin.api.hologram

import org.bukkit.Location

interface HologramService {
    fun createHologram(id: String, location: Location, lines: List<String>)
    fun deleteHologram(id: String): Boolean
    fun editHologram(id: String, lines: List<String>): Boolean
    fun moveHologram(id: String, location: Location): Boolean
}
