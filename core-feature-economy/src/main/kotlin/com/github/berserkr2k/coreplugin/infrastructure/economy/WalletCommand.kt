package com.github.berserkr2k.coreplugin.infrastructure.economy

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import net.kyori.adventure.text.minimessage.MiniMessage
import org.incendo.cloud.parser.standard.StringParser.stringParser
import com.github.berserkr2k.coreplugin.infrastructure.config.MessagesConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.getEconomy
import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.common.FancyLogger
import com.github.berserkr2k.coreplugin.common.TransactionLockManager
import java.math.BigDecimal
import java.util.UUID

class WalletCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val economyService: EconomyService,
    private val messagesConfig: MessagesConfig
) {

    init {
        registerWalletCommands()
        registerPayCommand()
        registerAdminEcoCommands()
        registerDynamicCurrencyCommands()
    }

    private fun getMsg(key: String, vararg placeholders: Pair<String, Any>): String {
        return messagesConfig.getEconomy(key, *placeholders)
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
                        sender.sendMessage(ColorUtility.parse(getMsg("no-permission-other")))
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
                        sender.sendMessage(ColorUtility.parse(getMsg("usage-wallet")))
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
        Bukkit.getAsyncScheduler().runNow(plugin) { _ ->
            // Cargar el formato del header de messages.conf
            val headerTemplate = messagesConfig.leaderboards["header"] ?: "<gold><bold>★ BILLETERA DE <top_id> ★</bold></gold>\n"
            val builder = StringBuilder(headerTemplate.replace("<top_id>", targetName))
            var hasAny = false

            for (currency in economyService.currencies.values) {
                // Verificar permiso de divisa si aplica
                if (currency.permissionRequired != null && !sender.hasPermission(currency.permissionRequired)) {
                    continue
                }
                val bal = economyService.getBalance(uuid, currency.id)
                val formatted = economyService.formatBalance(currency.id, bal)
                builder.append("<gray> - ${currency.displayName}: </gray><white>$formatted</white>\n")
                hasAny = true
            }

            if (!hasAny) {
                sender.sendMessage(ColorUtility.parse(getMsg("no-permission-view")))
            } else {
                sender.sendMessage(ColorUtility.parse(builder.toString().trimEnd()))
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
                        context.sender().sendMessage(ColorUtility.parse(getMsg("only-players-pay")))
                        return@handler
                    }

                    val targetName = context.get<String>("player")
                    val amountStr = context.get<String>("amount")
                    val currencyId = context.optional<String>("currency").orElse("credits") // credits por defecto

                    val currency = economyService.currencies[currencyId]
                    if (currency == null) {
                        sender.sendMessage(ColorUtility.parse(getMsg("currency-not-found", "currency" to currencyId)))
                        return@handler
                    }

                    if (!currency.p2pEnabled) {
                        sender.sendMessage(ColorUtility.parse(getMsg("p2p-disabled")))
                        return@handler
                    }

                    val rawAmount = economyService.parseShorthand(amountStr)
                    if (rawAmount == null || rawAmount <= BigDecimal.ZERO) {
                        sender.sendMessage(ColorUtility.parse(getMsg("invalid-amount")))
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
                        sender.sendMessage(ColorUtility.parse(getMsg("min-transfer", "min_transfer" to formattedMin)))
                        return@handler
                    }

                    val targetPlayer = Bukkit.getPlayer(targetName)
                    if (targetPlayer == null || !targetPlayer.isOnline) {
                        sender.sendMessage(ColorUtility.parse(getMsg("target-offline")))
                        return@handler
                    }

                    if (targetPlayer.uniqueId == sender.uniqueId) {
                        sender.sendMessage(ColorUtility.parse(getMsg("cannot-pay-self")))
                        return@handler
                    }

                    // Adquirir Bloqueos en Hilo Principal de forma atómica para prevenir Doble Gasto concurrente
                    if (!TransactionLockManager.acquire(sender.uniqueId)) {
                        sender.sendMessage(ColorUtility.parse(getMsg("transaction-in-progress-self")))
                        return@handler
                    }
                    if (!TransactionLockManager.acquire(targetPlayer.uniqueId)) {
                        TransactionLockManager.release(sender.uniqueId)
                        sender.sendMessage(ColorUtility.parse(getMsg("transaction-in-progress-target")))
                        return@handler
                    }

                    // Ejecutar transacción SQL P2P asíncrona de forma segura
                    economyService.transferP2P(sender.uniqueId, targetPlayer.uniqueId, currency.id, amount).thenAccept { success ->
                        if (success) {
                            val formatted = economyService.formatBalance(currency.id, amount)
                            sender.sendMessage(ColorUtility.parse(getMsg("pay-success-sender", "amount" to formatted, "target" to targetPlayer.name)))
                            targetPlayer.sendMessage(ColorUtility.parse(getMsg("pay-success-receiver", "amount" to formatted, "sender" to sender.name)))
                        } else {
                            sender.sendMessage(ColorUtility.parse(getMsg("pay-failed")))
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
                        sender.sendMessage(ColorUtility.parse(getMsg("currency-not-found", "currency" to currencyId)))
                        return@handler
                    }

                    val amount = economyService.parseShorthand(amountStr)
                    if (amount == null || amount <= BigDecimal.ZERO) {
                        sender.sendMessage(ColorUtility.parse(getMsg("invalid-admin-amount")))
                        return@handler
                    }

                    val targetPlayer = Bukkit.getPlayer(targetName)
                    val targetOffline = if (targetPlayer == null) Bukkit.getOfflinePlayer(targetName) else null
                    val uuid = targetPlayer?.uniqueId ?: targetOffline?.uniqueId

                    if (uuid == null) {
                        sender.sendMessage(ColorUtility.parse(getMsg("player-not-found")))
                        return@handler
                    }

                    economyService.modifyBalance(uuid, currency.id, amount, "ADMIN_GIVE").thenAccept { success ->
                        if (success) {
                            val formatted = economyService.formatBalance(currency.id, amount)
                            sender.sendMessage(ColorUtility.parse(getMsg("eco-give-sender", "amount" to formatted, "target" to targetName)))
                            targetPlayer?.sendMessage(ColorUtility.parse(getMsg("eco-give-receiver", "amount" to formatted)))
                            FancyLogger.logAdminAction("ECONOMY", "El administrador ${sender.name} ha dado $formatted a ${targetName}.")
                        } else {
                            sender.sendMessage(ColorUtility.parse(getMsg("eco-give-failed")))
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
                        sender.sendMessage(ColorUtility.parse(getMsg("currency-not-found", "currency" to currencyId)))
                        return@handler
                    }

                    val amount = economyService.parseShorthand(amountStr)
                    if (amount == null || amount <= BigDecimal.ZERO) {
                        sender.sendMessage(ColorUtility.parse(getMsg("invalid-admin-amount")))
                        return@handler
                    }

                    val targetPlayer = Bukkit.getPlayer(targetName)
                    val targetOffline = if (targetPlayer == null) Bukkit.getOfflinePlayer(targetName) else null
                    val uuid = targetPlayer?.uniqueId ?: targetOffline?.uniqueId

                    if (uuid == null) {
                        sender.sendMessage(ColorUtility.parse(getMsg("player-not-found")))
                        return@handler
                    }

                    economyService.modifyBalance(uuid, currency.id, amount.negate(), "ADMIN_TAKE").thenAccept { success ->
                        if (success) {
                            val formatted = economyService.formatBalance(currency.id, amount)
                            sender.sendMessage(ColorUtility.parse(getMsg("eco-take-sender", "amount" to formatted, "target" to targetName)))
                            targetPlayer?.sendMessage(ColorUtility.parse(getMsg("eco-take-receiver", "amount" to formatted)))
                            FancyLogger.logAdminAction("ECONOMY", "El administrador ${sender.name} ha retirado $formatted a ${targetName}.")
                        } else {
                            sender.sendMessage(ColorUtility.parse(getMsg("eco-take-failed")))
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
                        sender.sendMessage(ColorUtility.parse(getMsg("currency-not-found", "currency" to currencyId)))
                        return@handler
                    }

                    val amount = economyService.parseShorthand(amountStr)
                    if (amount == null || amount < BigDecimal.ZERO) {
                        sender.sendMessage(ColorUtility.parse(getMsg("invalid-admin-amount")))
                        return@handler
                    }

                    val targetPlayer = Bukkit.getPlayer(targetName)
                    val targetOffline = if (targetPlayer == null) Bukkit.getOfflinePlayer(targetName) else null
                    val uuid = targetPlayer?.uniqueId ?: targetOffline?.uniqueId

                    if (uuid == null) {
                        sender.sendMessage(ColorUtility.parse(getMsg("player-not-found")))
                        return@handler
                    }

                    // Para ejecutar SET en modifyBalance calculamos la diferencia
                    Bukkit.getAsyncScheduler().runNow(plugin) { _ ->
                        val current = economyService.getBalance(uuid, currency.id)
                        val diff = amount.subtract(current)
                        economyService.modifyBalance(uuid, currency.id, diff, "ADMIN_SET").thenAccept { success ->
                            if (success) {
                                val formatted = economyService.formatBalance(currency.id, amount)
                                sender.sendMessage(ColorUtility.parse(getMsg("eco-set-sender", "amount" to formatted, "target" to targetName)))
                                targetPlayer?.sendMessage(ColorUtility.parse(getMsg("eco-set-receiver", "amount" to formatted)))
                                FancyLogger.logAdminAction("ECONOMY", "El administrador ${sender.name} ha establecido el saldo de ${targetName} en $formatted.")
                            } else {
                                sender.sendMessage(ColorUtility.parse(getMsg("eco-set-failed")))
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
                                    sender.sendMessage(ColorUtility.parse(getMsg("alias-no-permission-other")))
                                    return@handler
                                }
                                val targetPlayer = Bukkit.getPlayer(targetName)
                                val targetOffline = if (targetPlayer == null) Bukkit.getOfflinePlayer(targetName) else null
                                  val uuid: UUID = targetPlayer?.uniqueId ?: targetOffline!!.uniqueId
                                val name: String = targetPlayer?.name ?: (targetOffline?.name ?: targetName)

                                showSingleBalance(sender, uuid, name, currency)
                            } else {
                                if (sender !is Player) {
                                    sender.sendMessage(ColorUtility.parse(getMsg("alias-usage", "alias" to alias)))
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
            sender.sendMessage(ColorUtility.parse(getMsg("no-permission-currency")))
            return
        }
        Bukkit.getAsyncScheduler().runNow(plugin) { _ ->
            val bal = economyService.getBalance(uuid, currency.id)
            val formatted = economyService.formatBalance(currency.id, bal)
            sender.sendMessage(ColorUtility.parse(getMsg("balance-display", "player" to targetName, "currency" to currency.displayName, "amount" to formatted)))
        }
    }
}
