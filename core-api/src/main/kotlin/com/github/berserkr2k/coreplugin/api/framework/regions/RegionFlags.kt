package com.github.berserkr2k.coreplugin.api.regions

object RegionFlags {
    const val NONE              = 0
    const val PVP               = 1 shl 0
    const val BLOCK_BREAK       = 1 shl 1
    const val BLOCK_PLACE       = 1 shl 2
    const val INTERACT          = 1 shl 3
    const val CHEST_ACCESS      = 1 shl 4
    const val ENDERCHEST_ACCESS = 1 shl 5
    const val ANVIL_USE         = 1 shl 6
    const val ENCHANTING_USE    = 1 shl 7
    const val USE_WITHOUT_BREAK    = 1 shl 8
    const val BLOCK_PHYSICS        = 1 shl 9
    const val ITEM_DROP            = 1 shl 10
    const val ITEM_PICKUP          = 1 shl 11
    const val PROJECTILE_DAMAGE    = 1 shl 12
    const val PLAYER_COLLISION     = 1 shl 13
    const val MOB_TARGETING        = 1 shl 14
    const val LIQUID_FLOW          = 1 shl 15
    const val FALL_DAMAGE          = 1 shl 16
    const val ELYTRA_USAGE         = 1 shl 17
    const val REDSTONE_INTERACTION = 1 shl 18
    const val VEHICLE_USAGE        = 1 shl 19
    const val EXP_GAIN             = 1 shl 20
    const val HUNGER_LOSS          = 1 shl 21
    const val HOSTILE_SPAWN        = 1 shl 22
    const val PASSIVE_SPAWN        = 1 shl 23
    const val ARMOR_STAND_INTERACTION = 1 shl 24
    const val ENTITY_INTERACTION = 1 shl 25
    const val CONTAINER_INTERACTION = 1 shl 26
    const val ITEM_FRAME_INTERACTION = 1 shl 27

    // Categories
    const val COMBAT_FLAGS = PVP or PROJECTILE_DAMAGE or FALL_DAMAGE
    const val WORLD_FLAGS = BLOCK_BREAK or BLOCK_PLACE or BLOCK_PHYSICS or LIQUID_FLOW
    const val INTERACTION_FLAGS = INTERACT or CHEST_ACCESS or ENDERCHEST_ACCESS or ANVIL_USE or ENCHANTING_USE or REDSTONE_INTERACTION or ARMOR_STAND_INTERACTION or ENTITY_INTERACTION or CONTAINER_INTERACTION or ITEM_FRAME_INTERACTION
    const val PLAYER_FLAGS = ITEM_DROP or ITEM_PICKUP or ELYTRA_USAGE or VEHICLE_USAGE or EXP_GAIN or HUNGER_LOSS or PLAYER_COLLISION or USE_WITHOUT_BREAK
    const val ENTITY_FLAGS = HOSTILE_SPAWN or PASSIVE_SPAWN or MOB_TARGETING

    fun parse(flagName: String): Int = when (flagName.uppercase()) {
        "PVP"               -> PVP
        "BLOCK_BREAK"       -> BLOCK_BREAK
        "BLOCK_PLACE"       -> BLOCK_PLACE
        "INTERACT"          -> INTERACT
        "CHEST_ACCESS"      -> CHEST_ACCESS
        "ENDERCHEST_ACCESS" -> ENDERCHEST_ACCESS
        "ANVIL_USE"         -> ANVIL_USE
        "ENCHANTING_USE"    -> ENCHANTING_USE
        "USE_WITHOUT_BREAK" -> USE_WITHOUT_BREAK
        "BLOCK_PHYSICS"     -> BLOCK_PHYSICS
        "ITEM_DROP"         -> ITEM_DROP
        "ITEM_PICKUP"       -> ITEM_PICKUP
        "PROJECTILE_DAMAGE" -> PROJECTILE_DAMAGE
        "PLAYER_COLLISION"  -> PLAYER_COLLISION
        "MOB_TARGETING"     -> MOB_TARGETING
        "LIQUID_FLOW"       -> LIQUID_FLOW
        "FALL_DAMAGE"       -> FALL_DAMAGE
        "ELYTRA_USAGE"      -> ELYTRA_USAGE
        "REDSTONE_INTERACTION" -> REDSTONE_INTERACTION
        "VEHICLE_USAGE"     -> VEHICLE_USAGE
        "EXP_GAIN"          -> EXP_GAIN
        "HUNGER_LOSS"       -> HUNGER_LOSS
        "HOSTILE_SPAWN"     -> HOSTILE_SPAWN
        "PASSIVE_SPAWN"     -> PASSIVE_SPAWN
        "ARMOR_STAND_INTERACTION" -> ARMOR_STAND_INTERACTION
        "ENTITY_INTERACTION" -> ENTITY_INTERACTION
        "CONTAINER_INTERACTION" -> CONTAINER_INTERACTION
        "ITEM_FRAME_INTERACTION" -> ITEM_FRAME_INTERACTION
        "COMBAT_FLAGS"      -> COMBAT_FLAGS
        "WORLD_FLAGS"       -> WORLD_FLAGS
        "INTERACTION_FLAGS" -> INTERACTION_FLAGS
        "PLAYER_FLAGS"      -> PLAYER_FLAGS
        "ENTITY_FLAGS"      -> ENTITY_FLAGS
        else                -> NONE
    }

