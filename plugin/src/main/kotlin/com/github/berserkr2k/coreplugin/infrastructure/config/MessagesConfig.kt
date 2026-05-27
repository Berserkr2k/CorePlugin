package com.github.berserkr2k.coreplugin.infrastructure.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MessagesConfig(
    val leaderboards: Map<String, String> = mapOf(
        "loading" to "<gold>Cargando datos del podio...</gold>",
        "header" to "<gold><bold>★ CLASIFICACIÓN DE <top_id> ★</bold></gold>\n",
        "row-format" to "<yellow>#<pos></yellow> <gray><player></gray> » <green>$<balance></green>"
    )
)
