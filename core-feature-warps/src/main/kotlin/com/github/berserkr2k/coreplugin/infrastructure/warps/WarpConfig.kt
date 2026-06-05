package com.github.berserkr2k.coreplugin.infrastructure.warps

import com.github.berserkr2k.coreplugin.infrastructure.config.ItemConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class WarpConfig(
    val name: String = "spawn",
    val world: String = "world",
    val x: Double = 0.0,
    val y: Double = 64.0,
    val z: Double = 0.0,
    val yaw: Float = 0.0f,
    val pitch: Float = 0.0f,
    val permission: String = "core.warps.use.spawn",
    val warmupSeconds: Int = 0,
    val cooldownSeconds: Int = 0,
    val guiSlot: Int = -1,
    val item: ItemConfig = ItemConfig(
        material = "ENDER_PEARL",
        displayName = "<green><bold>Warp spawn</bold></green>",
        lore = listOf(
            "<gray>Haz clic para viajar a este warp.</gray>",
            " ",
            "<yellow>▶ Click para viajar</yellow>"
        )
    )
) {
    val displayName: String
        get() = item.displayName ?: "<green><bold>Warp $name</bold></green>"
}