    fun isCategory(flagName: String): Boolean = when (flagName.uppercase()) {
        "COMBAT_FLAGS", "WORLD_FLAGS", "INTERACTION_FLAGS", "PLAYER_FLAGS", "ENTITY_FLAGS" -> true
        else -> false
    }

    fun getIndividualFlags(categoryName: String): List<String> = when (categoryName.uppercase()) {
        "COMBAT_FLAGS"      -> listOf("PVP", "PROJECTILE_DAMAGE", "FALL_DAMAGE")
        "WORLD_FLAGS"       -> listOf("BLOCK_BREAK", "BLOCK_PLACE", "BLOCK_PHYSICS", "LIQUID_FLOW")
        "INTERACTION_FLAGS" -> listOf("INTERACT", "CHEST_ACCESS", "ENDERCHEST_ACCESS", "ANVIL_USE", "ENCHANTING_USE", "REDSTONE_INTERACTION", "ARMOR_STAND_INTERACTION", "ENTITY_INTERACTION", "CONTAINER_INTERACTION", "ITEM_FRAME_INTERACTION")
        "PLAYER_FLAGS"      -> listOf("ITEM_DROP", "ITEM_PICKUP", "ELYTRA_USAGE", "VEHICLE_USAGE", "EXP_GAIN", "HUNGER_LOSS", "PLAYER_COLLISION", "USE_WITHOUT_BREAK")
        "ENTITY_FLAGS"      -> listOf("HOSTILE_SPAWN", "PASSIVE_SPAWN", "MOB_TARGETING")
        else                -> emptyList()
    }

    fun getCategoryOfFlag(flag: Int): String? {
        return when {
            (flag and COMBAT_FLAGS) != 0 -> "COMBAT"
            (flag and WORLD_FLAGS) != 0 -> "WORLD"
            (flag and INTERACTION_FLAGS) != 0 -> "INTERACTION"
            (flag and PLAYER_FLAGS) != 0 -> "PLAYER"
            (flag and ENTITY_FLAGS) != 0 -> "ENTITY"
            else -> null
        }
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
        USE_WITHOUT_BREAK -> "USE_WITHOUT_BREAK"
        BLOCK_PHYSICS     -> "BLOCK_PHYSICS"
        ITEM_DROP         -> "ITEM_DROP"
        ITEM_PICKUP       -> "ITEM_PICKUP"
        PROJECTILE_DAMAGE -> "PROJECTILE_DAMAGE"
        PLAYER_COLLISION  -> "PLAYER_COLLISION"
        MOB_TARGETING     -> "MOB_TARGETING"
        LIQUID_FLOW       -> "LIQUID_FLOW"
        FALL_DAMAGE       -> "FALL_DAMAGE"
        ELYTRA_USAGE      -> "ELYTRA_USAGE"
        REDSTONE_INTERACTION -> "REDSTONE_INTERACTION"
        VEHICLE_USAGE     -> "VEHICLE_USAGE"
        EXP_GAIN          -> "EXP_GAIN"
        HUNGER_LOSS       -> "HUNGER_LOSS"
        HOSTILE_SPAWN     -> "HOSTILE_SPAWN"
        PASSIVE_SPAWN     -> "PASSIVE_SPAWN"
        ARMOR_STAND_INTERACTION -> "ARMOR_STAND_INTERACTION"
        ENTITY_INTERACTION -> "ENTITY_INTERACTION"
        CONTAINER_INTERACTION -> "CONTAINER_INTERACTION"
        ITEM_FRAME_INTERACTION -> "ITEM_FRAME_INTERACTION"
        COMBAT_FLAGS      -> "COMBAT_FLAGS"
        WORLD_FLAGS       -> "WORLD_FLAGS"
        INTERACTION_FLAGS -> "INTERACTION_FLAGS"
        PLAYER_FLAGS      -> "PLAYER_FLAGS"
        ENTITY_FLAGS      -> "ENTITY_FLAGS"
        else              -> "NONE"
    }
}
