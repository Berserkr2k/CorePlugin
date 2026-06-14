package com.github.berserkr2k.coreplugin.infrastructure.warps

import com.github.berserkr2k.coreplugin.api.core.config.ConfigDefinition
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class WarpsSelectorMenuConfig(
    val schemaVersion: Int = 1,
    val menu: MenuConfig = MenuConfig(
        title = "<dark_gray>Puntos de Teletransporte</dark_gray>",
        size = 27,
        filler = com.github.berserkr2k.coreplugin.api.framework.menu.FillerConfig(
            enabled = true,
            item = com.github.berserkr2k.coreplugin.api.config.ItemConfig(material = "GRAY_STAINED_GLASS_PANE", displayName = " ")
        )
    ),
    
    val statusLocked: String = "<red>❌ Bloqueado</red>",
    val statusRequiresPermission: String = "<gray>Requiere permiso: <red><permission></red></gray>",
    val defaultDisplayName: String = "<green><bold>Warp <name></bold></green>"
)

object WarpsSelectorMenuConfigDefinition : ConfigDefinition<WarpsSelectorMenuConfig> {
    override val fileName = "menus/selector.conf"
    override val schemaVersion = 1
    override val configType = WarpsSelectorMenuConfig::class.java
}
