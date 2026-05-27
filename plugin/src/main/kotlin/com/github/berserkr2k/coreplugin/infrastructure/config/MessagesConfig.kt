package com.github.berserkr2k.coreplugin.infrastructure.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MessagesConfig(
    val leaderboards: Map<String, String> = mapOf(
        "loading" to "<gold>Cargando datos del podio...</gold>",
        "header" to "<gold><bold>★ CLASIFICACIÓN DE <top_id> ★</bold></gold>\n",
        "row-format" to "<yellow>#<pos></yellow> <gray><player></gray> » <green>$<balance></green>"
    ),
    val economy: Map<String, String> = mapOf(
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
)
