package com.github.berserkr2k.coreplugin.infrastructure.message

import com.github.berserkr2k.coreplugin.api.core.filesystem.FeatureFolderProvider
import com.github.berserkr2k.coreplugin.api.core.message.MessageKey
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class FeatureMessageRegistry(
    private val folderProvider: FeatureFolderProvider
) : MessageService {

    private val featureMessages = ConcurrentHashMap<String, Map<String, String>>()
    private val componentCache = ConcurrentHashMap<String, Component>()
    private val miniMessage = MiniMessage.miniMessage()

    fun getRegisteredFeatures(): Set<String> = featureMessages.keys
    
    fun getMessageCount(feature: String): Int = featureMessages[feature.lowercase()]?.size ?: 0
    
    fun getCachedComponentsCount(): Int = componentCache.size

    /**
     * Registra y carga el archivo de mensajes para una feature específica.
     */
    fun registerFeature(featureId: String, defaultMessages: Map<String, String> = emptyMap()) {
        val folder = folderProvider.getFeatureFolder(featureId)
        val file = folder.resolve("messages.conf")

        // Asegurar que el archivo exista e inyectar valores por defecto si está vacío
        if (Files.notExists(file)) {
            Files.createFile(file)
            val loader = HoconConfigurationLoader.builder().path(file).build()
            val root = loader.createNode()
            for ((k, v) in defaultMessages) {
                setNestedNodeValue(root, k, v)
            }
            loader.save(root)
        }

        loadMessagesOfFeature(featureId, file)
    }

    private fun loadMessagesOfFeature(featureId: String, file: Path) {
        try {
            val loader = HoconConfigurationLoader.builder().path(file).build()
            val root = loader.load()
            val flatMap = ConcurrentHashMap<String, String>()
            flattenNode(root, "", flatMap)
            featureMessages[featureId.lowercase()] = flatMap
            // Limpiar caché de componentes de esta feature
            componentCache.keys.removeIf { it.startsWith("${featureId.lowercase()}:") }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun send(audience: Audience, key: MessageKey, placeholders: PlaceholderContext) {
        val template = getRawTemplate(key)
        if (template.isEmpty()) return

        // Si no hay variables de contexto dinámicas, resolver y cachear el Component de Adventure
        val component = if (placeholders.resolver == net.kyori.adventure.text.minimessage.tag.resolver.TagResolver.empty()) {
            val cacheKey = "${key.feature.lowercase()}:${key.path.lowercase()}"
            componentCache.computeIfAbsent(cacheKey) { _ ->
                miniMessage.deserialize(template)
            }
        } else {
            miniMessage.deserialize(template, placeholders.resolver)
        }

        audience.sendMessage(component)
    }

    override fun getRawTemplate(key: MessageKey): String {
        val featureMap = featureMessages[key.feature.lowercase()] ?: return ""
        return featureMap[key.path] ?: ""
    }

    private fun flattenNode(node: ConfigurationNode, path: String, targetMap: MutableMap<String, String>) {
        if (node.isMap) {
            for (entry in node.childrenMap().entries) {
                val childKey = entry.key.toString()
                val childPath = if (path.isEmpty()) childKey else "$path.$childKey"
                flattenNode(entry.value, childPath, targetMap)
            }
        } else {
            val value = node.string
            if (value != null) {
                targetMap[path] = value
            }
        }
    }

    private fun setNestedNodeValue(node: ConfigurationNode, path: String, value: Any) {
        val parts = path.split(".")
        var current = node
        for (part in parts) {
            current = current.node(part)
        }
        current.raw(value)
    }
}
