package com.github.berserkr2k.coreplugin.infrastructure.economy

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.StringParser.stringParser
import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.common.FancyLogger
import com.github.berserkr2k.coreplugin.common.TransactionLockManager
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.CoreMessages
import com.github.berserkr2k.coreplugin.api.core.message.PlaceholderContext
import java.math.BigDecimal
import java.util.UUID

import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry

class WalletCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val economyService: EconomyService,
    private val messageService: MessageService,
    private val serviceRegistry: ServiceRegistry
) {
    private val taskScheduler = serviceRegistry.get(TaskScheduler::class.java)

    init {
        registerWalletCommands()
        registerPayCommand()
        registerAdminEcoCommands()
        registerDynamicCurrencyCommands()
    }

    private fun send(sender: CommandSender, key: com.github.berserkr2k.coreplugin.api.core.message.MessageKey, vararg placeholders: Pair<String, String>) {
        messageService.send(sender, key, PlaceholderContext.of(*placeholders))
    }

    private fun registerWalletCommands() {
        // 1. /wallet o /bal [player]
        val walletBuilder = manager.commandBuilder("wallet")
            .optional("player", stringParser())
            .permission("core.economy.wallet")
            .handler { context ->
                val sender = context.sender()
                val targetName = context.optional<String>("player").orElse(null)
                
                if (targetName != null) {
                    // Consultar balance de otro jugador
                    if (!sender.hasPermission("core.economy.wallet.other")) {
                        send(sender, EconomyMessages.NO_PERMISSION_OTHER)
                        return@handler
                    }
                    val targetPlayer = Bukkit.getPlayer(targetName)
                    val targetOffline = if (targetPlayer == null) Bukkit.getOfflinePlayer(targetName) else null
                    val uuid: UUID = targetPlayer?.uniqueId ?: targetOffline!!.uniqueId
                    val name: String = targetPlayer?.name ?: (targetOffline?.name ?: targetName)

                    showWallet(sender, uuid, name)
                } else {
                    // Consultar balance propio
                    if (sender !is Player) {
                        send(sender, EconomyMessages.USAGE_WALLET)
                        return@handler
                    }
                    showWallet(sender, sender.uniqueId, sender.name)
                }
            }

        manager.command(walletBuilder)
        
        // Registrar alias /bal
        manager.command(
            manager.commandBuilder("bal")
                .optional("player", stringParser())
                .permission("core.economy.wallet")
                .handler(walletBuilder.handler())
        )
    }

    private fun showWallet(sender: CommandSender, uuid: UUID, targetName: String) {
        // Ejecutar en segundo plano de forma no bloqueante
        taskScheduler.runAsync {
            // Cargar el formato del header
            val headerTemplate = messageService.getRawTemplate(EconomyMessages.WALLET_HEADER)
                .ifEmpty { "<gold><bold>★ BILLETERA DE <top_id> ★</bold></gold>\n" }
            val builder = StringBuilder(headerTemplate.replace("<top_id>", targetName))
            var hasAny = false

            for (currency in economyService.currencies.values) {
                // Verificar permiso de divisa si aplica
                if (currency.permissionRequired != null && !sender.hasPermission(currency.permissionRequired)) {
                    continue
                }
                val bal = economyService.getBalance(uuid, currency.id)
                val formatted = economyService.formatBalance(currency.id, bal)
                val lineTemplate = messageService.getRawTemplate(EconomyMessages.WALLET_LINE)
                    .ifEmpty { "  <gray>- <display_name>: </gray><white><balance></white>" }
                val resolvedLine = lineTemplate
                    .replace("<display_name>", currency.displayName)
                    .replace("<balance>", formatted)
                builder.append(resolvedLine).append("\n")
                hasAny = true
            }

            if (!hasAny) {
                send(sender, EconomyMessages.NO_PERMISSION_VIEW)
            } else {
                messageService.sendRaw(sender, builder.toString().trimEnd())
            }
        }
    }

    private fun registerPayCommand() {
        // /pay <player> <amount> [currency]
        manager.command(
            manager.commandBuilder("pay")
                .required("player", stringParser())
                .required("amount", stringParser())
                .optional("currency", stringParser())
                .permission("core.economy.pay")
                .handler { context ->
                    val sender = context.sender() as? Player ?: run {
                        send(context.sender(), EconomyMessages.ONLY_PLAYERS_PAY)
                        return@handler
                    }

                    val targetName = context.get<String>("player")
                    val amountStr = context.get<String>("amount")
                    val currencyId = context.optional<String>("currency").orElse("credits") // credits por defecto

                    val currency = economyService.currencies[currencyId]
                    if (currency == null) {
                        send(sender, EconomyMessages.CURRENCY_NOT_FOUND, "currency" to currencyId)
                        return@handler
                    }

                    if (!currency.p2pEnabled) {
                        send(sender, EconomyMessages.P2P_DISABLED)
                        return@handler
                    }

                    val rawAmount = economyService.parseShorthand(amountStr)
                    if (rawAmount == null || rawAmount <= BigDecimal.ZERO) {
                        send(sender, EconomyMessages.INVALID_AMOUNT)
                        return@handler
                    }

                    // Asegurar precisión decimal
                    val amount = if (currency.isDecimal) {
                        val scale = currency.maxDecimal?.length ?: 2
                        rawAmount.setScale(scale, java.math.RoundingMode.HALF_UP)
                    } else {
                        rawAmount.setScale(0, java.math.RoundingMode.HALF_UP)
                    }

                    val minTransfer = BigDecimal(currency.minTransfer)
                    if (amount < minTransfer) {
                        val formattedMin = economyService.formatBalance(currency.id, minTransfer)
                        send(sender, EconomyMessages.MIN_TRANSFER, "min_transfer" to formattedMin)
                        return@handler
                    }

                    val targetPlayer = Bukkit.getPlayer(targetName)
                    if (targetPlayer == null || !targetPlayer.isOnline) {
                        send(sender, EconomyMessages.TARGET_OFFLINE)
                        return@handler
                    }

                    if (targetPlayer.uniqueId == sender.uniqueId) {
                        send(sender, EconomyMessages.CANNOT_PAY_SELF)
                        return@handler
                    }

                    // Adquirir Bloqueos en Hilo Principal de forma atómica para prevenir Doble Gasto concurrente
                    if (!TransactionLockManager.acquire(sender.uniqueId)) {
                        send(sender, EconomyMessages.TRANSACTION_IN_PROGRESS_SELF)
                        return@handler
                    }
                    if (!TransactionLockManager.acquire(targetPlayer.uniqueId)) {
                        TransactionLockManager.release(sender.uniqueId)
                        send(sender, EconomyMessages.TRANSACTION_IN_PROGRESS_TARGET)
                        return@handler
                    }

                    // Ejecutar transacción SQL P2P asíncrona de forma segura
                    economyService.transferP2P(sender.uniqueId, targetPlayer.uniqueId, currency.id, amount).thenAccept { success ->
                        if (success) {
                            val formatted = economyService.formatBalance(currency.id, amount)
                            send(sender, EconomyMessages.PAY_SUCCESS_SENDER, "amount" to formatted, "target" to targetPlayer.name)
                            send(targetPlayer, EconomyMessages.PAY_SUCCESS_RECEIVER, "amount" to formatted, "sender" to sender.name)
                        } else {
                            send(sender, EconomyMessages.PAY_FAILED)
                            // Liberar en caso de fallo
                            TransactionLockManager.release(sender.uniqueId)
                            TransactionLockManager.release(targetPlayer.uniqueId)
                        }
                    }
                }
        )
    }

    private fun registerAdminEcoCommands() {
        // /eco <give/take/set> <player> <amount> <currency>
        val ecoBuilder = manager.commandBuilder("eco")
            .permission("core.economy.admin")

        // 1. /eco give
        manager.command(
            ecoBuilder.literal("give")
                .required("player", stringParser())
                .required("amount", stringParser())
                .required("currency", stringParser())
                .handler { context ->
                    val sender = context.sender()
                    val targetName = context.get<String>("player")
                    val amountStr = context.get<String>("amount")
                    val currencyId = context.get<String>("currency")

                    val currency = economyService.currencies[currencyId]
                    if (currency == null) {
                        send(sender, EconomyMessages.CURRENCY_NOT_FOUND, "currency" to currencyId)
                        return@handler
                    }

                    val amount = economyService.parseShorthand(amountStr)
                    if (amount == null || amount <= BigDecimal.ZERO) {
                        send(sender, EconomyMessages.INVALID_ADMIN_AMOUNT)
                        return@handler
                    }

                    val targetPlayer = Bukkit.getPlayer(targetName)
                    val targetOffline = if (targetPlayer == null) Bukkit.getOfflinePlayer(targetName) else null
                    val uuid = targetPlayer?.uniqueId ?: targetOffline?.uniqueId

                    if (uuid == null) {
                        send(sender, EconomyMessages.PLAYER_NOT_FOUND)
                        return@handler
                    }

                    economyService.modifyBalance(uuid, currency.id, amount, "ADMIN_GIVE").thenAccept { success ->
                        if (success) {
                            val formatted = economyService.formatBalance(currency.id, amount)
                            send(sender, EconomyMessages.ECO_GIVE_SENDER, "amount" to formatted, "target" to targetName)
                            if (targetPlayer != null) {
                                send(targetPlayer, EconomyMessages.ECO_GIVE_RECEIVER, "amount" to formatted)
                            }
                            FancyLogger.logAdminAction("ECONOMY", "El administrador ${sender.name} ha dado $formatted a ${targetName}.")
                        } else {
                            send(sender, EconomyMessages.ECO_GIVE_FAILED)
                        }
                    }
                }
        )

        // 2. /eco take
        manager.command(
            ecoBuilder.literal("take")
                .required("player", stringParser())
                .required("amount", stringParser())
                .required("currency", stringParser())
                .handler { context ->
                    val sender = context.sender()
                    val targetName = context.get<String>("player")
                    val amountStr = context.get<String>("amount")
                    val currencyId = context.get<String>("currency")

                    val currency = economyService.currencies[currencyId]
                    if (currency == null) {
                        send(sender, EconomyMessages.CURRENCY_NOT_FOUND, "currency" to currencyId)
                        return@handler
                    }

                    val amount = economyService.parseShorthand(amountStr)
                    if (amount == null || amount <= BigDecimal.ZERO) {
                        send(sender, EconomyMessages.INVALID_ADMIN_AMOUNT)
                        return@handler
                    }

                    val targetPlayer = Bukkit.getPlayer(targetName)
                    val targetOffline = if (targetPlayer == null) Bukkit.getOfflinePlayer(targetName) else null
                    val uuid = targetPlayer?.uniqueId ?: targetOffline?.uniqueId

                    if (uuid == null) {
                        send(sender, EconomyMessages.PLAYER_NOT_FOUND)
                        return@handler
                    }

                    economyService.modifyBalance(uuid, currency.id, amount.negate(), "ADMIN_TAKE").thenAccept { success ->
                        if (success) {
                            val formatted = economyService.formatBalance(currency.id, amount)
                            send(sender, EconomyMessages.ECO_TAKE_SENDER, "amount" to formatted, "target" to targetName)
                            if (targetPlayer != null) {
                                send(targetPlayer, EconomyMessages.ECO_TAKE_RECEIVER, "amount" to formatted)
                            }
                            FancyLogger.logAdminAction("ECONOMY", "El administrador ${sender.name} ha retirado $formatted a ${targetName}.")
                        } else {
                            send(sender, EconomyMessages.ECO_TAKE_FAILED)
                        }
                    }
                }
        )

        // 3. /eco set
        manager.command(
            ecoBuilder.literal("set")
                .required("player", stringParser())
                .required("amount", stringParser())
                .required("currency", stringParser())
                .handler { context ->
                    val sender = context.sender()
                    val targetName = context.get<String>("player")
                    val amountStr = context.get<String>("amount")
                    val currencyId = context.get<String>("currency")

                    val currency = economyService.currencies[currencyId]
                    if (currency == null) {
                        send(sender, EconomyMessages.CURRENCY_NOT_FOUND, "currency" to currencyId)
                        return@handler
                    }

                    val amount = economyService.parseShorthand(amountStr)
                    if (amount == null || amount < BigDecimal.ZERO) {
                        send(sender, EconomyMessages.INVALID_ADMIN_AMOUNT)
                        return@handler
                    }

                    val targetPlayer = Bukkit.getPlayer(targetName)
                    val targetOffline = if (targetPlayer == null) Bukkit.getOfflinePlayer(targetName) else null
                    val uuid = targetPlayer?.uniqueId ?: targetOffline?.uniqueId

                    if (uuid == null) {
                        send(sender, EconomyMessages.PLAYER_NOT_FOUND)
                        return@handler
                    }

                    // Para ejecutar SET en modifyBalance calculamos la diferencia
                    taskScheduler.runAsync {
                        val current = economyService.getBalance(uuid, currency.id)
                        val diff = amount.subtract(current)
                        economyService.modifyBalance(uuid, currency.id, diff, "ADMIN_SET").thenAccept { success ->
                            if (success) {
                                val formatted = economyService.formatBalance(currency.id, amount)
                                send(sender, EconomyMessages.ECO_SET_SENDER, "amount" to formatted, "target" to targetName)
                                if (targetPlayer != null) {
                                    send(targetPlayer, EconomyMessages.ECO_SET_RECEIVER, "amount" to formatted)
                                }
                                FancyLogger.logAdminAction("ECONOMY", "El administrador ${sender.name} ha establecido el saldo de ${targetName} en $formatted.")
                            } else {
                                send(sender, EconomyMessages.ECO_SET_FAILED)
                            }
                        }
                    }
                }
        )
    }

    private fun registerDynamicCurrencyCommands() {
        for (currency in economyService.currencies.values) {
            for (alias in currency.commands) {
                // Registrar alias dinámico
                manager.command(
                    manager.commandBuilder(alias)
                        .optional("player", stringParser())
                        .permission("core.economy.wallet")
                        .handler { context ->
                            val sender = context.sender()
                            val targetName = context.optional<String>("player").orElse(null)

                            if (targetName != null) {
                                if (!sender.hasPermission("core.economy.wallet.other")) {
                                    send(sender, EconomyMessages.ALIAS_NO_PERMISSION_OTHER)
                                    return@handler
                                }
                                val targetPlayer = Bukkit.getPlayer(targetName)
                                val targetOffline = if (targetPlayer == null) Bukkit.getOfflinePlayer(targetName) else null
                                val uuid: UUID = targetPlayer?.uniqueId ?: targetOffline!!.uniqueId
                                val name: String = targetPlayer?.name ?: (targetOffline?.name ?: targetName)

                                showSingleBalance(sender, uuid, name, currency)
                            } else {
                                if (sender !is Player) {
                                    send(sender, EconomyMessages.ALIAS_USAGE, "alias" to alias)
                                    return@handler
                                }
                                showSingleBalance(sender, sender.uniqueId, sender.name, currency)
                            }
                        }
                )
            }
        }
    }

    private fun showSingleBalance(sender: CommandSender, uuid: UUID, targetName: String, currency: CurrencyConfig) {
        if (currency.permissionRequired != null && !sender.hasPermission(currency.permissionRequired)) {
            send(sender, EconomyMessages.NO_PERMISSION_CURRENCY)
            return
        }
        taskScheduler.runAsync {
            val bal = economyService.getBalance(uuid, currency.id)
            val formatted = economyService.formatBalance(currency.id, bal)
            send(sender, EconomyMessages.BALANCE_DISPLAY, "player" to targetName, "currency" to currency.displayName, "amount" to formatted)
        }
    }
}
