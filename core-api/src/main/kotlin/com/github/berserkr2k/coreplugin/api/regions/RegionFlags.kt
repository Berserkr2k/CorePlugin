package com.github.berserkr2k.coreplugin.api.regions

object RegionFlags {
    const val NONE         = 0
    const val PVP          = 1 shl 0 // 0001
    const val BLOCK_BREAK  = 1 shl 1 // 0010
    const val BLOCK_PLACE  = 1 shl 2 // 0100
    const val INTERACT     = 1 shl 3 // 1000

    fun parse(flagName: String): Int = when (flagName.uppercase()) {
        "PVP"         -> PVP
        "BLOCK_BREAK" -> BLOCK_BREAK
        "BLOCK_PLACE" -> BLOCK_PLACE
        "INTERACT"    -> INTERACT
        else          -> NONE
    }

    fun toString(flag: Int): String = when (flag) {
        PVP          -> "PVP"
        BLOCK_BREAK  -> "BLOCK_BREAK"
        BLOCK_PLACE  -> "BLOCK_PLACE"
        INTERACT     -> "INTERACT"
        else          -> "NONE"
    }
}
