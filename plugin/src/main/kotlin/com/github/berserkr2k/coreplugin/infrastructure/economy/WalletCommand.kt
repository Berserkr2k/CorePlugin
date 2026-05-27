package com.github.berserkr2k.coreplugin.infrastructure.economy

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.incendo.cloud.CommandManager
import net.kyori.adventure.text.minimessage.MiniMessage
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.parser.standard.DoubleParser.doubleParser
import java.math.BigDecimal
import java.util.UUID

class WalletCommand(
    private val plugin: Plugin,
    private val manager: CommandManager<CommandSender>,
    private val economyService: EconomyService
) {
    private val miniMessage = MiniMessage.miniMessage()

    init {
        registerWalletCommands()
        registerPayCommand()
        registerAdminEcoCommands()
        registerDynamicCurrencyCommands()
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
                        sender.sendMessage(miniMessage.deserialize("<red>No tienes permiso para ver la billetera de otros jugadores.</red>"))
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
                        sender.sendMessage(miniMessage.deserialize("<red>Uso: /wallet <player></red>"))
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
            val builder = StringBuilder("<gold><bold>★ BILLETERA DE $targetName ★</bold></gold>\n")
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
                sender.sendMessage(miniMessage.deserialize("<red>No tienes permisos para ver ninguna divisa de esta billetera.</red>"))
            } else {
                sender.sendMessage(miniMessage.deserialize(builder.toString().trimEnd()))
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
                        context.sender().sendMessage(miniMessage.deserialize("<red>Solo jugadores pueden transferir dinero.</red>"))
                        return@handler
                    }

                    val targetName = context.get<String>("player")
                    val amountStr = context.get<String>("amount")
                    val currencyId = context.optional<String>("currency").orElse("credits") // credits por defecto

                    val currency = economyService.currencies[currencyId]
                    if (currency == null) {
                        sender.sendMessage(miniMessage.deserialize("<red>La divisa especificada '$currencyId' no existe.</red>"))
                        return@handler
                    }

                    if (!currency.p2pEnabled) {
                        sender.sendMessage(miniMessage.deserialize("<red>Las transferencias directas P2P están deshabilitadas para esta moneda.</red>"))
                        return@handler
                    }

                    val rawAmount = economyService.parseShorthand(amountStr)
                    if (rawAmount == null || rawAmount <= BigDecimal.ZERO) {
                        sender.sendMessage(miniMessage.deserialize("<red>Monto inválido especificado. Debe ser positivo (Ej: 100, 1.5k, 2m).</red>"))
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
                        sender.sendMessage(miniMessage.deserialize("<red>La transferencia mínima permitida es de ${economyService.formatBalance(currency.id, minTransfer)}.</red>"))
                        return@handler
                    }

                    val targetPlayer = Bukkit.getPlayer(targetName)
                    if (targetPlayer == null || !targetPlayer.isOnline) {
                        sender.sendMessage(miniMessage.deserialize("<red>El jugador destino debe estar conectado en el mismo servidor.</red>"))
                        return@handler
                    }

                    if (targetPlayer.uniqueId == sender.uniqueId) {
                        sender.sendMessage(miniMessage.deserialize("<red>No puedes transferirte dinero a ti mismo.</red>"))
                        return@handler
                    }

                    // Adquirir Bloqueos en Hilo Principal de forma atómica para prevenir Doble Gasto concurrente
                    if (!TransactionLockManager.acquire(sender.uniqueId)) {
                        sender.sendMessage(miniMessage.deserialize("<red>Hay otra transacción económica en curso para ti, por favor espera.</red>"))
                        return@handler
                    }
                    if (!TransactionLockManager.acquire(targetPlayer.uniqueId)) {
                        TransactionLockManager.release(sender.uniqueId)
                        sender.sendMessage(miniMessage.deserialize("<red>El jugador destino se encuentra procesando otra transacción, por favor espera.</red>"))
                        return@handler
                    }

                    // Ejecutar transacción SQL P2P asíncrona de forma segura
                    economyService.transferP2P(sender.uniqueId, targetPlayer.uniqueId, currency.id, amount).thenAccept { success ->
                        if (success) {
                            val formatted = economyService.formatBalance(currency.id, amount)
                            sender.sendMessage(miniMessage.deserialize("<green>¡Has enviado <white>$formatted</white> a <white>${targetPlayer.name}</white> con éxito!</green>"))
                            targetPlayer.sendMessage(miniMessage.deserialize("<green>¡Has recibido <white>$formatted</white> de <white>${sender.name}</white>!</green>"))
                        } else {
                            sender.sendMessage(miniMessage.deserialize("<red>Fondos insuficientes o se ha superado la capacidad máxima de la billetera destino.</red>"))
                            // Liberar en caso de fallo, transferP2P ya libera automáticamente en finally, pero por seguridad:
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
                        sender.sendMessage(miniMessage.deserialize("<red>La divisa especificada '$currencyId' no existe.</red>"))
                        return@handler
                    }

                    val amount = economyService.parseShorthand(amountStr)
                    if (amount == null || amount <= BigDecimal.ZERO) {
                        sender.sendMessage(miniMessage.deserialize("<red>Monto inválido especificado.</red>"))
                        return@handler
                    }

                    val targetPlayer = Bukkit.getPlayer(targetName)
                    val targetOffline = if (targetPlayer == null) Bukkit.getOfflinePlayer(targetName) else null
                    val uuid = targetPlayer?.uniqueId ?: targetOffline?.uniqueId

                    if (uuid == null) {
                        sender.sendMessage(miniMessage.deserialize("<red>El jugador especificado no existe.</red>"))
                        return@handler
                    }

                    economyService.modifyBalance(uuid, currency.id, amount, "ADMIN_GIVE").thenAccept { success ->
                        if (success) {
                            val formatted = economyService.formatBalance(currency.id, amount)
                            sender.sendMessage(miniMessage.deserialize("<green>Has dado <white>$formatted</white> a <white>$targetName</white>.</green>"))
                            targetPlayer?.sendMessage(miniMessage.deserialize("<green>Has recibido <white>$formatted</white> del Administrador.</green>"))
                        } else {
                            sender.sendMessage(miniMessage.deserialize("<red>Error al dar dinero. El saldo superaría el límite de la cuenta.</red>"))
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
                        sender.sendMessage(miniMessage.deserialize("<red>La divisa especificada '$currencyId' no existe.</red>"))
                        return@handler
                    }

                    val amount = economyService.parseShorthand(amountStr)
                    if (amount == null || amount <= BigDecimal.ZERO) {
                        sender.sendMessage(miniMessage.deserialize("<red>Monto inválido especificado.</red>"))
                        return@handler
                    }

                    val targetPlayer = Bukkit.getPlayer(targetName)
                    val targetOffline = if (targetPlayer == null) Bukkit.getOfflinePlayer(targetName) else null
                    val uuid = targetPlayer?.uniqueId ?: targetOffline?.uniqueId

                    if (uuid == null) {
                        sender.sendMessage(miniMessage.deserialize("<red>El jugador especificado no existe.</red>"))
                        return@handler
                    }

                    economyService.modifyBalance(uuid, currency.id, amount.negate(), "ADMIN_TAKE").thenAccept { success ->
                        if (success) {
                            val formatted = economyService.formatBalance(currency.id, amount)
                            sender.sendMessage(miniMessage.deserialize("<green>Has retirado <white>$formatted</white> a <white>$targetName</white>.</green>"))
                            targetPlayer?.sendMessage(miniMessage.deserialize("<red>Se te han retirado <white>$formatted</white> de tu saldo.</red>"))
                        } else {
                            sender.sendMessage(miniMessage.deserialize("<red>El jugador no posee fondos suficientes.</red>"))
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
                        sender.sendMessage(miniMessage.deserialize("<red>La divisa especificada '$currencyId' no existe.</red>"))
                        return@handler
                    }

                    val amount = economyService.parseShorthand(amountStr)
                    if (amount == null || amount < BigDecimal.ZERO) {
                        sender.sendMessage(miniMessage.deserialize("<red>Monto inválido especificado.</red>"))
                        return@handler
                    }

                    val targetPlayer = Bukkit.getPlayer(targetName)
                    val targetOffline = if (targetPlayer == null) Bukkit.getOfflinePlayer(targetName) else null
                    val uuid = targetPlayer?.uniqueId ?: targetOffline?.uniqueId

                    if (uuid == null) {
                        sender.sendMessage(miniMessage.deserialize("<red>El jugador especificado no existe.</red>"))
                        return@handler
                    }

                    // Para ejecutar SET en modifyBalance calculamos la diferencia
                    Bukkit.getAsyncScheduler().runNow(plugin) { _ ->
                        val current = economyService.getBalance(uuid, currency.id)
                        val diff = amount.subtract(current)
                        economyService.modifyBalance(uuid, currency.id, diff, "ADMIN_SET").thenAccept { success ->
                            if (success) {
                                val formatted = economyService.formatBalance(currency.id, amount)
                                sender.sendMessage(miniMessage.deserialize("<green>Has establecido el saldo de <white>$targetName</white> en <white>$formatted</white>.</green>"))
                                targetPlayer?.sendMessage(miniMessage.deserialize("<green>Tu saldo ha sido establecido en <white>$formatted</white>.</green>"))
                            } else {
                                sender.sendMessage(miniMessage.deserialize("<red>Error al establecer saldo. Fuera del límite máximo.</red>"))
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
                                    sender.sendMessage(miniMessage.deserialize("<red>No tienes permiso para ver el saldo de otros jugadores.</red>"))
                                    return@handler
                                }
                                val targetPlayer = Bukkit.getPlayer(targetName)
                                val targetOffline = if (targetPlayer == null) Bukkit.getOfflinePlayer(targetName) else null
                                val uuid: UUID = targetPlayer?.uniqueId ?: targetOffline!!.uniqueId
                                val name: String = targetPlayer?.name ?: (targetOffline?.name ?: targetName)

                                showSingleBalance(sender, uuid, name, currency)
                            } else {
                                if (sender !is Player) {
                                    sender.sendMessage(miniMessage.deserialize("<red>Uso: /$alias <player></red>"))
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
            sender.sendMessage(miniMessage.deserialize("<red>No tienes permiso para usar esta divisa.</red>"))
            return
        }
        Bukkit.getAsyncScheduler().runNow(plugin) { _ ->
            val bal = economyService.getBalance(uuid, currency.id)
            val formatted = economyService.formatBalance(currency.id, bal)
            sender.sendMessage(miniMessage.deserialize("<gray>Saldo de <white>$targetName</white> (${currency.displayName}): </gray><green>$formatted</green>"))
        }
    }
}
