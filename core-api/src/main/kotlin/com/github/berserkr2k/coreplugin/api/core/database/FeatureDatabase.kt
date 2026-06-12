package com.github.berserkr2k.coreplugin.api.core.database

import java.sql.Connection
import java.sql.ResultSet
import java.util.concurrent.CompletableFuture

interface FeatureDatabase {
    fun executeUpdate(sql: String, vararg args: Any): CompletableFuture<Int>

    fun <T> query(
        sql: String, 
        mapper: (ResultSet) -> T, 
        vararg args: Any
    ): CompletableFuture<List<T>>

    fun <T> querySingle(
        sql: String, 
        mapper: (ResultSet) -> T, 
        vararg args: Any
    ): CompletableFuture<T?>

    // Soporte para transacciones seguras sin exponer la conexión nativa
    fun executeTransaction(
        action: (Connection) -> Unit
    ): CompletableFuture<Void>
}
