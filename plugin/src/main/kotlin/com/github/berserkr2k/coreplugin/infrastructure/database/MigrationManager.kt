package com.github.berserkr2k.coreplugin.infrastructure.database

import java.sql.Connection
import java.util.logging.Logger

class MigrationManager(
    private val connectionProvider: () -> Connection,
    private val driver: String,
    private val logger: Logger
) {

    fun runMigrations() {
        val isSQLite = driver.equals("sqlite", ignoreCase = true)
        val isPostgreSQL = driver.equals("postgresql", ignoreCase = true)
        val isMySQL = !isSQLite && !isPostgreSQL

        val idType = when {
            isSQLite -> "INTEGER PRIMARY KEY AUTOINCREMENT"
            isPostgreSQL -> "SERIAL PRIMARY KEY"
            else -> "INT UNSIGNED AUTO_INCREMENT PRIMARY KEY"
        }

        val fkType = when {
            isSQLite -> "INTEGER"
            else -> "INT UNSIGNED"
        }

        val migrations = listOf(
            // 1. Tabla Maestra de Usuarios
            """
            CREATE TABLE IF NOT EXISTS core_users (
                id $idType,
                uuid VARCHAR(36) NOT NULL UNIQUE,
                username VARCHAR(16) NOT NULL,
                first_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),

            // 2. Tabla de Economías Múltiples (Estructura Vertical)
            """
            CREATE TABLE IF NOT EXISTS core_economies (
                user_id $fkType NOT NULL,
                currency_id VARCHAR(32) NOT NULL,
                balance DECIMAL(20,4) NOT NULL DEFAULT 0.0000,
                PRIMARY KEY (user_id, currency_id),
                FOREIGN KEY (user_id) REFERENCES core_users(id) ON DELETE CASCADE
            )
            """.trimIndent(),

            // 3. Tabla de Cooldowns de Kits
            """
            CREATE TABLE IF NOT EXISTS core_kits_cooldowns (
                user_id $fkType NOT NULL,
                kit_id VARCHAR(32) NOT NULL,
                last_claimed BIGINT NOT NULL,
                PRIMARY KEY (user_id, kit_id),
                FOREIGN KEY (user_id) REFERENCES core_users(id) ON DELETE CASCADE
            )
            """.trimIndent(),

            // 4. Tabla de Clasificaciones (Leaderboards)
            """
            CREATE TABLE IF NOT EXISTS player_scores (
                user_id $fkType NOT NULL,
                leaderboard_id VARCHAR(32) NOT NULL,
                score DOUBLE NOT NULL DEFAULT 0.0,
                PRIMARY KEY (user_id, leaderboard_id),
                FOREIGN KEY (user_id) REFERENCES core_users(id) ON DELETE CASCADE
            )
            """.trimIndent(),

            // 5. Tabla de Estelas de Proyectil
            """
            CREATE TABLE IF NOT EXISTS core_player_projectile_trails (
                user_id $fkType PRIMARY KEY,
                trail_id VARCHAR(32) NOT NULL,
                FOREIGN KEY (user_id) REFERENCES core_users(id) ON DELETE CASCADE
            )
            """.trimIndent(),

            // 6. Tabla de Bloqueo de Sincronización (Cross-Server)
            """
            CREATE TABLE IF NOT EXISTS player_sync_locks (
                uuid VARCHAR(36) PRIMARY KEY,
                locked_at BIGINT NOT NULL
            )
            """.trimIndent()
        )

        try {
            connectionProvider().use { conn ->
                val originalAutoCommit = conn.autoCommit
                conn.autoCommit = false
                try {
                    conn.createStatement().use { stmt ->
                        // Desactivar temporalmente foreign key checks si es MySQL o SQLite para evitar errores de dependencias circulares
                        if (isMySQL) {
                            stmt.execute("SET FOREIGN_KEY_CHECKS = 0;")
                        } else if (isSQLite) {
                            stmt.execute("PRAGMA foreign_keys = OFF;")
                        }

                        for (query in migrations) {
                            stmt.execute(query)
                        }

                        if (isMySQL) {
                            stmt.execute("SET FOREIGN_KEY_CHECKS = 1;")
                        } else if (isSQLite) {
                            stmt.execute("PRAGMA foreign_keys = ON;")
                        }
                    }
                    conn.commit()
                    logger.info("✔ ¡Migraciones de la base de datos completadas exitosamente ($driver)!")
                } catch (e: Exception) {
                    conn.rollback()
                    throw e
                } finally {
                    try {
                        conn.autoCommit = originalAutoCommit
                    } catch (ignored: Exception) {}
                }
            }
        } catch (e: Exception) {
            logger.severe("❌ Fallaron las migraciones de la base de datos: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}
