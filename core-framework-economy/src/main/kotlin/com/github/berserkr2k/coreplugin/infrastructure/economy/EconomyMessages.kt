package com.github.berserkr2k.coreplugin.infrastructure.economy

import com.github.berserkr2k.coreplugin.api.core.message.MessageKey

enum class EconomyMessages(override val path: String) : MessageKey {
    NO_PERMISSION_OTHER("no-permission-other"),
    USAGE_WALLET("usage-wallet"),
    NO_PERMISSION_CURRENCY("no-permission-currency"),
    BALANCE_DISPLAY("balance-display"),
    NO_PERMISSION_VIEW("no-permission-view"),
    ONLY_PLAYERS_PAY("only-players-pay"),
    CURRENCY_NOT_FOUND("currency-not-found"),
    P2P_DISABLED("p2p-disabled"),
    INVALID_AMOUNT("invalid-amount"),
    MIN_TRANSFER("min-transfer"),
    TARGET_OFFLINE("target-offline"),
    CANNOT_PAY_SELF("cannot-pay-self"),
    TRANSACTION_IN_PROGRESS_SELF("transaction-in-progress-self"),
    TRANSACTION_IN_PROGRESS_TARGET("transaction-in-progress-target"),
    PAY_SUCCESS_SENDER("pay-success-sender"),
    PAY_SUCCESS_RECEIVER("pay-success-receiver"),
    PAY_FAILED("pay-failed"),
    INVALID_ADMIN_AMOUNT("invalid-admin-amount"),
    PLAYER_NOT_FOUND("player-not-found"),
    ECO_GIVE_SENDER("eco-give-sender"),
    ECO_GIVE_RECEIVER("eco-give-receiver"),
    ECO_GIVE_FAILED("eco-give-failed"),
    ECO_TAKE_SENDER("eco-take-sender"),
    ECO_TAKE_RECEIVER("eco-take-receiver"),
    ECO_TAKE_FAILED("eco-take-failed"),
    ECO_SET_SENDER("eco-set-sender"),
    ECO_SET_RECEIVER("eco-set-receiver"),
    ECO_SET_FAILED("eco-set-failed"),
    ALIAS_NO_PERMISSION_OTHER("alias-no-permission-other"),
    ALIAS_USAGE("alias-usage"),
    WALLET_HEADER("wallet-header");

    override val feature: String = "economy"

    companion object {
        val defaults = mapOf(
            "wallet-header" to "<gold><bold>★ BILLETERA DE <top_id> ★</bold></gold>\n",
            "no-permission-other" to "<red>No tienes permiso para ver la billetera de otros jugadores.</red>",
            "usage-wallet" to "<red>Uso: /wallet <player></red>",
            "no-permission-currency" to "<red>No tienes permiso para usar esta divisa.</red>",
            "balance-display" to "<gray>Saldo de <white><player></white> (<currency>): </gray><green><amount></green>",
            "no-permission-view" to "<red>No tienes permisos para ver ninguna divisa de esta billetera.</red>",
            "only-players-pay" to "<red>Solo jugadores pueden transferir dinero.</red>",
            "currency-not-found" to "<red>La divisa especificada '<currency>' no existe.</red>",
            "p2p-disabled" to "<red>Las transferencias directas P2P están deshabilitadas para esta moneda.</red>",
            "invalid-amount" to "<red>Monto inválido especificado. Debe ser positivo (Ej: 100, 1.5k, 2m).</red>",
            "min-transfer" to "<red>La transferencia mínima permitida es de <min_transfer>.</red>",
            "target-offline" to "<red>El jugador destino debe estar conectado en el mismo servidor.</red>",
            "cannot-pay-self" to "<red>No puedes transferirte dinero a ti mismo.</red>",
            "transaction-in-progress-self" to "<red>Hay otra transacción económica en curso para ti, por favor espera.</red>",
            "transaction-in-progress-target" to "<red>El jugador destino se encuentra procesando otra transacción, por favor espera.</red>",
            "pay-success-sender" to "<green>¡Has enviado <white><amount></white> a <white><target></white> con éxito!</green>",
            "pay-success-receiver" to "<green>¡Has recibido <white><amount></white> de <white><sender></white>!</green>",
            "pay-failed" to "<red>Fondos insuficientes o se ha superado la capacidad máxima de la billetera destino.</red>",
            "invalid-admin-amount" to "<red>Monto inválido especificado.</red>",
            "player-not-found" to "<red>El jugador especificado no existe.</red>",
            "eco-give-sender" to "<green>Has dado <white><amount></white> a <white><target></white>.</green>",
            "eco-give-receiver" to "<green>Has recibido <white><amount></white> del Administrador.</green>",
            "eco-give-failed" to "<red>Error al dar dinero. El saldo superaría el límite de la cuenta.</red>",
            "eco-take-sender" to "<green>Has retirado <white><amount></white> a <white><target></white>.</green>",
            "eco-take-receiver" to "<red>Se te han retirado <white><amount></white> de tu saldo.</red>",
            "eco-take-failed" to "<red>El jugador no posee fondos suficientes.</red>",
            "eco-set-sender" to "<green>Has establecido el saldo de <white><target></white> en <white><amount></white>.</green>",
            "eco-set-receiver" to "<green>Tu saldo ha sido establecido en <white><amount></white>.</green>",
            "eco-set-failed" to "<red>Error al establecer saldo. Fuera del límite máximo.</red>",
            "alias-no-permission-other" to "<red>No tienes permiso para ver el saldo de otros jugadores.</red>",
            "alias-usage" to "<red>Uso: /<alias> <player></red>"
        )
    }
}
