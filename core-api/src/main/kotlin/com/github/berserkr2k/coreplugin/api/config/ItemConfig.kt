package com.github.berserkr2k.coreplugin.api.config

data class ItemConfig(
    val material: String = "AIR",
    val skullTexture: String? = null, // base64
    val skullOwner: String? = null,   // player name
    val skullUuid: String? = null,    // UUID
    val amount: Int = 1,
    val displayName: String? = null,
    val lore: List<String> = emptyList(),
    val enchantments: Map<String, Int> = emptyMap(),
    val itemFlags: List<String> = emptyList(),
    val unbreakable: Boolean = false,
    val customModelData: Int? = null,
    val glow: Boolean = false,
    val potionType: String? = null,
    val leatherColor: String? = null, // HEX #rrggbb
    val damage: Int? = null,          // durability damage
    val pdc: Map<String, String> = emptyMap() // key to value
)
