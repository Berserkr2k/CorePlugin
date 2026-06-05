package com.github.berserkr2k.coreplugin.infrastructure.kits

import com.github.berserkr2k.coreplugin.infrastructure.config.ItemConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class KitConfig(
    val permission: String = "core.kits.starter",
    val cooldownSeconds: Int = 86400,
    val cost: Double = 0.0,
    val currency: String = "credits",
    val items: List<ItemConfig> = listOf(
        ItemConfig(
            material = "DIAMOND_SWORD",
            amount = 1,
            displayName = "<gold>Espada de Iniciado</gold>",
            lore = listOf(
                "<gray>Un arma forjada para valientes.</gray>",
                "<gray>Otorga valor al portador.</gray>"
            ),
            enchantments = mapOf("SHARPNESS" to 2, "UNBREAKING" to 1),
            unbreakable = true,
            customModelData = 1001
        ),
        ItemConfig(
            material = "COOKED_BEEF",
            amount = 32
        )
    ),
    val commands: List<String> = listOf(
        "console:give %player_name% diamond 1",
        "player:say ¡He reclamado mi Kit Starter!"
    ),
    val effects: KitEffects = KitEffects(),
    val guiSlot: Int = -1,
    val item: ItemConfig = ItemConfig(
        material = "IRON_SWORD",
        displayName = "<gold><bold>Kit Starter</bold></gold>",
        lore = listOf(
            "<gray>Un kit excelente para comenzar tu</gray>",
            "<gray>aventura en el servidor.</gray>",
            "",
            "<yellow>Precio: Gratis</yellow>"
        )
    )
) {
    val displayName: String
        get() = item.displayName ?: "Kit"

    @ConfigSerializable
    data class KitEffects(
        val sound: String = "ENTITY_PLAYER_LEVELUP",
        val particle: String = "HAPPY_VILLAGER"
    )
}
