package com.github.berserkr2k.coreplugin.infrastructure.filesystem

import com.github.berserkr2k.coreplugin.api.core.filesystem.FeatureFolderProvider
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.configurate.CommentedConfigurationNode
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

class RuntimeFolderBootstrapper(
    private val dataFolder: Path,
    private val logger: Logger
) : FeatureFolderProvider {

    private val folderCache = ConcurrentHashMap<String, Path>()

    init {
        bootstrap()
    }

    override fun getFeatureFolder(featureId: String): Path {
        return folderCache.computeIfAbsent(featureId.lowercase()) { id ->
            val folder = dataFolder.resolve(id)
            if (Files.notExists(folder)) {
                Files.createDirectories(folder)
            }
            folder
        }
    }

    private fun bootstrap() {
        try {
            // 1. Crear directorios runtime
            val folders = listOf(
                "core", "core/storage",
                "spawn",
                "regions", "regions/regions",
                "economy", "economy/currencies", "economy/leaderboards",
                "kits", "kits/kits",
                "holograms", "holograms/holograms",
                "shops", "shops/shops",
                "menus", "menus/layouts",
                "utility",
                "warps", "warps/warps",
                "trails", "trails/trails"
            )

            for (f in folders) {
                val path = dataFolder.resolve(f)
                if (Files.notExists(path)) {
                    Files.createDirectories(path)
                }
                // Cachear las carpetas raíz de las features
                if (!f.contains("/")) {
                    folderCache[f] = path
                }
            }

            // 2. Ejecutar migraciones de archivos de configuración
            migrateFile("database.conf", "core/database.conf")
            migrateFile("network.conf", "core/network.conf")
            migrateFile("spawn.conf", "spawn/spawn.conf")
            migrateFile("utility.conf", "utility/utility.conf")
            migrateFile("data/database.db", "core/storage/database.db")
            migrateFile("chat.conf", "core/chat.conf")
            migrateFile("scoreboard.conf", "core/scoreboard.conf")
            migrateFile("anvil.conf", "core/anvil.conf")
            migrateFile("tablist.conf", "core/tablist.conf")
            migrateFile("editor.conf", "core/editor.conf")

            // Migrar subcarpetas y sus contenidos
            migrateDirectoryContents("holograms", "holograms/holograms")
            migrateDirectoryContents("kits", "kits/kits")
            migrateDirectoryContents("leaderboards", "economy/leaderboards")
            migrateDirectoryContents("shops/categories", "shops/shops")
            migrateDirectoryContents("warps", "warps/warps")
            migrateDirectoryContents("trails", "trails/trails")

            // 3. Separar y migrar messages.conf global
            splitGlobalMessages()

        } catch (e: Exception) {
            logger.severe("Error al inicializar la estructura de carpetas: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun migrateFile(oldRelPath: String, newRelPath: String) {
        val oldFile = dataFolder.resolve(oldRelPath)
        val newFile = dataFolder.resolve(newRelPath)

        if (Files.exists(oldFile) && Files.notExists(newFile)) {
            try {
                val parent = newFile.parent
                if (parent != null && Files.notExists(parent)) {
                    Files.createDirectories(parent)
                }
                Files.move(oldFile, newFile, StandardCopyOption.REPLACE_EXISTING)
                logger.info("Migrado archivo: $oldRelPath -> $newRelPath")
            } catch (e: Exception) {
                logger.severe("Fallo al migrar $oldRelPath: ${e.message}")
            }
        }
    }

    private fun migrateDirectoryContents(oldRelDir: String, newRelDir: String) {
        val oldDir = dataFolder.resolve(oldRelDir)
        val newDir = dataFolder.resolve(newRelDir)

        if (Files.exists(oldDir) && Files.isDirectory(oldDir)) {
            val files = oldDir.toFile().listFiles() ?: return
            for (file in files) {
                // Evitar mover el directorio destino si está dentro del origen
                if (file.isDirectory && file.name == newDir.fileName.toString()) continue
                if (file.isFile) {
                    val destFile = newDir.resolve(file.name)
                    if (Files.notExists(destFile)) {
                        try {
                            Files.move(file.toPath(), destFile, StandardCopyOption.REPLACE_EXISTING)
                            logger.info("Migrado archivo de subcarpeta: ${file.name} -> $newRelDir")
                        } catch (e: Exception) {
                            logger.severe("Fallo al migrar contenido ${file.name}: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    private fun splitGlobalMessages() {
        val globalMsgFile = dataFolder.resolve("messages.conf")
        if (Files.notExists(globalMsgFile)) return

        try {
            logger.info("Detectado messages.conf global. Iniciando división por feature...")
            val loader = HoconConfigurationLoader.builder()
                .path(globalMsgFile)
                .build()
            val root = loader.load()

            // Mapear cada sección del HOCON global a su respectivo messages.conf en la feature
            val mappings = mapOf(
                "spawn" to "spawn/messages.conf",
                "regions" to "regions/messages.conf",
                "economy" to "economy/messages.conf",
                "utility" to "utility/messages.conf",
                "shops" to "shops/messages.conf"
            )

            for ((key, targetRelPath) in mappings) {
                val section = root.node(key)
                if (!section.empty()) {
                    val targetFile = dataFolder.resolve(targetRelPath)
                    if (Files.notExists(targetFile)) {
                        val targetLoader = HoconConfigurationLoader.builder().path(targetFile).build()
                        val targetRoot = targetLoader.createNode()
                        // Copiar todos los hijos de la sección al nodo raíz del nuevo archivo
                        for (entry in section.childrenMap().entries) {
                            targetRoot.node(entry.key).raw(entry.value.raw())
                        }
                        targetLoader.save(targetRoot)
                        logger.info("Dividido y creado: $targetRelPath")
                    }
                }
            }

            // Crear core/messages.conf para chat, leaderboards y scoreboard
            val coreFile = dataFolder.resolve("core/messages.conf")
            if (Files.notExists(coreFile)) {
                val coreLoader = HoconConfigurationLoader.builder().path(coreFile).build()
                val coreRoot = coreLoader.createNode()
                val sectionsToCore = listOf("chat", "leaderboards", "scoreboard")
                for (sec in sectionsToCore) {
                    val section = root.node(sec)
                    if (!section.empty()) {
                        coreRoot.node(sec).raw(section.raw())
                    }
                }
                coreLoader.save(coreRoot)
                logger.info("Dividido y creado: core/messages.conf")
            }

            // Renombrar messages.conf viejo para backup
            val backupFile = dataFolder.resolve("messages.conf.backup")
            Files.move(globalMsgFile, backupFile, StandardCopyOption.REPLACE_EXISTING)
            logger.info("El archivo messages.conf original ha sido renombrado a messages.conf.backup")

        } catch (e: Exception) {
            logger.severe("Error al dividir messages.conf: ${e.message}")
            e.printStackTrace()
        }
    }
}
