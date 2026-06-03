package com.github.berserkr2k.coreplugin.infrastructure.database

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class DatabaseConfig(
    val driver: String = "sqlite", // sqlite o postgresql
    val host: String = "localhost",
    val port: Int = 5432,
    val database: String = "core_db",
    val username: String = "postgres",
    val password: String = "secret",
    val maxPoolSize: Int = 10,
    val minIdle: Int = 2
)
