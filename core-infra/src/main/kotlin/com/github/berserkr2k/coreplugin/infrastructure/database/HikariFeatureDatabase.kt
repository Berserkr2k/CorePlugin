package com.github.berserkr2k.coreplugin.infrastructure.database

import com.github.berserkr2k.coreplugin.api.core.database.FeatureDatabase
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.concurrent.CompletableFuture

class HikariFeatureDatabase(
    val featureId: String,
    private val service: DatabaseServiceImpl
) : FeatureDatabase {

    override fun executeUpdate(sql: String, vararg args: Any): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync({
            service.getConnection().use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    bindParameters(stmt, args)
                    stmt.executeUpdate()
                }
            }
        }, service.executor)
    }

    override fun <T> query(
        sql: String, 
        mapper: (ResultSet) -> T, 
        vararg args: Any
    ): CompletableFuture<List<T>> {
        return CompletableFuture.supplyAsync({
            service.getConnection().use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    bindParameters(stmt, args)
                    stmt.executeQuery().use { rs ->
                        val list = ArrayList<T>()
                        while (rs.next()) {
                            list.add(mapper(rs))
                        }
                        list
                    }
                }
            }
        }, service.executor)
    }

    override fun <T> querySingle(
        sql: String, 
        mapper: (ResultSet) -> T, 
        vararg args: Any
    ): CompletableFuture<T?> {
        return CompletableFuture.supplyAsync({
            service.getConnection().use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    bindParameters(stmt, args)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            mapper(rs)
                        } else {
                            null
                        }
                    }
                }
            }
        }, service.executor)
    }

    override fun executeTransaction(
        action: (Connection) -> Unit
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            service.getConnection().use { conn ->
                val originalAutoCommit = conn.autoCommit
                try {
                    conn.autoCommit = false
                    action(conn)
                    conn.commit()
                } catch (e: Exception) {
                    try {
                        conn.rollback()
                    } catch (rollbackEx: Exception) {
                        e.addSuppressed(rollbackEx)
                    }
                    throw e
                } finally {
                    try {
                        conn.autoCommit = originalAutoCommit
                    } catch (ignored: Exception) {}
                }
            }
        }, service.executor)
    }

    private fun bindParameters(stmt: PreparedStatement, args: Array<out Any>) {
        for ((index, arg) in args.withIndex()) {
            stmt.setObject(index + 1, arg)
        }
    }
}
