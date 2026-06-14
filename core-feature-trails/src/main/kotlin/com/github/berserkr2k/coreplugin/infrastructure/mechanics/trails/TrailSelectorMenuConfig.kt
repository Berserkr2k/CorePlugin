package com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails

import com.github.berserkr2k.coreplugin.api.core.config.ConfigDefinition
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuConfig
import com.github.berserkr2k.coreplugin.api.config.ItemConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class TrailSelectorMenuConfig(
    val schemaVersion: Int = 1,
    val menu: MenuConfig = MenuConfig(
        title = "<gold><bold>Estelas de Proyectil</bold></gold>",
        size = 27,
        filler = com.github.berserkr2k.coreplugin.api.framework.menu.FillerConfig(
            enabled = true,
            item = ItemConfig(material = "GRAY_STAINED_GLASS_PANE", displayName = " ")
        ),
        items = mapOf(
            "clear" to com.github.berserkr2k.coreplugin.api.framework.menu.MenuItemConfig(
                slots = listOf(22),
                item = ItemConfig(
                    material = "BARRIER",
                    displayName = "<red><bold>❌ Quitar Estela</bold></red>",
                    lore = listOf(
                        "<gray>Haz click aquí para remover tu</gray>",
                        "<gray>estela de partículas activa.</gray>",
                        " ",
                        "<yellow>⚡ Click para remover</yellow>"
                    )
                ),
                action = "clear",
                sound = "BLOCK_LAVA_EXTINGUISH"
            )
        )
    )
)

object TrailSelectorMenuConfigDefinition : ConfigDefinition<TrailSelectorMenuConfig> {
    override val fileName = "menus/selector.conf"
    override val schemaVersion = 1
    override val configType = TrailSelectorMenuConfig::class.java
}
