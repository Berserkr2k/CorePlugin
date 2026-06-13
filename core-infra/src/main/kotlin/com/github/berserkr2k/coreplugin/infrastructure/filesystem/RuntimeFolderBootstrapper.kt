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
            val folder = dataFolder.resolve("features").resolve(id)
            if (Files.notExists(folder)) {
                Files.createDirectories(folder)
            }
            folder
        }
    }

    override fun getFeatureConfigFolder(featureId: String): Path {
        val path = getFeatureFolder(featureId).resolve("config")
        if (Files.notExists(path)) {
            Files.createDirectories(path)
        }
        return path
    }

    override fun getFeatureDataFolder(featureId: String): Path {
        val path = getFeatureFolder(featureId).resolve("data")
        if (Files.notExists(path)) {
            Files.createDirectories(path)
        }
        return path
    }

    private fun bootstrap() {
        try {
            // 1. Crear directorios runtime oficiales
            val folders = listOf(
                "config",
                "features",
                "data",
                "data/database",
                "data/regions",
                "data/cache",
                "data/backups",
                "logs"
            )

            for (f in folders) {
                val path = dataFolder.resolve(f)
                if (Files.notExists(path)) {
                    Files.createDirectories(path)
                }
            }

            // 2. Ejecutar migraciones de carpetas y archivos antiguos
            // Migrar carpetas de features antiguas
            migrateFolder("warps", "features/warps/data")
            migrateFolder("kits", "features/kits/data")
            migrateFolder("trails", "features/trails/data")
            migrateFolder("holograms", "features/holograms/data")
            migrateFolder("shops/shops", "features/shop/data")
            migrateFolder("shops/categories", "features/shop/data")
            migrateFolder("shops", "features/shop/data")
            migrateFolder("regions/regions", "data/regions")
            migrateFolder("regions", "data/regions")

            // Migrar archivos sueltos a config/
            migrateFile("database.conf", "config/database.conf")
            migrateFile("core/database.conf", "config/database.conf")
            migrateFile("network.conf", "config/network.conf")
            migrateFile("core/network.conf", "config/network.conf")
            migrateFile("anvil.conf", "config/anvil.conf")
            migrateFile("core/anvil.conf", "config/anvil.conf")
            migrateFile("tablist.conf", "config/tablist.conf")
            migrateFile("core/tablist.conf", "config/tablist.conf")
            migrateFile("regions.conf", "config/regions.conf")
            migrateFile("core/messages.conf", "config/messages.conf")

            // Migrar configs de features a sus respectivas carpetas config/
            migrateFile("spawn.conf", "features/spawn/config/config.conf")
            migrateFile("spawn/spawn.conf", "features/spawn/config/config.conf")
            migrateFile("utility.conf", "features/utility/config/config.conf")
            migrateFile("utility/utility.conf", "features/utility/config/config.conf")
            migrateFile("chat.conf", "features/chat/config/config.conf")
            migrateFile("core/chat.conf", "features/chat/config/config.conf")
            migrateFile("scoreboard.conf", "features/scoreboard/config/config.conf")
            migrateFile("core/scoreboard.conf", "features/scoreboard/config/config.conf")
            migrateFile("core/editor.conf", "features/leaderboard/config/editor.conf")
            migrateFile("editor.conf", "features/leaderboard/config/editor.conf")
            
            // Migrar selectores/menus a las carpetas config/ de la feature correspondiente
            migrateFile("menus/armorstand-editor.conf", "features/leaderboard/config/armorstand-editor.conf")
            migrateFile("menus/warps-selector.conf", "features/warps/config/warps-selector.conf")
            migrateFile("menus/kits-selector.conf", "features/kits/config/kits-selector.conf")
            migrateFile("menus/kits-showcase.conf", "features/kits/config/kits-showcase.conf")

            // Migrar base de datos SQLite
            migrateFile("data/database.db", "data/database/database.db")
            migrateFile("core/storage/database.db", "data/database/database.db")

            // 3. Separar y migrar messages.conf global
            splitGlobalMessages()

            // 4. Ejecutar auditoría de almacenamiento
            runStorageAudit()

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

    private fun migrateFolder(oldRelPath: String, newRelPath: String) {
        val oldDir = dataFolder.resolve(oldRelPath)
        val newDir = dataFolder.resolve(newRelPath)

        if (Files.exists(oldDir) && Files.isDirectory(oldDir)) {
            try {
                if (Files.notExists(newDir)) {
                    Files.createDirectories(newDir.parent)
                    Files.move(oldDir, newDir, StandardCopyOption.REPLACE_EXISTING)
                    logger.info("Migrada carpeta: $oldRelPath -> $newRelPath")
                } else {
                    // Mover archivos individuales de la carpeta antigua a la nueva
                    val files = oldDir.toFile().listFiles()
                    if (files != null) {
                        for (file in files) {
                            val targetFile = newDir.resolve(file.name)
                            if (Files.notExists(targetFile)) {
                                Files.createDirectories(targetFile.parent)
                                Files.move(file.toPath(), targetFile, StandardCopyOption.REPLACE_EXISTING)
                            }
                        }
                    }
                    // Eliminar la carpeta original si está vacía
                    if (oldDir.toFile().listFiles()?.isEmpty() == true) {
                        Files.delete(oldDir)
                    }
                }
            } catch (e: Exception) {
                logger.severe("Fallo al migrar carpeta $oldRelPath: ${e.message}")
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

            val mappings = mapOf(
                "spawn" to "features/spawn/config/messages.conf",
                "regions" to "features/regions/config/messages.conf",
                "economy" to "features/economy/config/messages.conf",
                "utility" to "features/utility/config/messages.conf",
                "shops" to "features/shop/config/messages.conf"
            )

            for ((key, targetRelPath) in mappings) {
                val section = root.node(key)
                if (!section.empty()) {
                    val targetFile = dataFolder.resolve(targetRelPath)
                    if (Files.notExists(targetFile)) {
                        Files.createDirectories(targetFile.parent)
                        val targetLoader = HoconConfigurationLoader.builder().path(targetFile).build()
                        val targetRoot = targetLoader.createNode()
                        for (entry in section.childrenMap().entries) {
                            targetRoot.node(entry.key).raw(entry.value.raw())
                        }
                        targetLoader.save(targetRoot)
                        logger.info("Dividido y creado: $targetRelPath")
                    }
                }
            }

            // Crear core/messages.conf para chat, leaderboards y scoreboard
            val coreFile = dataFolder.resolve("features/core/config/messages.conf")
            if (Files.notExists(coreFile)) {
                Files.createDirectories(coreFile.parent)
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
                logger.info("Dividido y creado: features/core/config/messages.conf")
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

    private fun runStorageAudit() {
        val root = dataFolder.toFile()
        if (!root.exists() || !root.isDirectory) return

        val knownLegacy = setOf("warps", "kits", "trails", "holograms", "regions", "menus", "core", "spawn", "utility", "shops")
        val allowedRootDirs = setOf("config", "features", "data", "logs")

        val files = root.listFiles() ?: return
        val legacyFound = mutableListOf<String>()
        val unknownFound = mutableListOf<String>()

        for (file in files) {
            if (file.isDirectory) {
                val name = file.name.lowercase()
                if (knownLegacy.contains(name)) {
                    legacyFound.add(file.name)
                } else if (!allowedRootDirs.contains(name)) {
                    unknownFound.add(file.name)
                }
            } else {
                val name = file.name.lowercase()
                val legacyFiles = setOf("database.conf", "network.conf", "anvil.conf", "tablist.conf", "messages.conf", "chat.conf", "scoreboard.conf", "spawn.conf", "utility.conf")
                if (legacyFiles.contains(name)) {
                    legacyFound.add(file.name)
                }
            }
        }

        if (legacyFound.isNotEmpty()) {
            logger.severe("""
                ======================================================
                ❌ ERROR: ERROR DE MIGRACIÓN / CARPETAS LEGACY DETECTADAS
                Las siguientes rutas/archivos antiguos no se pudieron migrar:
                ${legacyFound.joinToString(separator = "\n") { "  - $it" }}
                Por favor, detén el servidor y muévelos manualmente a la nueva estructura.
                ======================================================
            """.trimIndent())
        }

        if (unknownFound.isNotEmpty()) {
            logger.warning("""
                ======================================================
                ⚠️ WARNING: CARPETAS NO RECONOCIDAS DETECTADAS
                Se encontraron directorios desconocidos en el directorio raíz del plugin:
                ${unknownFound.joinToString(separator = "\n") { "  - $it" }}
                Considera limpiarlos o moverlos si no son necesarios.
                ======================================================
            """.trimIndent())
        }
    }
}
