package com.github.berserkr2k.coreplugin.api.framework.menu

import com.github.berserkr2k.coreplugin.api.config.ItemConfig

data class FillerConfig(
    val enabled: Boolean = true,
    val item: ItemConfig = ItemConfig(
        material = "GRAY_STAINED_GLASS_PANE",
        displayName = " "
    )
)

data class MenuItemConfig(
    val slots: List<Int> = emptyList(),
    val item: ItemConfig = ItemConfig(),
    val action: String? = null,
    val sound: String? = null,
    val permission: String? = null
)

data class MenuConfig(
    val title: String = "<gold>Menú</gold>",
    val size: Int = 27,
    val filler: FillerConfig = FillerConfig(),
    val items: Map<String, MenuItemConfig> = emptyMap(),
    val paginated: Boolean = false,
    val dynamicSlots: List<Int> = emptyList(),
    val previousPageSlot: Int? = null,
    val nextPageSlot: Int? = null,
    val previousPageItem: ItemConfig = ItemConfig(material = "ARROW", displayName = "<yellow>Página Anterior</yellow>"),
    val nextPageItem: ItemConfig = ItemConfig(material = "ARROW", displayName = "<yellow>Siguiente Página</yellow>")
)
