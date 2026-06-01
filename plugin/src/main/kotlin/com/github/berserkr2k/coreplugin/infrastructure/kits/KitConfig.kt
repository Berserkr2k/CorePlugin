package com.github.berserkr2k.coreplugin.infrastructure.kits

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class KitConfig(
    val displayName: String = "<gold><bold>Kit Starter</bold></gold>",
    val permission: String = "core.kits.starter",
    val cooldownSeconds: Int = 86400,
    val cost: Double = 0.0,
    val currency: String = "credits",
    val items: List<KitItem> = listOf(
        KitItem(
            material = "DIAMOND_SWORD",
            amount = 1,
            displayName = "<gold>Espada de Iniciado</gold>",
            lore = listOf(
                "<gray>Un arma forjada para valientes.</gray>",
                "<gray>Otorga valor al portador.</gray>"
            ),
            enchantments = mapOf("SHARPNESS" to 2, "UNBREAKABLE" to 1),
            unbreakable = true,
            customModelData = 1001
        ),
        KitItem(
            material = "COOKED_BEEF",
            amount = 32
        )
    ),
    val commands: List<String> = listOf(
        "console:give %player_name% diamond 1",
        "player:say ¡He reclamado mi Kit Starter!"
    ),
    val effects: KitEffects = KitEffects(),
    val guiIcon: String = "IRON_SWORD",
    val guiSlot: Int = -1,
    val guiLore: List<String> = listOf(
        "<gray>Un kit excelente para comenzar tu</gray>",
        "<gray>aventura en el servidor.</gray>",
        "",
        "<yellow>Precio: Gratis</yellow>"
    )
) {
    @ConfigSerializable
    data class KitItem(
        val material: String = "AIR",
        val amount: Int = 1,
        val displayName: String? = null,
        val lore: List<String> = emptyList(),
        val enchantments: Map<String, Int> = emptyMap(),
        val unbreakable: Boolean = false,
        val customModelData: Int? = null
    )

    @ConfigSerializable
    data class KitEffects(
        val sound: String = "ENTITY_PLAYER_LEVELUP",
        val particle: String = "HAPPY_VILLAGER"
    )
}
