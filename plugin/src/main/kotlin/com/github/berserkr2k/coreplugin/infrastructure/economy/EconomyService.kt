package com.github.berserkr2k.coreplugin.infrastructure.economy

import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import com.github.berserkr2k.coreplugin.infrastructure.database.DatabaseService
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Connection
import java.sql.ResultSetMetaData
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class EconomyService(
    private val plugin: Plugin,
    private val configManager: ModularConfigManager,
    private val databaseService: DatabaseService
) {
    val currencies = ConcurrentHashMap<String, CurrencyConfig>()
    private val balanceCache = ConcurrentHashMap<UUID, ConcurrentHashMap<String, BigDecimal>>()
    
    // Futuro de inicialización del servicio
    val initFuture = CompletableFuture<Void>()

    init {
        // Asegurar que la base de datos esté lista antes de inicializar la economía (síncronamente)
        try {
            setupCurrenciesAndDatabase()
        } catch (ex: Exception) {
            initFuture.completeExceptionally(ex)
            throw ex
        }
    }

    private fun setupCurrenciesAndDatabase() {
        val currenciesFolder = File(plugin.dataFolder, "currencies")
        if (!currenciesFolder.exists()) {
            currenciesFolder.mkdirs()
            // Escribir divisas predeterminadas
            writeDefaultCurrencyFile(File(currenciesFolder, "credits.conf"), "credits", "Créditos", "$", "balance_credits", "100.0", "10000000.0", true, "##")
            writeDefaultCurrencyFile(File(currenciesFolder, "points.conf"), "points", "Puntos", "🔶", "balance_points", "0", "5000000", false, null)
        }

        val configFiles = currenciesFolder.listFiles { _, name -> name.endsWith(".conf") } ?: emptyArray()
        for (file in configFiles) {
            val relativePath = "currencies/${file.name}"
            try {
                val loadedConfig = configManager.loadModuleConfig(relativePath, CurrencyConfig::class.java, CurrencyConfig()).join()
                currencies[loadedConfig.id] = loadedConfig
            } catch (e: Exception) {
                plugin.logger.severe("Error al cargar la divisa desde ${file.name}: ${e.message}")
            }
        }

        try {
            initializeDatabaseTables()
            initFuture.complete(null)
            plugin.logger.info("¡Módulo de Economía Multi-Divisa inicializado con ${currencies.size} monedas!")
        } catch (e: Exception) {
            initFuture.completeExceptionally(e)
            throw e
        }
    }

    private fun writeDefaultCurrencyFile(
        file: File,
        id: String,
        displayName: String,
        symbol: String,
        dbColumn: String,
        initialBalance: String,
        maxBalance: String,
        isDecimal: Boolean,
        maxDecimal: String?
    ) {
        val decimalStr = if (isDecimal) "true" else "false"
        val maxDecStr = if (maxDecimal != null) "\"$maxDecimal\"" else "null"
        file.writeText("""
            id = "$id"
            displayName = "$displayName"
            symbol = "$symbol"
            format = "%symbol%%amount%"
            isDecimal = $decimalStr
            maxDecimal = $maxDecStr
            initialBalance = "$initialBalance"
            maxBalance = "$maxBalance"
            permissionRequired = null
            p2pEnabled = true
            minTransfer = "1.0"
            exchangeRates = {}
            dbColumn = "$dbColumn"
            crossServer = false
            commands = ["$id"]
        """.trimIndent())
    }

    private fun initializeDatabaseTables() {
        val isSQLite = databaseService.config.driver.equals("sqlite", ignoreCase = true)
        databaseService.getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                // 1. Tabla de Economía del Jugador
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_economy (
                        uuid VARCHAR(36) PRIMARY KEY,
                        last_seen BIGINT NOT NULL
                    )
                """.trimIndent())

                // 2. Tabla de Logs de Transacciones (Append-Only)
                if (isSQLite) {
                    stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS economy_transactions (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            sender_uuid VARCHAR(36),
                            receiver_uuid VARCHAR(36),
                            currency_id VARCHAR(64) NOT NULL,
                            amount DECIMAL(20,4) NOT NULL,
                            transaction_type VARCHAR(32) NOT NULL,
                            initial_balance DECIMAL(20,4) NOT NULL,
                            final_balance DECIMAL(20,4) NOT NULL,
                            timestamp BIGINT NOT NULL
                        )
                    """.trimIndent())
                } else {
                    stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS economy_transactions (
                            id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            sender_uuid VARCHAR(36),
                            receiver_uuid VARCHAR(36),
                            currency_id VARCHAR(64) NOT NULL,
                            amount DECIMAL(20,4) NOT NULL,
                            transaction_type VARCHAR(32) NOT NULL,
                            initial_balance DECIMAL(20,4) NOT NULL,
                            final_balance DECIMAL(20,4) NOT NULL,
                            timestamp BIGINT NOT NULL
                        )
                    """.trimIndent())
                }

                // 3. Tabla de Bloqueos Cross-Server
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_sync_locks (
                        uuid VARCHAR(36) PRIMARY KEY,
                        locked_at BIGINT NOT NULL
                    )
                """.trimIndent())
            }

            // 4. Agregar Columnas Dinámicas para Divisas
            checkAndAddColumns(conn)
        }
    }

    private fun checkAndAddColumns(conn: Connection) {
        val metaData = conn.prepareStatement("SELECT * FROM player_economy LIMIT 0").use { stmt ->
            stmt.executeQuery().metaData
        }
        val existingColumns = mutableSetOf<String>()
        for (i in 1..metaData.columnCount) {
            existingColumns.add(metaData.getColumnName(i).lowercase())
        }

        for (currency in currencies.values) {
            val col = currency.dbColumn
            if (!existingColumns.contains(col.lowercase())) {
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate("ALTER TABLE player_economy ADD COLUMN $col DECIMAL(20,4) NOT NULL DEFAULT ${currency.initialBalance}")
                }
                plugin.logger.info("Columna dinámica registrada en base de datos: player_economy ($col)")
            }
        }
    }

    /**
     * Verifica si el balance de un jugador está cargado en el caché (jugador online).
     */
    fun isCached(uuid: UUID): Boolean {
        return balanceCache.containsKey(uuid)
    }

    /**
     * Obtiene el balance de una divisa específica para un jugador.
     * Consulta el caché para jugadores online y la base de datos para offline.
     */
    fun getBalance(uuid: UUID, currencyId: String): BigDecimal {
        val currency = currencies[currencyId] ?: throw IllegalArgumentException("Divisa no registrada: $currencyId")
        
        // 1. Verificar Caché en memoria
        val playerCache = balanceCache[uuid]
        if (playerCache != null) {
            val cachedBal = playerCache[currencyId]
            if (cachedBal != null) return cachedBal
        }

        // 2. Jugador Offline -> Consultar Base de Datos directamente (Síncrono)
        return getBalanceFromDB(uuid, currency)
    }

    private fun getBalanceFromDB(uuid: UUID, currency: CurrencyConfig): BigDecimal {
        databaseService.getConnection().use { conn ->
            val ps = conn.prepareStatement("SELECT ${currency.dbColumn} FROM player_economy WHERE uuid = ?")
            ps.setString(1, uuid.toString())
            val rs = ps.executeQuery()
            if (rs.next()) {
                return rs.getBigDecimal(1) ?: BigDecimal(currency.initialBalance)
            }
        }
        return BigDecimal(currency.initialBalance)
    }

    /**
     * Carga el perfil del jugador en el caché al entrar al servidor.
     */
    fun loadPlayerCache(uuid: UUID): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            val map = ConcurrentHashMap<String, BigDecimal>()
            databaseService.getConnection().use { conn ->
                // Registrar/Actualizar el jugador en player_economy
                val checkPs = conn.prepareStatement("SELECT last_seen FROM player_economy WHERE uuid = ?")
                checkPs.setString(1, uuid.toString())
                val exists = checkPs.executeQuery().next()
                
                val now = System.currentTimeMillis()
                if (!exists) {
                    // Crear registro inicial
                    val insertPs = conn.prepareStatement("INSERT INTO player_economy (uuid, last_seen) VALUES (?, ?)")
                    insertPs.setString(1, uuid.toString())
                    insertPs.setLong(2, now)
                    insertPs.executeUpdate()
                } else {
                    // Actualizar actividad
                    val updatePs = conn.prepareStatement("UPDATE player_economy SET last_seen = ? WHERE uuid = ?")
                    updatePs.setLong(1, now)
                    updatePs.setString(2, uuid.toString())
                    updatePs.executeUpdate()
                }

                // Cargar balances de todas las divisas activas
                val ps = conn.prepareStatement("SELECT * FROM player_economy WHERE uuid = ?")
                ps.setString(1, uuid.toString())
                val rs = ps.executeQuery()
                if (rs.next()) {
                    for (currency in currencies.values) {
                        val bal = rs.getBigDecimal(currency.dbColumn) ?: BigDecimal(currency.initialBalance)
                        map[currency.id] = bal
                    }
                }
            }
            balanceCache[uuid] = map
        }, { command -> Bukkit.getAsyncScheduler().runNow(plugin) { _ -> command.run() } })
    }

    /**
     * Guarda y libera el perfil del jugador al salir del servidor (Ejecutado síncronamente en quit para cross-server).
     */
    fun saveAndUnloadPlayer(uuid: UUID) {
        val playerCache = balanceCache.remove(uuid) ?: return
        
        databaseService.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                val sb = StringBuilder("UPDATE player_economy SET last_seen = ?")
                val params = mutableListOf<Any>()
                params.add(System.currentTimeMillis())

                for (currency in currencies.values) {
                    val bal = playerCache[currency.id] ?: BigDecimal(currency.initialBalance)
                    sb.append(", ${currency.dbColumn} = ?")
                    params.add(bal)
                }
                sb.append(" WHERE uuid = ?")
                params.add(uuid.toString())

                val ps = conn.prepareStatement(sb.toString())
                for (i in params.indices) {
                    val p = params[i]
                    if (p is Long) ps.setLong(i + 1, p)
                    else if (p is BigDecimal) ps.setBigDecimal(i + 1, p)
                    else ps.setString(i + 1, p.toString())
                }
                ps.executeUpdate()
                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                plugin.logger.severe("Fallo al guardar balances de salida para $uuid: ${e.message}")
            }
        }
    }

    /**
     * Modifica el balance de una divisa en caché de forma asíncrona y lo impacta en la base de datos de manera segura.
     */
    fun modifyBalance(uuid: UUID, currencyId: String, amount: BigDecimal, type: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            val currency = currencies[currencyId] ?: return@supplyAsync false
            val isSQLite = databaseService.config.driver.equals("sqlite", ignoreCase = true)
            
            databaseService.getConnection().use { conn ->
                conn.autoCommit = false
                try {
                    // 1. Obtener balance con row-locking
                    val query = if (isSQLite) {
                        "SELECT ${currency.dbColumn} FROM player_economy WHERE uuid = ?"
                    } else {
                        "SELECT ${currency.dbColumn} FROM player_economy WHERE uuid = ? FOR UPDATE"
                    }
                    
                    val ps = conn.prepareStatement(query)
                    ps.setString(1, uuid.toString())
                    val rs = ps.executeQuery()
                    
                    val currentBal = if (rs.next()) {
                        rs.getBigDecimal(1) ?: BigDecimal(currency.initialBalance)
                    } else {
                        BigDecimal(currency.initialBalance)
                    }

                    val finalBal = currentBal.add(amount)
                    val maxBal = BigDecimal(currency.maxBalance)
                    
                    // Validaciones de seguridad
                    if (finalBal < BigDecimal.ZERO || finalBal > maxBal) {
                        conn.rollback()
                        return@supplyAsync false
                    }

                    // 2. Actualizar balance
                    val updatePs = conn.prepareStatement("UPDATE player_economy SET ${currency.dbColumn} = ? WHERE uuid = ?")
                    updatePs.setBigDecimal(1, finalBal)
                    updatePs.setString(2, uuid.toString())
                    updatePs.executeUpdate()

                    // 3. Registrar Log inmutable
                    writeTransaction(conn, null, uuid, currency.id, amount.abs(), type, currentBal, finalBal)

                    conn.commit()

                    // Actualizar caché en memoria si el jugador está online
                    val playerCache = balanceCache[uuid]
                    if (playerCache != null) {
                        playerCache[currencyId] = finalBal
                    }
                    true
                } catch (e: Exception) {
                    conn.rollback()
                    plugin.logger.severe("Fallo en modifyBalance para $uuid ($currencyId): ${e.message}")
                    e.printStackTrace()
                    false
                }
            }
        }, { command -> Bukkit.getAsyncScheduler().runNow(plugin) { _ -> command.run() } })
    }

    /**
     * Retira balance de un jugador de forma segura utilizando Caché en Memoria con Escritura Asíncrona (Write-Behind).
     * El saldo en caché se actualiza de forma síncrona/atómica para mitigar Click-Spam en GUIs.
     */
    fun withdrawCacheBehind(uuid: UUID, currencyId: String, amount: BigDecimal, type: String): CompletableFuture<Boolean> {
        val currency = currencies[currencyId] ?: return CompletableFuture.completedFuture(false)
        if (amount <= BigDecimal.ZERO) return CompletableFuture.completedFuture(false)

        val playerCache = balanceCache[uuid]
        if (playerCache != null) {
            // JUGADOR ONLINE -> Sincronizar en memoria instantáneamente
            synchronized(playerCache) {
                val current = playerCache[currencyId] ?: BigDecimal(currency.initialBalance)
                if (current < amount) return CompletableFuture.completedFuture(false)
                
                val finalBal = current.subtract(amount)
                playerCache[currencyId] = finalBal
            }

            // Delegar la persistencia a un hilo asíncrono seguro
            return CompletableFuture.supplyAsync({
                val isSQLite = databaseService.config.driver.equals("sqlite", ignoreCase = true)
                databaseService.getConnection().use { conn ->
                    conn.autoCommit = false
                    try {
                        val query = if (isSQLite) {
                            "SELECT ${currency.dbColumn} FROM player_economy WHERE uuid = ?"
                        } else {
                            "SELECT ${currency.dbColumn} FROM player_economy WHERE uuid = ? FOR UPDATE"
                        }
                        val ps = conn.prepareStatement(query)
                        ps.setString(1, uuid.toString())
                        val rs = ps.executeQuery()

                        val dbCurrent = if (rs.next()) {
                            rs.getBigDecimal(1) ?: BigDecimal(currency.initialBalance)
                        } else {
                            BigDecimal(currency.initialBalance)
                        }

                        if (dbCurrent < amount) {
                            conn.rollback()
                            return@supplyAsync false
                        }

                        val dbFinal = dbCurrent.subtract(amount)
                        val updatePs = conn.prepareStatement("UPDATE player_economy SET ${currency.dbColumn} = ? WHERE uuid = ?")
                        updatePs.setBigDecimal(1, dbFinal)
                        updatePs.setString(2, uuid.toString())
                        updatePs.executeUpdate()

                        writeTransaction(conn, null, uuid, currency.id, amount, type, dbCurrent, dbFinal)
                        conn.commit()
                        true
                    } catch (e: Exception) {
                        conn.rollback()
                        plugin.logger.severe("Fallo al persistir retiro asíncrono para $uuid ($currencyId): ${e.message}")
                        e.printStackTrace()
                        false
                    }
                }
            }, { command -> Bukkit.getAsyncScheduler().runNow(plugin) { _ -> command.run() } })
        } else {
            // JUGADOR OFFLINE -> Transacción directa en base de datos
            return modifyBalance(uuid, currencyId, amount.negate(), type)
        }
    }

    /**
     * Deposita balance de un jugador utilizando Caché en Memoria con Escritura Asíncrona (Write-Behind).
     */
    fun depositCacheBehind(uuid: UUID, currencyId: String, amount: BigDecimal, type: String): CompletableFuture<Boolean> {
        val currency = currencies[currencyId] ?: return CompletableFuture.completedFuture(false)
        if (amount <= BigDecimal.ZERO) return CompletableFuture.completedFuture(false)

        val playerCache = balanceCache[uuid]
        if (playerCache != null) {
            // JUGADOR ONLINE -> Sincronizar en memoria instantáneamente
            val maxBal = BigDecimal(currency.maxBalance)
            synchronized(playerCache) {
                val current = playerCache[currencyId] ?: BigDecimal(currency.initialBalance)
                val finalBal = current.add(amount)
                if (finalBal > maxBal) return CompletableFuture.completedFuture(false)
                
                playerCache[currencyId] = finalBal
            }

            // Delegar persistencia a un hilo asíncrono
            return CompletableFuture.supplyAsync({
                val isSQLite = databaseService.config.driver.equals("sqlite", ignoreCase = true)
                databaseService.getConnection().use { conn ->
                    conn.autoCommit = false
                    try {
                        val query = if (isSQLite) {
                            "SELECT ${currency.dbColumn} FROM player_economy WHERE uuid = ?"
                        } else {
                            "SELECT ${currency.dbColumn} FROM player_economy WHERE uuid = ? FOR UPDATE"
                        }
                        val ps = conn.prepareStatement(query)
                        ps.setString(1, uuid.toString())
                        val rs = ps.executeQuery()

                        val dbCurrent = if (rs.next()) {
                            rs.getBigDecimal(1) ?: BigDecimal(currency.initialBalance)
                        } else {
                            BigDecimal(currency.initialBalance)
                        }

                        val dbFinal = dbCurrent.add(amount)
                        val maxBalanceVal = BigDecimal(currency.maxBalance)
                        if (dbFinal > maxBalanceVal) {
                            conn.rollback()
                            return@supplyAsync false
                        }

                        val updatePs = conn.prepareStatement("UPDATE player_economy SET ${currency.dbColumn} = ? WHERE uuid = ?")
                        updatePs.setBigDecimal(1, dbFinal)
                        updatePs.setString(2, uuid.toString())
                        updatePs.executeUpdate()

                        writeTransaction(conn, null, uuid, currency.id, amount, type, dbCurrent, dbFinal)
                        conn.commit()
                        true
                    } catch (e: Exception) {
                        conn.rollback()
                        plugin.logger.severe("Fallo al persistir depósito asíncrono para $uuid ($currencyId): ${e.message}")
                        e.printStackTrace()
                        false
                    }
                }
            }, { command -> Bukkit.getAsyncScheduler().runNow(plugin) { _ -> command.run() } })
        } else {
            // JUGADOR OFFLINE -> Transacción directa en base de datos
            return modifyBalance(uuid, currencyId, amount, type)
        }
    }

    /**
     * Realiza una transferencia P2P de fondos de forma transaccional ACID asíncrona con Row-Locking.
     */
    fun transferP2P(sender: UUID, receiver: UUID, currencyId: String, amount: BigDecimal): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            val currency = currencies[currencyId] ?: return@supplyAsync false
            val isSQLite = databaseService.config.driver.equals("sqlite", ignoreCase = true)
            
            // Ordenar UUIDs para prevenir interbloqueos (deadlocks)
            val first = if (sender.toString() < receiver.toString()) sender else receiver
            val second = if (first == sender) receiver else sender

            databaseService.getConnection().use { conn ->
                conn.autoCommit = false
                try {
                    // 1. Obtener y bloquear saldos
                    val query = if (isSQLite) {
                        "SELECT uuid, ${currency.dbColumn} FROM player_economy WHERE uuid IN (?, ?)"
                    } else {
                        "SELECT uuid, ${currency.dbColumn} FROM player_economy WHERE uuid IN (?, ?) FOR UPDATE"
                    }
                    
                    val ps = conn.prepareStatement(query)
                    ps.setString(1, first.toString())
                    ps.setString(2, second.toString())
                    val rs = ps.executeQuery()

                    var senderBal = BigDecimal(currency.initialBalance)
                    var receiverBal = BigDecimal(currency.initialBalance)

                    while (rs.next()) {
                        val rowUuid = UUID.fromString(rs.getString(1))
                        val bal = rs.getBigDecimal(2) ?: BigDecimal(currency.initialBalance)
                        if (rowUuid == sender) senderBal = bal
                        else if (rowUuid == receiver) receiverBal = bal
                    }

                    // 2. Verificar fondos suficientes en el emisor
                    if (senderBal < amount) {
                        conn.rollback()
                        return@supplyAsync false
                    }

                    val finalSenderBal = senderBal.subtract(amount)
                    val finalReceiverBal = receiverBal.add(amount)
                    val maxBal = BigDecimal(currency.maxBalance)

                    if (finalReceiverBal > maxBal) {
                        conn.rollback()
                        return@supplyAsync false // El receptor superaría el saldo máximo
                    }

                    // 3. Ejecutar actualizaciones
                    val updateSender = conn.prepareStatement("UPDATE player_economy SET ${currency.dbColumn} = ? WHERE uuid = ?")
                    updateSender.setBigDecimal(1, finalSenderBal)
                    updateSender.setString(2, sender.toString())
                    updateSender.executeUpdate()

                    val updateReceiver = conn.prepareStatement("UPDATE player_economy SET ${currency.dbColumn} = ? WHERE uuid = ?")
                    updateReceiver.setBigDecimal(1, finalReceiverBal)
                    updateReceiver.setString(2, receiver.toString())
                    updateReceiver.executeUpdate()

                    // 4. Escribir registro inmutable de transacciones
                    writeTransaction(conn, sender, receiver, currency.id, amount, "P2P", senderBal, finalSenderBal)

                    conn.commit()

                    // Actualizar cachés locales
                    balanceCache[sender]?.put(currencyId, finalSenderBal)
                    balanceCache[receiver]?.put(currencyId, finalReceiverBal)
                    true
                } catch (e: Exception) {
                    conn.rollback()
                    plugin.logger.severe("Fallo crítico en transacción P2P entre $sender y $receiver ($currencyId): ${e.message}")
                    e.printStackTrace()
                    false
                } finally {
                    TransactionLockManager.release(sender)
                    TransactionLockManager.release(receiver)
                }
            }
        }, { command -> Bukkit.getAsyncScheduler().runNow(plugin) { _ -> command.run() } })
    }

    private fun writeTransaction(
        conn: Connection,
        sender: UUID?,
        receiver: UUID,
        currencyId: String,
        amount: BigDecimal,
        type: String,
        initial: BigDecimal,
        final: BigDecimal
    ) {
        val ps = conn.prepareStatement("""
            INSERT INTO economy_transactions (sender_uuid, receiver_uuid, currency_id, amount, transaction_type, initial_balance, final_balance, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent())
        
        if (sender != null) ps.setString(1, sender.toString()) else ps.setNull(1, java.sql.Types.VARCHAR)
        ps.setString(2, receiver.toString())
        ps.setString(3, currencyId)
        ps.setBigDecimal(4, amount)
        ps.setString(5, type)
        ps.setBigDecimal(6, initial)
        ps.setBigDecimal(7, final)
        ps.setLong(8, System.currentTimeMillis())
        ps.executeUpdate()
    }

    /**
     * Realiza un bloqueo global temporal para evitar duplicación por saltos de servidores.
     */
    fun acquireCrossServerLock(uuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            databaseService.getConnection().use { conn ->
                try {
                    val ps = conn.prepareStatement("REPLACE INTO player_sync_locks (uuid, locked_at) VALUES (?, ?)")
                    ps.setString(1, uuid.toString())
                    ps.setLong(2, System.currentTimeMillis())
                    ps.executeUpdate()
                    true
                } catch (e: Exception) {
                    false
                }
            }
        }, { command -> Bukkit.getAsyncScheduler().runNow(plugin) { _ -> command.run() } })
    }

    /**
     * Remueve el bloqueo global temporal cross-server.
     */
    fun releaseCrossServerLock(uuid: UUID): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            databaseService.getConnection().use { conn ->
                val ps = conn.prepareStatement("DELETE FROM player_sync_locks WHERE uuid = ?")
                ps.setString(1, uuid.toString())
                ps.executeUpdate()
            }
        }, { command -> Bukkit.getAsyncScheduler().runNow(plugin) { _ -> command.run() } })
    }

    /**
     * Consulta si el jugador posee un bloqueo activo en el clúster.
     */
    fun getCrossServerLockTime(uuid: UUID): Long {
        databaseService.getConnection().use { conn ->
            val ps = conn.prepareStatement("SELECT locked_at FROM player_sync_locks WHERE uuid = ?")
            ps.setString(1, uuid.toString())
            val rs = ps.executeQuery()
            if (rs.next()) {
                return rs.getLong(1)
            }
        }
        return 0L
    }

    /**
     * Purga registros inactivos de la tabla de economía de forma programada.
     */
    fun purgeInactiveRecords(daysThreshold: Int): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync({
            val thresholdTime = System.currentTimeMillis() - (daysThreshold.toLong() * 24L * 60L * 60L * 1000L)
            databaseService.getConnection().use { conn ->
                val ps = conn.prepareStatement("DELETE FROM player_economy WHERE last_seen < ?")
                ps.setLong(1, thresholdTime)
                ps.executeUpdate()
            }
        }, { command -> Bukkit.getAsyncScheduler().runNow(plugin) { _ -> command.run() } })
    }

    /**
     * Parser utilitario estricto para traducir abreviaturas como "1.5k" a 1500 directamente en BigDecimal.
     */
    fun parseShorthand(input: String): BigDecimal? {
        val trimmed = input.trim()
        val regex = Regex("^(\\d+(?:\\.\\d+)?)([kmKM]?)$")
        val match = regex.matchEntire(trimmed) ?: return null
        val numberPart = match.groupValues[1]
        val multiplierPart = match.groupValues[2].uppercase()

        return try {
            val value = BigDecimal(numberPart)
            val multiplier = when (multiplierPart) {
                "K" -> BigDecimal("1000")
                "M" -> BigDecimal("1000000")
                else -> BigDecimal.ONE
            }
            value.multiply(multiplier)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formatea un balance numérico BigDecimal según el HOCON de la divisa especificada.
     */
    fun formatBalance(currencyId: String, amount: BigDecimal): String {
        val currency = currencies[currencyId] ?: return amount.toPlainString()
        val scale = if (currency.isDecimal) {
            currency.maxDecimal?.length ?: 2
        } else {
            0
        }
        val rounded = amount.setScale(scale, RoundingMode.HALF_UP)
        val pattern = if (scale > 0) "#,##0." + "0".repeat(scale) else "#,##0"
        val formatter = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
        val amountStr = formatter.format(rounded)
        
        return currency.format
            .replace("%symbol%", currency.symbol ?: "")
            .replace("%amount%", amountStr)
    }
}
