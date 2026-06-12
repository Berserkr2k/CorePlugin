package com.github.berserkr2k.coreplugin.api.framework.regions

data class RegionFlag(
    val name: String,
    val mask: Long,
    val displayName: String,
    val description: String,
    val categoryId: String,
    val iconMaterial: String
)

data class RegionCategory(
    val id: String,
    val displayName: String,
    val description: String,
    val iconMaterial: String
)

object RegionFlags {
    const val NONE                    = 0L
    const val BUILD                   = 1L shl 0
    const val BLOCK_BREAK             = 1L shl 1
    const val BLOCK_PLACE             = 1L shl 2
    const val USE                     = 1L shl 3
    const val INTERACT                = 1L shl 4
    const val PVP                     = 1L shl 5
    const val MOB_SPAWNING            = 1L shl 6
    const val MOB_DAMAGE              = 1L shl 7
    const val DAMAGE_ANIMALS          = 1L shl 8
    const val INVINCIBLE              = 1L shl 9
    const val FIRE_SPREAD             = 1L shl 10
    const val LAVA_FIRE               = 1L shl 11
    const val WATER_FLOW              = 1L shl 12
    const val LAVA_FLOW               = 1L shl 13
    const val SNOW_FALL               = 1L shl 14
    const val SNOW_MELT               = 1L shl 15
    const val ICE_MELT                = 1L shl 16
    const val ICE_FORM                = 1L shl 17
    const val LEAF_DECAY              = 1L shl 18
    const val GRASS_GROWTH            = 1L shl 19
    const val MYCELIUM_SPREAD         = 1L shl 20
    const val MUSHROOM_GROWTH         = 1L shl 21
    const val ENDERPEARL              = 1L shl 22
    const val CHORUS_FRUIT_TELEPORT   = 1L shl 23
    const val SLEEP                   = 1L shl 24
    const val VEHICLE_PLACE           = 1L shl 25
    const val VEHICLE_DESTROY         = 1L shl 26
    const val TNT                     = 1L shl 27
    const val OTHER_EXPLOSION         = 1L shl 28
    const val CREEPER_EXPLOSION       = 1L shl 29
    const val GHAST_FIREBALL          = 1L shl 30
    const val ENDERMAN_GRIEF          = 1L shl 31
    const val FALL_DAMAGE             = 1L shl 32
    const val FIREWORK_DAMAGE         = 1L shl 33
    const val RESPAWN_ANCHORS         = 1L shl 34
    const val USE_WITHOUT_BREAK       = 1L shl 35
    const val PLAYER_COLLISION        = 1L shl 36

