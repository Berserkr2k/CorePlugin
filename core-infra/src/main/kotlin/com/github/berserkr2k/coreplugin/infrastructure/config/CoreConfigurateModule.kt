package com.github.berserkr2k.coreplugin.infrastructure.config

import com.github.berserkr2k.coreplugin.api.config.ItemConfig
import com.github.berserkr2k.coreplugin.api.framework.menu.FillerConfig
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuConfig
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuItemConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.serializer.*
import org.spongepowered.configurate.serialize.TypeSerializerCollection

object CoreConfigurateModule {
    fun apply(builder: TypeSerializerCollection.Builder) {
        builder.register(ItemConfig::class.java, ItemConfigSerializer)
        builder.register(FillerConfig::class.java, FillerConfigSerializer)
        builder.register(MenuItemConfig::class.java, MenuItemConfigSerializer)
        builder.register(MenuConfig::class.java, MenuConfigSerializer)
    }
}
