package com.github.berserkr2k.coreplugin.infrastructure.config

import com.github.berserkr2k.coreplugin.api.core.config.FeatureConfig
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

class HoconFeatureConfig(
    private val file: Path,
    private val executor: Executor
) : FeatureConfig {
    private val loader = HoconConfigurationLoader.builder()
        .path(file)
        .build()
    private lateinit var rootNode: CommentedConfigurationNode

    init {
        reload()
    }

    override fun getString(path: String, default: String): String {
        return getNode(path).getString(default)
    }

    override fun getInt(path: String, default: Int): Int {
        return getNode(path).getInt(default)
    }

    override fun getBoolean(path: String, default: Boolean): Boolean {
        return getNode(path).getBoolean(default)
    }

    override fun getStringList(path: String): List<String> {
        return getNode(path).getList(String::class.java) ?: emptyList()
    }

    override fun set(path: String, value: Any?) {
        getNode(path).set(value)
    }

    override fun save(): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            loader.save(rootNode)
        }, executor)
    }

    override fun reload() {
        rootNode = loader.load()
    }

    private fun getNode(path: String): CommentedConfigurationNode {
        if (path.isEmpty()) return rootNode
        val parts = path.split('.')
        return rootNode.node(*parts.toTypedArray())
    }
}