    val ALL_FLAGS = listOf(
        RegionFlag("build", BUILD, "Construcción", "Controla si se pueden colocar o romper bloques en general.", "BUILDING", "GRASS_BLOCK"),
        RegionFlag("block-break", BLOCK_BREAK, "Romper Bloques", "Controla específicamente si se pueden romper bloques.", "BUILDING", "DIAMOND_PICKAXE"),
        RegionFlag("block-place", BLOCK_PLACE, "Colocar Bloques", "Controla específicamente si se pueden colocar bloques.", "BUILDING", "BRICKS"),
        RegionFlag("enderman-grief", ENDERMAN_GRIEF, "Grief de Enderman", "Evita o permite que los enderman muevan bloques.", "BUILDING", "ENDER_EYE"),
        RegionFlag("leaf-decay", LEAF_DECAY, "Caída de Hojas", "Controla si las hojas de los árboles se degradan.", "BUILDING", "OAK_LEAVES"),

        RegionFlag("use", USE, "Usar Bloques", "Interacción con puertas, botones, palancas, cofres, etc.", "INTERACTIONS", "OAK_DOOR"),
        RegionFlag("interact", INTERACT, "Interactuar", "Interacción con entidades, aldeanos, marcos de ítems, etc.", "INTERACTIONS", "VILLAGER_SPAWN_EGG"),
        RegionFlag("sleep", SLEEP, "Dormir", "Permite o bloquea el uso de camas.", "INTERACTIONS", "RED_BED"),
        RegionFlag("respawn-anchors", RESPAWN_ANCHORS, "Anclas de Reaparición", "Controla el uso e interacción con anclas de reaparición.", "INTERACTIONS", "RESPAWN_ANCHOR"),
        RegionFlag("vehicle-place", VEHICLE_PLACE, "Colocar Vehículos", "Colocación de barcos, vagonetas, etc.", "INTERACTIONS", "MINECART"),
        RegionFlag("vehicle-destroy", VEHICLE_DESTROY, "Destruir Vehículos", "Destrucción de barcos, vagonetas, etc.", "INTERACTIONS", "SADDLE"),

        RegionFlag("pvp", PVP, "PVP", "Habilita o deshabilita el combate entre jugadores.", "COMBAT", "DIAMOND_SWORD"),
        RegionFlag("invincible", INVINCIBLE, "Invencibilidad", "Hace invencibles a los jugadores dentro de la región.", "COMBAT", "GOLDEN_APPLE"),
        RegionFlag("fall-damage", FALL_DAMAGE, "Daño por Caída", "Evita o permite el daño al caer.", "COMBAT", "FEATHER"),
        RegionFlag("firework-damage", FIREWORK_DAMAGE, "Daño de Cohetes", "Evita o permite daño por fuegos artificiales.", "COMBAT", "FIREWORK_STAR"),
        RegionFlag("mob-damage", MOB_DAMAGE, "Daño a Mobs", "Define si los monstruos/mobs pueden recibir daño.", "COMBAT", "ROTTEN_FLESH"),
        RegionFlag("damage-animals", DAMAGE_ANIMALS, "Daño a Animales", "Define si los animales pueden recibir daño.", "COMBAT", "COOKED_BEEF"),
        RegionFlag("use-without-break", USE_WITHOUT_BREAK, "Usar sin Desgaste", "Evita el desgaste de durabilidad de herramientas/armaduras.", "COMBAT", "ANVIL"),

        RegionFlag("fire-spread", FIRE_SPREAD, "Propagación de Fuego", "Controla si el fuego se propaga por bloques.", "ENVIRONMENT", "FLINT_AND_STEEL"),
        RegionFlag("lava-fire", LAVA_FIRE, "Fuego por Lava", "Permite o bloquea que la lava encienda bloques adyacentes.", "ENVIRONMENT", "LAVA_BUCKET"),
        RegionFlag("water-flow", WATER_FLOW, "Flujo de Agua", "Controla si el agua puede fluir libremente.", "ENVIRONMENT", "WATER_BUCKET"),
        RegionFlag("lava-flow", LAVA_FLOW, "Flujo de Lava", "Controla si la lava puede fluir libremente.", "ENVIRONMENT", "MAGMA_BLOCK"),
        RegionFlag("snow-fall", SNOW_FALL, "Acumulación de Nieve", "Controla la formación de nieve al caer.", "ENVIRONMENT", "SNOWBALL"),
        RegionFlag("snow-melt", SNOW_MELT, "Derretimiento de Nieve", "Controla si la nieve se derrite por calor.", "ENVIRONMENT", "FIRE_CHARGE"),
        RegionFlag("ice-melt", ICE_MELT, "Derretimiento de Hielo", "Controla si el hielo se derrite por calor.", "ENVIRONMENT", "BLUE_ICE"),
        RegionFlag("ice-form", ICE_FORM, "Formación de Hielo", "Controla la formación de hielo en el agua.", "ENVIRONMENT", "PACKED_ICE"),
        RegionFlag("grass-growth", GRASS_GROWTH, "Crecimiento de Césped", "Controla si el césped crece o se propaga.", "ENVIRONMENT", "SHORT_GRASS"),
        RegionFlag("mycelium-spread", MYCELIUM_SPREAD, "Propagación de Micelio", "Controla si el micelio se propaga.", "ENVIRONMENT", "MYCELIUM"),
        RegionFlag("mushroom-growth", MUSHROOM_GROWTH, "Crecimiento de Hongos", "Controla el crecimiento de setas/hongos.", "ENVIRONMENT", "RED_MUSHROOM"),

        RegionFlag("mob-spawning", MOB_SPAWNING, "Aparición de Mobs", "Controla la aparición natural de monstruos o animales.", "ENTITIES", "ZOMBIE_HEAD"),
        RegionFlag("tnt", TNT, "TNT", "Evita o permite explosiones de TNT.", "ENTITIES", "TNT"),
        RegionFlag("creeper-explosion", CREEPER_EXPLOSION, "Explosión de Creeper", "Evita o permite daño de explosión de creepers.", "ENTITIES", "CREEPER_HEAD"),
        RegionFlag("ghast-fireball", GHAST_FIREBALL, "Bolas de Ghast", "Daño de explosiones de bolas de fuego de Ghast.", "ENTITIES", "FIRE_CHARGE"),
        RegionFlag("other-explosion", OTHER_EXPLOSION, "Otras Explosiones", "Controla explosiones de camas, cristales de end, etc.", "ENTITIES", "FIREWORK_ROCKET"),

        RegionFlag("enderpearl", ENDERPEARL, "Teletransporte de Perla", "Permite o bloquea el uso de perlas de ender.", "MOVEMENT", "ENDER_PEARL"),
        RegionFlag("chorus-fruit-teleport", CHORUS_FRUIT_TELEPORT, "Teletransporte de Coro", "Permite o bloquea teletransportarse al comer fruta de coro.", "MOVEMENT", "CHORUS_FRUIT"),
        RegionFlag("player-collision", PLAYER_COLLISION, "Colisión de Jugadores", "Define si los jugadores pueden empujarse o atravesarse.", "MOVEMENT", "SHIELD")
    )

