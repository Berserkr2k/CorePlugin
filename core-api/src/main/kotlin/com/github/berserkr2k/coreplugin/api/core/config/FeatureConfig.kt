package com.github.berserkr2k.coreplugin.api.core.config

import java.util.concurrent.CompletableFuture

interface FeatureConfig {
    fun getString(path: String, default: String): String
    fun getInt(path: String, default: Int): Int
    fun getBoolean(path: String, default: Boolean): Boolean
    fun getStringList(path: String): List<String>

    // Soporte para guardar cambios dinámicos en caliente
    fun set(path: String, value: Any?)
    fun save(): CompletableFuture<Void>
    fun reload()
}
