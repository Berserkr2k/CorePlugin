package com.github.berserkr2k.coreplugin.infrastructure.config.serializer

import com.github.berserkr2k.coreplugin.api.framework.menu.FillerConfig
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuConfig
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuItemConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.dto.FillerConfigNode
import com.github.berserkr2k.coreplugin.infrastructure.config.dto.MenuConfigNode
import com.github.berserkr2k.coreplugin.infrastructure.config.dto.MenuItemConfigNode
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

object FillerConfigSerializer : TypeSerializer<FillerConfig> {
    override fun deserialize(type: Type, node: ConfigurationNode): FillerConfig {
        val nodeConfig = node.get(FillerConfigNode::class.java) ?: FillerConfigNode()
        return nodeConfig.toModel()
    }

    override fun serialize(type: Type, obj: FillerConfig?, node: ConfigurationNode) {
        if (obj == null) {
            node.raw(null)
            return
        }
        node.set(FillerConfigNode::class.java, FillerConfigNode.fromModel(obj))
    }
}

object MenuItemConfigSerializer : TypeSerializer<MenuItemConfig> {
    override fun deserialize(type: Type, node: ConfigurationNode): MenuItemConfig {
        val nodeConfig = node.get(MenuItemConfigNode::class.java) ?: MenuItemConfigNode()
        return nodeConfig.toModel()
    }

    override fun serialize(type: Type, obj: MenuItemConfig?, node: ConfigurationNode) {
        if (obj == null) {
            node.raw(null)
            return
        }
        node.set(MenuItemConfigNode::class.java, MenuItemConfigNode.fromModel(obj))
    }
}

object MenuConfigSerializer : TypeSerializer<MenuConfig> {
    override fun deserialize(type: Type, node: ConfigurationNode): MenuConfig {
        val nodeConfig = node.get(MenuConfigNode::class.java) ?: MenuConfigNode()
        return nodeConfig.toModel()
    }

    override fun serialize(type: Type, obj: MenuConfig?, node: ConfigurationNode) {
        if (obj == null) {
            node.raw(null)
            return
        }
        node.set(MenuConfigNode::class.java, MenuConfigNode.fromModel(obj))
    }
}
