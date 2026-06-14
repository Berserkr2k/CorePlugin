package com.github.berserkr2k.coreplugin.api.core.config

interface ConfigDefinition<T> {
    val fileName: String
    val schemaVersion: Int
    val configType: Class<T>
}
