package com.github.berserkr2k.coreplugin.infrastructure.database

import java.sql.Connection
import java.util.logging.Logger

class MigrationManager(
    private val connectionProvider: () -> Connection,
    private val driver: String,
    private val logger: Logger
) {

    fun runMigrations() {
        val isPostgreSQL = driver.equals("postgresql", ignoreCase = true)
        val isSQLite = !isPostgreSQL

        val idType = if (isPostgreSQL) {
            "SERIAL PRIMARY KEY"
        } else {
            "INTEGER PRIMARY KEY AUTOINCREMENT"
        }

        // En SQLite e PostgreSQL 'INTEGER' es el tipo estándar para llaves primarias/foráneas
        val fkType = "INTEGER"

        val migrations = listOf(
            // 1. Tabla Maestra de Usuarios
            """
            CREATE TABLE IF NOT EXISTS core_users (
                id $idType,
                uuid VARCHAR(36) NOT NULL UNIQUE,
                username VARCHAR(16) NOT NULL,
                chat_color VARCHAR(16) DEFAULT NULL,
                social_spy INT NOT NULL DEFAULT 0,
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
            """.trimIndent(),

            // 7. Tabla de Logs de Transacciones de Economía
            """
            CREATE TABLE IF NOT EXISTS economy_transactions (
                id $idType,
                sender_uuid VARCHAR(36),
                receiver_uuid VARCHAR(36) NOT NULL,
                currency_id VARCHAR(32) NOT NULL,
                amount DECIMAL(20,4) NOT NULL,
                transaction_type VARCHAR(16) NOT NULL,
                initial_balance DECIMAL(20,4) NOT NULL,
                final_balance DECIMAL(20,4) NOT NULL,
                timestamp BIGINT NOT NULL
            )
            """.trimIndent(),

            // 8. Tabla de Transacciones del Mercado Dinámico (Tienda)
            """
            CREATE TABLE IF NOT EXISTS market_transactions (
                id $idType,
                user_id $fkType NOT NULL,
                shop_id VARCHAR(32) NOT NULL,
                item_id VARCHAR(64) NOT NULL,
                transaction_type VARCHAR(8) NOT NULL,
                quantity INT NOT NULL,
                total_price DECIMAL(20,4) NOT NULL DEFAULT 0.0000,
                timestamp BIGINT NOT NULL,
                FOREIGN KEY (user_id) REFERENCES core_users(id) ON DELETE CASCADE
            )
            """.trimIndent(),

            // 9. Índice para búsquedas rápidas en transacciones de mercado
            "CREATE INDEX IF NOT EXISTS idx_market_lookup ON market_transactions (item_id, timestamp)"
        )

        // 1. Detectar y recrear tabla si es una estructura antigua (sin total_price)
        try {
            connectionProvider().use { conn ->
                val meta = conn.metaData
                var tableExists = false
                meta.getTables(null, null, null, arrayOf("TABLE")).use { rs ->
                    while (rs.next()) {
                        val name = rs.getString("TABLE_NAME")
                        if (name.equals("market_transactions", ignoreCase = true)) {
                            tableExists = true
                            break
                        }
                    }
                }

                var hasTotalPrice = false
                if (tableExists) {
                    meta.getColumns(null, null, null, null).use { rs ->
                        while (rs.next()) {
                            val tableName = rs.getString("TABLE_NAME")
                            val columnName = rs.getString("COLUMN_NAME")
                            if (tableName.equals("market_transactions", ignoreCase = true) &&
                                columnName.equals("total_price", ignoreCase = true)) {
                                hasTotalPrice = true
                                break
                            }
                        }
                    }
                }

                if (tableExists && !hasTotalPrice) {
                    logger.warning("⚠ Se detectó un esquema antiguo en la tabla market_transactions. Recreando la tabla para aplicar el nuevo diseño relacional...")
                    conn.createStatement().use { stmt ->
                        try {
                            stmt.execute("DROP INDEX IF EXISTS idx_market_lookup;")
                        } catch (ignored: Exception) {}
                        stmt.execute("DROP TABLE IF EXISTS market_transactions;")
                    }
                }
            }
        } catch (e: Exception) {
            logger.severe("Fallo al verificar/recrear la tabla market_transactions: ${e.message}")
        }

        // 1.5. Detectar y alterar tabla core_users si le faltan columnas chat_color o social_spy (solo si la tabla ya existe)
        try {
            connectionProvider().use { conn ->
                val meta = conn.metaData
                var coreUsersExists = false
                meta.getTables(null, null, null, arrayOf("TABLE")).use { rs ->
                    while (rs.next()) {
                        val name = rs.getString("TABLE_NAME")
                        if (name.equals("core_users", ignoreCase = true)) {
                            coreUsersExists = true
                            break
                        }
                    }
                }

                if (coreUsersExists) {
                    var hasChatColor = false
                    var hasSocialSpy = false
                    meta.getColumns(null, null, null, null).use { rs ->
                        while (rs.next()) {
                            val tableName = rs.getString("TABLE_NAME")
                            val columnName = rs.getString("COLUMN_NAME")
                            if (tableName.equals("core_users", ignoreCase = true)) {
                                if (columnName.equals("chat_color", ignoreCase = true)) {
                                    hasChatColor = true
                                }
                                if (columnName.equals("social_spy", ignoreCase = true)) {
                                    hasSocialSpy = true
                                }
                            }
                        }
                    }

                    conn.createStatement().use { stmt ->
                        if (!hasChatColor) {
                            logger.info("Añadiendo columna chat_color a la tabla core_users...")
                            try {
                                stmt.execute("ALTER TABLE core_users ADD COLUMN chat_color VARCHAR(16) DEFAULT NULL")
                            } catch (e: Exception) {
                                logger.warning("No se pudo añadir columna chat_color (quizá ya existe): ${e.message}")
                            }
                        }
                        if (!hasSocialSpy) {
                            logger.info("Añadiendo columna social_spy a la tabla core_users...")
                            try {
                                stmt.execute("ALTER TABLE core_users ADD COLUMN social_spy INT NOT NULL DEFAULT 0")
                            } catch (e: Exception) {
                                logger.warning("No se pudo añadir columna social_spy (quizá ya existe): ${e.message}")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.severe("Fallo al verificar/alterar la tabla core_users: ${e.message}")
        }

        try {
            connectionProvider().use { conn ->
                val originalAutoCommit = conn.autoCommit
                conn.autoCommit = false
                try {
                    conn.createStatement().use { stmt ->
                        // Desactivar temporalmente foreign key checks si es SQLite para evitar errores de dependencias circulares
                        if (isSQLite) {
                            stmt.execute("PRAGMA foreign_keys = OFF;")
                        }

                        for (query in migrations) {
                            stmt.execute(query)
                        }

                        if (isSQLite) {
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
