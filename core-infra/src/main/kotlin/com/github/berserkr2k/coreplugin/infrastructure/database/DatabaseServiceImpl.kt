package com.github.berserkr2k.coreplugin.infrastructure.database

import com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler
import com.github.berserkr2k.coreplugin.api.core.database.FeatureDatabase
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.plugin.Plugin
import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

class DatabaseServiceImpl(
    private val plugin: Plugin,
    private val configManager: ModularConfigManager,
    private val taskScheduler: TaskScheduler
) : com.github.berserkr2k.coreplugin.api.core.database.DatabaseService {
    private var dataSource: HikariDataSource? = null
    lateinit var config: DatabaseConfig
        private set
    private val databases = ConcurrentHashMap<String, FeatureDatabase>()
    val executor: Executor = Executor { command -> taskScheduler.runAsync(command) }

    init {
        try {
            val loadedConfig = configManager.loadModuleConfig("core/database.conf", DatabaseConfig::class.java, DatabaseConfig()).join()
            this.config = loadedConfig
            initializePool()
        } catch (ex: Exception) {
            plugin.logger.severe("Error during database initialization: ${ex.message}")
            throw ex
        }
    }

    private fun initializePool() {
        val hikariConfig = HikariConfig()
        
        when {
            config.driver.equals("postgresql", ignoreCase = true) -> {
                hikariConfig.jdbcUrl = "jdbc:postgresql://${config.host}:${config.port}/${config.database}?sslmode=disable"
                hikariConfig.username = config.username
                hikariConfig.password = config.password
                hikariConfig.driverClassName = "com.github.berserkr2k.coreplugin.libs.postgresql.Driver"
            }
            else -> {
                // SQLite as default local fallback
                val dbFile = plugin.dataFolder.resolve("core/storage/database.db")
                val parent = dbFile.parentFile
                if (!parent.exists()) {
                    parent.mkdirs()
                }
                hikariConfig.jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
                hikariConfig.driverClassName = "org.sqlite.JDBC"
                // Habilitar modo WAL y busy_timeout de 5s para alta concurrencia/evitar bloqueos de base de datos
                hikariConfig.connectionInitSql = "PRAGMA journal_mode=WAL; PRAGMA busy_timeout=5000; PRAGMA synchronous=NORMAL;"
            }
        }

        hikariConfig.maximumPoolSize = config.maxPoolSize
        hikariConfig.minimumIdle = config.minIdle
        hikariConfig.poolName = "CorePlugin-HikariCP"
        
        // Optimizaciones recomendadas
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true")
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250")
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")

        try {
            dataSource = HikariDataSource(hikariConfig)
            plugin.logger.info("¡Pool de conexiones HikariCP (${config.driver.uppercase()}) inicializado con éxito!")
            
            // Ejecutar migraciones de base de datos de forma automática y secuencial
            MigrationManager({ getConnection() }, config.driver, plugin.logger).runMigrations()
        } catch (e: Exception) {
            plugin.logger.severe("Fallo al inicializar el pool de conexiones HikariCP o ejecutar migraciones: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    override fun getDatabase(featureId: String): FeatureDatabase {
        return databases.computeIfAbsent(featureId.lowercase()) {
            HikariFeatureDatabase(it, this)
        }
    }

    fun getConnection(): Connection {
        val ds = dataSource ?: throw IllegalStateException("El DataSource de base de datos no está inicializado.")
        return ds.connection
    }

    fun shutdown() {
        dataSource?.close()
        databases.clear()
    }
}
