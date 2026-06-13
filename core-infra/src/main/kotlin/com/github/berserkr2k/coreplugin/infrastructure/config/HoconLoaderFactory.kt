package com.github.berserkr2k.coreplugin.infrastructure.config

import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.util.NamingSchemes
import java.io.File

object HoconLoaderFactory {
    val mapperFactory = ObjectMapper.factoryBuilder()
        .defaultNamingScheme(NamingSchemes.PASSTHROUGH)
        .build()

    fun create(file: File): HoconConfigurationLoader {
        return HoconConfigurationLoader.builder()
            .path(file.toPath())
            .defaultOptions { options ->
                options.serializers { builder ->
                    builder.registerAnnotatedObjects(mapperFactory)
                    CoreConfigurateModule.apply(builder)
                }
            }
            .build()
    }
}
