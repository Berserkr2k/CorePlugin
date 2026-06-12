package com.github.berserkr2k.coreplugin.api.framework.regions

import org.bukkit.Location

interface RegionService {
    fun hasFlag(location: Location, flag: RegionFlag): Boolean
    fun getRegion(location: Location): Region?
    fun getRegions(location: Location): List<Region>
}
