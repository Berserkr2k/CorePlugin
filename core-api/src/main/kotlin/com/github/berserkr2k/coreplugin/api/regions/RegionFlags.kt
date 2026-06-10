package com.github.berserkr2k.coreplugin.api.regions

object RegionFlags {
    const val NONE              = 0
    const val PVP               = 1 shl 0 // 00000001
    const val BLOCK_BREAK       = 1 shl 1 // 00000010
    const val BLOCK_PLACE       = 1 shl 2 // 00000100
    const val INTERACT          = 1 shl 3 // 00001000
    const val CHEST_ACCESS      = 1 shl 4 // 00010000
    const val ENDERCHEST_ACCESS = 1 shl 5 // 00100000
    const val ANVIL_USE         = 1 shl 6 // 01000000
    const val ENCHANTING_USE    = 1 shl 7 // 10000000

    fun parse(flagName: String): Int = when (flagName.uppercase()) {
        "PVP"               -> PVP
        "BLOCK_BREAK"       -> BLOCK_BREAK
        "BLOCK_PLACE"       -> BLOCK_PLACE
        "INTERACT"          -> INTERACT
        "CHEST_ACCESS"      -> CHEST_ACCESS
        "ENDERCHEST_ACCESS" -> ENDERCHEST_ACCESS
        "ANVIL_USE"         -> ANVIL_USE
        "ENCHANTING_USE"    -> ENCHANTING_USE
        else                -> NONE
    }

    fun toString(flag: Int): String = when (flag) {
        PVP               -> "PVP"
        BLOCK_BREAK       -> "BLOCK_BREAK"
        BLOCK_PLACE       -> "BLOCK_PLACE"
        INTERACT          -> "INTERACT"
        CHEST_ACCESS      -> "CHEST_ACCESS"
        ENDERCHEST_ACCESS -> "ENDERCHEST_ACCESS"
        ANVIL_USE         -> "ANVIL_USE"
        ENCHANTING_USE    -> "ENCHANTING_USE"
        else              -> "NONE"
    }
}
