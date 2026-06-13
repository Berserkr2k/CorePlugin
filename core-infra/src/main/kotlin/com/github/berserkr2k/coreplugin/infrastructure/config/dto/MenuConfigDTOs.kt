package com.github.berserkr2k.coreplugin.infrastructure.config.dto

import com.github.berserkr2k.coreplugin.api.config.ItemConfig
import com.github.berserkr2k.coreplugin.api.framework.menu.FillerConfig
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuConfig
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuItemConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class FillerConfigNode(
    val enabled: Boolean = true,
    val item: ItemConfig = ItemConfig(
        material = "GRAY_STAINED_GLASS_PANE",
        displayName = " "
    )
) {
    fun toModel(): FillerConfig {
        return FillerConfig(enabled = enabled, item = item)
    }

    companion object {
        fun fromModel(model: FillerConfig): FillerConfigNode {
            return FillerConfigNode(enabled = model.enabled, item = model.item)
        }
    }
}

@ConfigSerializable
data class MenuItemConfigNode(
    val slots: List<Int> = emptyList(),
    val item: ItemConfig = ItemConfig(),
    val action: String? = null,
    val sound: String? = null,
    val permission: String? = null
) {
    fun toModel(): MenuItemConfig {
        return MenuItemConfig(
            slots = slots,
            item = item,
            action = action,
            sound = sound,
            permission = permission
        )
    }

    companion object {
        fun fromModel(model: MenuItemConfig): MenuItemConfigNode {
            return MenuItemConfigNode(
                slots = model.slots,
                item = model.item,
                action = model.action,
                sound = model.sound,
                permission = model.permission
            )
        }
    }
}

@ConfigSerializable
data class MenuConfigNode(
    val title: String = "<gold>Menú</gold>",
    val size: Int = 27,
    val filler: FillerConfigNode = FillerConfigNode(),
    val items: Map<String, MenuItemConfigNode> = emptyMap(),
    val paginated: Boolean = false,
    val dynamicSlots: List<Int> = emptyList(),
    val previousPageSlot: Int? = null,
    val nextPageSlot: Int? = null,
    val previousPageItem: ItemConfig = ItemConfig(material = "ARROW", displayName = "<yellow>Página Anterior</yellow>"),
    val nextPageItem: ItemConfig = ItemConfig(material = "ARROW", displayName = "<yellow>Siguiente Página</yellow>")
) {
    fun toModel(): MenuConfig {
        return MenuConfig(
            title = title,
            size = size,
            filler = filler.toModel(),
            items = items.mapValues { it.value.toModel() },
            paginated = paginated,
            dynamicSlots = dynamicSlots,
            previousPageSlot = previousPageSlot,
            nextPageSlot = nextPageSlot,
            previousPageItem = previousPageItem,
            nextPageItem = nextPageItem
        )
    }

    companion object {
        fun fromModel(model: MenuConfig): MenuConfigNode {
            return MenuConfigNode(
                title = model.title,
                size = model.size,
                filler = FillerConfigNode.fromModel(model.filler),
                items = model.items.mapValues { MenuItemConfigNode.fromModel(it.value) },
                paginated = model.paginated,
                dynamicSlots = model.dynamicSlots,
                previousPageSlot = model.previousPageSlot,
                nextPageSlot = model.nextPageSlot,
                previousPageItem = model.previousPageItem,
                nextPageItem = model.nextPageItem
            )
        }
    }
}
