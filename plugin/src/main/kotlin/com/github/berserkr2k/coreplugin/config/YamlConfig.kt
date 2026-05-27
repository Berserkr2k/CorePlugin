package com.github.berserkr2k.coreplugin.config

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.InputStreamReader

/**
 * Gestor modular de archivos YAML.
 * Permite instanciar múltiples archivos (ej. config.yml, messages.yml, data.yml)
 * de forma independiente y segura, con auto-actualización de llaves nuevas.
 */
class YamlConfig(
    private val plugin: JavaPlugin,
    fileName: String
) {
    // Aseguramos que el nombre siempre termine en .yml
    private val exactFileName = if (fileName.endsWith(".yml")) fileName else "$fileName.yml"
    private val file: File = File(plugin.dataFolder, exactFileName)
    private var config: FileConfiguration = YamlConfiguration()

    init {
        createFile()
        reload()
    }

    /**
     * Obtiene la configuración actual para leer o escribir datos.
     * @return La configuración en memoria.
     */
    fun get(): FileConfiguration = config

    /**
     * Guarda los cambios hechos en memoria hacia el archivo físico.
     */
    fun save() {
        try {
            config.save(file)
        } catch (e: Exception) {
            plugin.logger.severe("¡Error al guardar el archivo $exactFileName!")
            e.printStackTrace()
        }
    }

    /**
     * Recarga el archivo físico a la memoria. Ideal para comandos como /core reload.
     * Además, inyecta automáticamente nuevas llaves desde el archivo original (.jar)
     * sin borrar los valores modificados por el usuario (Fusión Inteligente).
     */
    fun reload() {
        config = YamlConfiguration.loadConfiguration(file)

        // Buscamos el archivo original dentro de nuestro .jar
        val defaultStream = plugin.getResource(exactFileName)
        if (defaultStream != null) {
            // Lo leemos usando UTF-8 para evitar problemas con acentos (ñ, á, etc.)
            val defaultConfig = YamlConfiguration.loadConfiguration(InputStreamReader(defaultStream, Charsets.UTF_8))

            // Le decimos a Bukkit: "Compara el archivo del usuario con este del JAR"
            config.setDefaults(defaultConfig)

            // Copia las llaves que falten del JAR al archivo actual (sin sobreescribir las que ya existen)
            config.options().copyDefaults(true)

            // Guardamos inmediatamente para que el archivo físico reciba las nuevas llaves
            save()
        }
    }

    /**
     * Crea el archivo físico si no existe.
     */
    private fun createFile() {
        if (!file.exists()) {
            file.parentFile.mkdirs() // Crea la carpeta del plugin si no existe

            // Si el archivo existe en resources (ej. un config.yml predeterminado), lo copia.
            // Si no (ej. un archivo de jugadores nuevo), crea uno en blanco.
            if (plugin.getResource(exactFileName) != null) {
                plugin.saveResource(exactFileName, false)
            } else {
                file.createNewFile()
            }
        }
    }
}