    val CATEGORIES = listOf(
        RegionCategory("BUILDING", "Construcción", "Controla colocación, destrucción y modificación de bloques.", "BRICKS"),
        RegionCategory("INTERACTIONS", "Interacciones", "Puertas, camas, cofres, vehículos e interacciones con entidades.", "CHEST"),
        RegionCategory("COMBAT", "Combate y Daño", "Controla PVP, daño por caída, invencibilidad y daño a mobs/animales.", "DIAMOND_SWORD"),
        RegionCategory("ENVIRONMENT", "Naturaleza", "Flujo de líquidos, fuego, derretimiento de hielo/nieve y crecimiento.", "WATER_BUCKET"),
        RegionCategory("ENTITIES", "Entidades y Explosiones", "Generación de mobs y detonaciones de creepers, TNT, etc.", "TNT"),
        RegionCategory("MOVEMENT", "Movimiento", "Teletransportación usando perlas de ender o fruta de coro.", "ENDER_PEARL")
    )

    private val nameToFlag = ALL_FLAGS.associateBy { it.name.uppercase() }

    fun parse(flagName: String): Long {
        val nameStandardized = flagName.replace("_", "-").uppercase()
        return nameToFlag[nameStandardized]?.mask ?: NONE
    }

    fun getFlagByMask(mask: Long): RegionFlag? {
        return ALL_FLAGS.firstOrNull { it.mask == mask }
    }

    fun getFlagByName(name: String): RegionFlag? {
        val nameStandardized = name.replace("_", "-").uppercase()
        return nameToFlag[nameStandardized]
    }

    fun isCategory(categoryName: String): Boolean {
        return CATEGORIES.any { it.id.equals(categoryName, ignoreCase = true) }
    }

    fun getIndividualFlags(categoryName: String): List<String> {
        return ALL_FLAGS.filter { it.categoryId.equals(categoryName, ignoreCase = true) }.map { it.name }
    }

    fun getCategoryOfFlag(flagMask: Long): String? {
        val flag = getFlagByMask(flagMask) ?: return null
        return flag.categoryId
    }

    fun toString(flagMask: Long): String {
        return getFlagByMask(flagMask)?.name ?: "NONE"
    }
}
