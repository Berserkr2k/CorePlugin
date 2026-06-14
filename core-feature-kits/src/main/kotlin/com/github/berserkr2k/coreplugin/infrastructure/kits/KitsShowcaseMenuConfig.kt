package com.github.berserkr2k.coreplugin.infrastructure.kits

import com.github.berserkr2k.coreplugin.api.core.config.ConfigDefinition
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuConfig
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuItemConfig
import com.github.berserkr2k.coreplugin.api.config.ItemConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class KitsShowcaseMenuConfig(
    val schemaVersion: Int = 1,
    val menu: MenuConfig = MenuConfig(
        title = "<gold>Previsualizar: %kit_name%</gold>",
        size = 27,
        filler = com.github.berserkr2k.coreplugin.api.framework.menu.FillerConfig(
            enabled = true,
            item = ItemConfig(material = "GRAY_STAINED_GLASS_PANE", displayName = " ")
        ),
        items = mapOf(
            "back" to MenuItemConfig(
                slots = listOf(18),
                item = ItemConfig(material = "BARRIER", displayName = "<red>Volver al Selector</red>", lore = listOf("<gray>Regresa al selector de kits principal.</gray>")),
                action = "back"
            ),
            "claim" to MenuItemConfig(
                slots = listOf(22),
                item = ItemConfig(material = "GREEN_CONCRETE", displayName = "<green><bold>✔ ¡Reclamar Kit!</bold></green>", lore = listOf("%price_lore%%bypass_lore%<gray>Haz click aquí para recibir los items.</gray>")),
                action = "claim"
            )
        )
    ),
    
    val backName: String = "<red>Volver al Selector</red>",
    val backLore: List<String> = listOf("<gray>Regresa al selector de kits principal.</gray>"),
    
    val lockedName: String = "<red><bold>❌ Kit Bloqueado</bold></red>",
    val lockedLore: List<String> = listOf("<gray>Requiere el rango de permiso:</gray>", "<red><permission></red>"),
    
    val cooldownName: String = "<yellow><bold>⏳ En Cooldown</bold></yellow>",
    val cooldownLore: List<String> = listOf("<gray>Debes esperar:</gray>", "<yellow><time></yellow>", "<gray>para reclamar nuevamente.</gray>"),
    
    val claimName: String = "<green><bold>✔ ¡Reclamar Kit!</bold></green>",
    val claimLore: List<String> = listOf("%price_lore%%bypass_lore%<gray>Haz click aquí para recibir los items.</gray>")
)

object KitsShowcaseMenuConfigDefinition : ConfigDefinition<KitsShowcaseMenuConfig> {
    override val fileName = "menus/showcase.conf"
    override val schemaVersion = 1
    override val configType = KitsShowcaseMenuConfig::class.java
}
