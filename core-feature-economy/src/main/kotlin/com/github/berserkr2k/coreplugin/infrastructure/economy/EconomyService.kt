package com.github.berserkr2k.coreplugin.infrastructure.economy

import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import com.github.berserkr2k.coreplugin.infrastructure.database.DatabaseService
import com.github.berserkr2k.coreplugin.infrastructure.database.*
import com.github.berserkr2k.coreplugin.domain.user.ProfileRegistry
import org.bukkit.plugin.Plugin
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Connection
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class EconomyService(
    private val plugin: Plugin,
    private val configManager: ModularConfigManager,
    private val databaseService: DatabaseService,
    private val profileRegistry: ProfileRegistry
) : com.github.berserkr2k.coreplugin.api.economy.EconomyService {
    override val currencies = ConcurrentHashMap<String, CurrencyConfig>()
    val initFuture = CompletableFuture<Void>()

    init {
        try {
            setupCurrencies()
            initFuture.complete(null)
        } catch (ex: Exception) {
            initFuture.completeExceptionally(ex)
            throw ex
        }
    }

    private fun setupCurrencies() {
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
        plugin.logger.info("¡Módulo de Economía Multi-Divisa inicializado con ${currencies.size} monedas!")
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

    /**
     * Verifica si el balance de un jugador está cargado en el caché (jugador online).
     */
    override fun isCached(uuid: UUID): Boolean {
        return profileRegistry.getProfile(uuid) != null
    }

    /**
     * Obtiene el balance de una divisa específica para un jugador.
     * Consulta el caché del UserProfile para jugadores online y la base de datos para offline.
     */
    override fun getBalance(uuid: UUID, currencyId: String): BigDecimal {
        val currency = currencies[currencyId] ?: throw IllegalArgumentException("Divisa no registrada: $currencyId")
        
        // 1. Consultar Caché en el perfil unificado
        val profile = profileRegistry.getProfile(uuid)
        if (profile != null) {
            val key = currencyId.lowercase()
            val cachedBal = profile.economies[key]
            if (cachedBal == null) {
                val initial = BigDecimal(currency.initialBalance)
                profile.setBalance(currencyId, initial)
                return initial
            }
            return cachedBal
        }

        // 2. Jugador Offline -> Consultar Base de Datos directamente (Síncrono)
        val sql = """
            SELECT e.balance FROM core_economies e
            JOIN core_users u ON e.user_id = u.id
            WHERE u.uuid = ? AND e.currency_id = ?
        """.trimIndent()

        return databaseService.querySingle(
            sql,
            preparer = { stmt -> 
                stmt.setString(1, uuid.toString())
                stmt.setString(2, currencyId.lowercase())
            },
            mapper = { rs -> rs.getBigDecimal(1) }
        ) ?: BigDecimal(currency.initialBalance)
    }

    /**
     * Modifica el balance de una divisa de forma asíncrona.
     * Si el jugador está online, actualiza la caché (0ms). Si está offline, ejecuta la query relacional.
     */
    fun modifyBalance(uuid: UUID, currencyId: String, amount: BigDecimal, type: String): CompletableFuture<Boolean> {
        val currency = currencies[currencyId] ?: return CompletableFuture.completedFuture(false)
        val profile = profileRegistry.getProfile(uuid)

        if (profile != null) {
            // JUGADOR ONLINE -> Operación en caché instantánea (0ms Spigot Tick)
            val current = getBalance(uuid, currencyId)
            val finalBal = current.add(amount)
            val maxBal = BigDecimal(currency.maxBalance)

            if (finalBal < BigDecimal.ZERO || finalBal > maxBal) {
                return CompletableFuture.completedFuture(false)
            }

            profile.setBalance(currencyId, finalBal)

            // Registrar transacción en la BD de forma asíncrona para auditoría
            databaseService.executeAsync(
                "INSERT INTO economy_transactions (sender_uuid, receiver_uuid, currency_id, amount, transaction_type, initial_balance, final_balance, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                preparer = { stmt ->
                    stmt.setString(1, null)
                    stmt.setString(2, uuid.toString())
                    stmt.setString(3, currencyId)
                    stmt.setBigDecimal(4, amount.abs())
                    stmt.setString(5, type)
                    stmt.setBigDecimal(6, current)
                    stmt.setBigDecimal(7, finalBal)
                    stmt.setLong(8, System.currentTimeMillis())
                }
            )
            return CompletableFuture.completedFuture(true)
        } else {
            // JUGADOR OFFLINE -> Transacción SQL relacional directa
            return databaseService.transactionAsync { conn ->
                // Obtener ID del usuario
                var userId = -1
                conn.prepareStatement("SELECT id FROM core_users WHERE uuid = ?").use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            userId = rs.getInt("id")
                        }
                    }
                }

                if (userId == -1) {
                    return@transactionAsync false // El usuario no está registrado en el core
                }

                // Obtener saldo actual
                var current = BigDecimal(currency.initialBalance)
                conn.prepareStatement("SELECT balance FROM core_economies WHERE user_id = ? AND currency_id = ?").use { stmt ->
                    stmt.setInt(1, userId)
                    stmt.setString(2, currencyId.lowercase())
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            current = rs.getBigDecimal("balance")
                        }
                    }
                }

                val finalBal = current.add(amount)
                val maxBal = BigDecimal(currency.maxBalance)

                if (finalBal < BigDecimal.ZERO || finalBal > maxBal) {
                    throw IllegalStateException("Límite de balance excedido en base de datos.")
                }

                // Guardar usando Upsert
                val upsertSql = "INSERT INTO core_economies (user_id, currency_id, balance) VALUES (?, ?, ?) ON CONFLICT(user_id, currency_id) DO UPDATE SET balance = excluded.balance"

                conn.prepareStatement(upsertSql).use { stmt ->
                    stmt.setInt(1, userId)
                    stmt.setString(2, currencyId.lowercase())
                    stmt.setBigDecimal(3, finalBal)
                    stmt.executeUpdate()
                }

                // Registrar Log
                writeTransaction(conn, null, uuid, currency.id, amount.abs(), type, current, finalBal)
                true
            }.exceptionally { e ->
                plugin.logger.severe("Fallo en modifyBalance offline para $uuid ($currencyId): ${e.message}")
                false
            }
        }
    }

    override fun withdrawCacheBehind(uuid: UUID, currencyId: String, amount: BigDecimal, type: String): CompletableFuture<Boolean> {
        return modifyBalance(uuid, currencyId, amount.negate(), type)
    }

    override fun depositCacheBehind(uuid: UUID, currencyId: String, amount: BigDecimal, type: String): CompletableFuture<Boolean> {
        return modifyBalance(uuid, currencyId, amount, type)
    }

    override fun deposit(uuid: UUID, amount: BigDecimal, currencyId: String): CompletableFuture<Boolean> {
        return depositCacheBehind(uuid, currencyId, amount, "API_DEPOSIT")
    }

    override fun withdraw(uuid: UUID, amount: BigDecimal, currencyId: String): CompletableFuture<Boolean> {
        return withdrawCacheBehind(uuid, currencyId, amount, "API_WITHDRAW")
    }

    override fun hasAccount(uuid: UUID): Boolean {
        return true
    }

    /**
     * Transfiere balance de un emisor a un receptor de manera transaccional y libre de deadlocks.
     */
    fun transferP2P(sender: UUID, receiver: UUID, currencyId: String, amount: BigDecimal): CompletableFuture<Boolean> {
        val currency = currencies[currencyId] ?: return CompletableFuture.completedFuture(false)
        if (amount <= BigDecimal.ZERO) return CompletableFuture.completedFuture(false)

        val senderProfile = profileRegistry.getProfile(sender)
        val receiverProfile = profileRegistry.getProfile(receiver)

        if (senderProfile != null && receiverProfile != null) {
            // Ambos online -> Operar en memoria atómicamente
            synchronized(this) {
                val sBal = getBalance(sender, currencyId)
                if (sBal < amount) return java.util.concurrent.CompletableFuture.completedFuture(false)
                
                val rBal = getBalance(receiver, currencyId)
                val newRBal = rBal.add(amount)
                if (newRBal > BigDecimal(currency.maxBalance)) return java.util.concurrent.CompletableFuture.completedFuture(false)

                val newSBal = sBal.subtract(amount)
                senderProfile.setBalance(currencyId, newSBal)
                receiverProfile.setBalance(currencyId, newRBal)

                // Guardar registro de transacción asíncrono para logs
                databaseService.executeAsync(
                    "INSERT INTO economy_transactions (sender_uuid, receiver_uuid, currency_id, amount, transaction_type, initial_balance, final_balance, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    preparer = { stmt ->
                        stmt.setString(1, sender.toString())
                        stmt.setString(2, receiver.toString())
                        stmt.setString(3, currencyId)
                        stmt.setBigDecimal(4, amount)
                        stmt.setString(5, "P2P")
                        stmt.setBigDecimal(6, sBal)
                        stmt.setBigDecimal(7, newSBal)
                        stmt.setLong(8, System.currentTimeMillis())
                    }
                )
                return CompletableFuture.completedFuture(true)
            }
        } else {
            // Al menos uno offline -> Bloqueo y transacción SQL relacional
            val first = if (sender.toString() < receiver.toString()) sender else receiver
            val second = if (first == sender) receiver else sender

            return databaseService.transactionAsync { conn ->
                var senderId = -1
                var receiverId = -1

                // 1. Obtener IDs internos
                conn.prepareStatement("SELECT id, uuid FROM core_users WHERE uuid IN (?, ?)").use { stmt ->
                    stmt.setString(1, sender.toString())
                    stmt.setString(2, receiver.toString())
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            val rUuid = rs.getString("uuid")
                            val rId = rs.getInt("id")
                            if (rUuid == sender.toString()) senderId = rId
                            else if (rUuid == receiver.toString()) receiverId = rId
                        }
                    }
                }

                if (senderId == -1 || receiverId == -1) {
                    throw IllegalStateException("El emisor o receptor no están registrados en el sistema.")
                }

                // 2. Obtener saldos de la base de datos
                var senderBal = BigDecimal(currency.initialBalance)
                var receiverBal = BigDecimal(currency.initialBalance)

                conn.prepareStatement("SELECT user_id, balance FROM core_economies WHERE user_id IN (?, ?) AND currency_id = ?").use { stmt ->
                    stmt.setInt(1, senderId)
                    stmt.setInt(2, receiverId)
                    stmt.setString(3, currencyId.lowercase())
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            val rUserId = rs.getInt("user_id")
                            val rBal = rs.getBigDecimal("balance")
                            if (rUserId == senderId) senderBal = rBal
                            else if (rUserId == receiverId) receiverBal = rBal
                        }
                    }
                }

                // Sincronizar con datos en memoria activos
                if (senderProfile != null) senderBal = senderProfile.getBalance(currencyId)
                if (receiverProfile != null) receiverBal = receiverProfile.getBalance(currencyId)

                if (senderBal < amount) {
                    throw IllegalStateException("Fondos insuficientes.")
                }

                val newSBal = senderBal.subtract(amount)
                val newRBal = receiverBal.add(amount)

                if (newRBal > BigDecimal(currency.maxBalance)) {
                    throw IllegalStateException("El receptor superaría el saldo máximo.")
                }

                // 3. Upserts
                val upsertSql = "INSERT INTO core_economies (user_id, currency_id, balance) VALUES (?, ?, ?) ON CONFLICT(user_id, currency_id) DO UPDATE SET balance = excluded.balance"

                conn.prepareStatement(upsertSql).use { stmt ->
                    stmt.setInt(1, senderId)
                    stmt.setString(2, currencyId.lowercase())
                    stmt.setBigDecimal(3, newSBal)
                    stmt.addBatch()

                    stmt.setInt(1, receiverId)
                    stmt.setString(2, currencyId.lowercase())
                    stmt.setBigDecimal(3, newRBal)
                    stmt.addBatch()

                    stmt.executeBatch()
                }

                // 4. Log
                writeTransaction(conn, sender, receiver, currency.id, amount, "P2P", senderBal, newSBal)

                // Actualizar cachés si corresponde
                senderProfile?.setBalance(currencyId, newSBal)
                receiverProfile?.setBalance(currencyId, newRBal)
                true
            }.exceptionally { e ->
                plugin.logger.severe("Fallo en transferencia P2P entre $sender y $receiver: ${e.message}")
                false
            }
        }
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
        conn.prepareStatement("""
            INSERT INTO economy_transactions (sender_uuid, receiver_uuid, currency_id, amount, transaction_type, initial_balance, final_balance, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { ps ->
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
    }

    /**
     * Purga registros de usuarios que no se han conectado en los últimos X días.
     * Por cascada, elimina automáticamente sus economías, cooldowns, estelas, etc.
     */
    fun purgeInactiveRecords(daysThreshold: Int): CompletableFuture<Int> {
        val thresholdMillis = System.currentTimeMillis() - (daysThreshold.toLong() * 24 * 60 * 60 * 1000)
        val sql = "DELETE FROM core_users WHERE last_login < ?"
        return databaseService.executeAsync(sql) { ps ->
            ps.setTimestamp(1, java.sql.Timestamp(thresholdMillis))
        }
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
    override fun formatBalance(currencyId: String, amount: BigDecimal): String {
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
