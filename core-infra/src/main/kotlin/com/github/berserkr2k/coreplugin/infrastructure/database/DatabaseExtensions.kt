package com.github.berserkr2k.coreplugin.infrastructure.database

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.concurrent.CompletableFuture

/**
 * Ejecuta una consulta SELECT síncrona y mapea el ResultSet a una lista de objetos T.
 */
fun <T> DatabaseService.query(
    sql: String,
    preparer: (PreparedStatement) -> Unit = {},
    mapper: (ResultSet) -> T
): List<T> {
    val results = mutableListOf<T>()
    this.getConnection().use { conn ->
        conn.prepareStatement(sql).use { stmt ->
            preparer(stmt)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    results.add(mapper(rs))
                }
            }
        }
    }
    return results
}

/**
 * Ejecuta una consulta SELECT de forma asíncrona y mapea el ResultSet a una lista de objetos T.
 */
fun <T> DatabaseService.queryAsync(
    sql: String,
    preparer: (PreparedStatement) -> Unit = {},
    mapper: (ResultSet) -> T
): CompletableFuture<List<T>> {
    return CompletableFuture.supplyAsync {
        query(sql, preparer, mapper)
    }
}

/**
 * Ejecuta una consulta SELECT síncrona que retorna un único resultado opcional (o nulo).
 */
fun <T> DatabaseService.querySingle(
    sql: String,
    preparer: (PreparedStatement) -> Unit = {},
    mapper: (ResultSet) -> T
): T? {
    this.getConnection().use { conn ->
        conn.prepareStatement(sql).use { stmt ->
            preparer(stmt)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return mapper(rs)
                }
            }
        }
    }
    return null
}

/**
 * Ejecuta una consulta SELECT asíncrona que retorna un único resultado opcional (o nulo).
 */
fun <T> DatabaseService.querySingleAsync(
    sql: String,
    preparer: (PreparedStatement) -> Unit = {},
    mapper: (ResultSet) -> T
): CompletableFuture<T?> {
    return CompletableFuture.supplyAsync {
        querySingle(sql, preparer, mapper)
    }
}

/**
 * Ejecuta una sentencia de actualización (INSERT, UPDATE, DELETE) de forma síncrona.
 * Retorna el número de filas afectadas.
 */
fun DatabaseService.execute(
    sql: String,
    preparer: (PreparedStatement) -> Unit = {}
): Int {
    this.getConnection().use { conn ->
        conn.prepareStatement(sql).use { stmt ->
            preparer(stmt)
            return stmt.executeUpdate()
        }
    }
}

/**
 * Ejecuta una sentencia de actualización (INSERT, UPDATE, DELETE) de forma asíncrona.
 * Retorna el número de filas afectadas en un CompletableFuture.
 */
fun DatabaseService.executeAsync(
    sql: String,
    preparer: (PreparedStatement) -> Unit = {}
): CompletableFuture<Int> {
    return CompletableFuture.supplyAsync {
        execute(sql, preparer)
    }
}

/**
 * Ejecuta una transacción síncrona en un bloque de ejecución seguro.
 * Desactiva el auto-commit, realiza las acciones, y hace commit.
 * En caso de excepción, hace rollback y vuelve a lanzar la excepción.
 */
fun <T> DatabaseService.transaction(
    action: (Connection) -> T
): T {
    this.getConnection().use { conn ->
        val originalAutoCommit = conn.autoCommit
        try {
            conn.autoCommit = false
            val result = action(conn)
            conn.commit()
            return result
        } catch (ex: Exception) {
            try {
                conn.rollback()
            } catch (rollbackEx: Exception) {
                ex.addSuppressed(rollbackEx)
            }
            throw ex
        } finally {
            try {
                conn.autoCommit = originalAutoCommit
            } catch (ignored: Exception) {}
        }
    }
}

/**
 * Ejecuta una transacción asíncrona en un bloque de ejecución seguro.
 */
fun <T> DatabaseService.transactionAsync(
    action: (Connection) -> T
): CompletableFuture<T> {
    return CompletableFuture.supplyAsync {
        transaction(action)
    }
}
