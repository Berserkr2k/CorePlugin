package com.github.berserkr2k.coreplugin.infrastructure.chat

import com.github.berserkr2k.coreplugin.common.gui.FillerConfig
import com.github.berserkr2k.coreplugin.common.gui.ItemConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ColorOptionConfig(
    val id: String = "",
    val format: String = "",
    val material: String = "STONE",
    val displayName: String = "",
    val slot: Int = 0
)

@ConfigSerializable
data class ColorMenuConfig(
    val title: String = "<dark_gray><bold>Selecciona un Color</bold></dark_gray>",
    val size: Int = 27,
    val filler: FillerConfig = FillerConfig(
        enabled = true,
        item = ItemConfig(material = "GRAY_STAINED_GLASS_PANE", displayName = " ")
    ),
    val colors: List<ColorOptionConfig> = listOf(
        ColorOptionConfig("black", "<black>", "BLACK_CONCRETE", "<black>Negro</black>", 1),
        ColorOptionConfig("dark_blue", "<dark_blue>", "BLUE_CONCRETE", "<dark_blue>Azul Oscuro</dark_blue>", 2),
        ColorOptionConfig("dark_green", "<dark_green>", "GREEN_CONCRETE", "<dark_green>Verde Oscuro</dark_green>", 3),
        ColorOptionConfig("dark_aqua", "<dark_aqua>", "CYAN_CONCRETE", "<dark_aqua>Cian Oscuro</dark_aqua>", 4),
        ColorOptionConfig("dark_red", "<dark_red>", "RED_CONCRETE", "<dark_red>Rojo Oscuro</dark_red>", 5),
        ColorOptionConfig("dark_purple", "<dark_purple>", "PURPLE_CONCRETE", "<dark_purple>Púrpura Oscuro</dark_purple>", 6),
        ColorOptionConfig("gold", "<gold>", "ORANGE_CONCRETE", "<gold>Dorado</gold>", 7),
        ColorOptionConfig("gray", "<gray>", "LIGHT_GRAY_CONCRETE", "<gray>Gris</gray>", 10),
        ColorOptionConfig("dark_gray", "<dark_gray>", "GRAY_CONCRETE", "<dark_gray>Gris Oscuro</dark_gray>", 11),
        ColorOptionConfig("blue", "<blue>", "LIGHT_BLUE_CONCRETE", "<blue>Azul</blue>", 12),
        ColorOptionConfig("green", "<green>", "LIME_CONCRETE", "<green>Verde</green>", 13),
        ColorOptionConfig("aqua", "<aqua>", "LIGHT_BLUE_WOOL", "<aqua>Cian</aqua>", 14),
        ColorOptionConfig("red", "<red>", "RED_WOOL", "<red>Rojo</red>", 15),
        ColorOptionConfig("light_purple", "<light_purple>", "PINK_CONCRETE", "<light_purple>Rosado / Púrpura Claro</light_purple>", 16),
        ColorOptionConfig("yellow", "<yellow>", "YELLOW_CONCRETE", "<yellow>Amarillo</yellow>", 20),
        ColorOptionConfig("white", "<white>", "WHITE_CONCRETE", "<white>Blanco</white>", 24)
    ),
    val resetItemSlot: Int = 22,
    val resetItem: ItemConfig = ItemConfig(
        material = "BARRIER",
        displayName = "<red><bold>❌ Restablecer Color</bold></red>",
        lore = listOf(
            " ",
            "<gray>Haz click aquí para restablecer</gray>",
            "<gray>tu color de nombre por defecto.</gray>",
            " ",
            "<yellow>⚡ Click para restablecer</yellow>"
        )
    )
)
