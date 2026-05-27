package com.github.berserkr2k.coreplugin.database

import com.github.berserkr2k.coreplugin.CorePlugin
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.File
import java.sql.Connection
import java.sql.SQLException

class DatabaseManager(private val plugin: CorePlugin) {

    private lateinit var hikari: HikariDataSource
    val tablePrefix: String

    init {
        // Leemos el prefijo de la tabla desde config.yml
        tablePrefix = plugin.mainConfig.get().getString("Database.TablePrefix") ?: "global_"
        connect()
        createTables()
    }

    private fun connect() {
        val config = HikariConfig()
        val dbType = plugin.mainConfig.get().getString("Database.Type")?.uppercase() ?: "SQLITE"

        if (dbType == "MYSQL") {
            // Conexión Global (MariaDB / MySQL)
            val host = plugin.mainConfig.get().getString("Database.Host")
            val port = plugin.mainConfig.get().getInt("Database.Port")
            val name = plugin.mainConfig.get().getString("Database.Name")
            val user = plugin.mainConfig.get().getString("Database.User")
            val pass = plugin.mainConfig.get().getString("Database.Password")

            config.jdbcUrl = "jdbc:mysql://$host:$port/$name?useSSL=false"
            config.driverClassName = "com.mysql.cj.jdbc.Driver"
            config.username = user
            config.password = pass
            plugin.logger.info("Conectando a base de datos MYSQL/MariaDB global...")
        } else {
            // Conexión Local (SQLite)
            val dbFile = File(plugin.dataFolder, "database.db")
            if (!dbFile.exists()) {
                dbFile.parentFile.mkdirs()
                dbFile.createNewFile()
            }
            config.jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
            config.driverClassName = "org.sqlite.JDBC"
            config.connectionTestQuery = "SELECT 1"
            plugin.logger.info("Conectando a base de datos SQLITE local...")
        }

        // Optimizaciones de la piscina (Pool)
        config.maximumPoolSize = 10
        config.minimumIdle = 2
        config.connectionTimeout = 30000
        config.idleTimeout = 600000
        config.maxLifetime = 1800000

        hikari = HikariDataSource(config)
    }

    @Throws(SQLException::class)
    fun getConnection(): Connection {
        return hikari.connection
    }

    fun close() {
        if (this::hikari.isInitialized && !hikari.isClosed) {
            hikari.close()
        }
    }

    private fun createTables() {
        // Usamos el tablePrefix para crear la tabla correcta
        val tableName = "${tablePrefix}player_stats"

        getConnection().use { connection ->
            connection.prepareStatement(
                """
                CREATE TABLE IF NOT EXISTS $tableName (
                    uuid VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(16) NOT NULL,
                    kills INT DEFAULT 0,
                    coins DOUBLE DEFAULT 0.0,
                    chat_color VARCHAR(16) DEFAULT '<white>'
                )
                """.trimIndent()
            ).use { statement ->
                statement.execute()
            }
        }
    }

    /**
     * Obtiene las monedas de un jugador desde la base de datos.
     */
    fun getCoins(uuid: java.util.UUID): Double {
        var coins = 0.0
        try {
            getConnection().use { connection ->
                val query = "SELECT coins FROM ${tablePrefix}player_stats WHERE uuid = ?"
                connection.prepareStatement(query).use { ps ->
                    ps.setString(1, uuid.toString())
                    val rs = ps.executeQuery()
                    if (rs.next()) {
                        coins = rs.getDouble("coins")
                    }
                }
            }
        } catch (e: Exception) {
            // Ignoramos errores silenciosamente para no spamear la consola si la DB está ocupada
        }
        return coins
    }
}