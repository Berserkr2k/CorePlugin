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
    ),
    val utility: Map<String, String> = mapOf(
        "fly-enabled" to "<green>Modo de vuelo habilitado.</green>",
        "fly-disabled" to "<red>Modo de vuelo deshabilitado.</red>",
        "fly-enabled-other" to "<green>Modo de vuelo habilitado para <player>.</green>",
        "fly-disabled-other" to "<red>Modo de vuelo deshabilitado para <player>.</red>",
        "fly-world-not-allowed" to "<red>El vuelo no está permitido en este mundo.</red>",
        "fly-world-left" to "<red>Has salido de un mundo permitido para volar. Modo de vuelo desactivado.</red>",
        
        "speed-invalid" to "<red>Velocidad inválida. Debe ser un número entre 1 y 10.</red>",
        "speed-fly-set" to "<green>Velocidad de vuelo establecida en <speed>.</green>",
        "speed-walk-set" to "<green>Velocidad de caminata establecida en <speed>.</green>",
        "speed-fly-set-other" to "<green>Velocidad de vuelo de <player> establecida en <speed>.</green>",
        "speed-walk-set-other" to "<green>Velocidad de caminata de <player> establecida en <speed>.</green>",
        
        "hat-equipped" to "<green>¡Te has puesto el ítem en la cabeza!</green>",
        "hat-empty" to "<red>Debes tener un ítem en la mano para usarlo de sombrero.</red>",
        
        "feed-success" to "<green>¡Tu apetito ha sido saciado!</green>",
        "feed-success-other" to "<green>¡Has saciado el apetito de <player>!</green>",
        "feed-success-by-admin" to "<green>¡Un administrador ha saciado tu apetito!</green>",
        
        "heal-success" to "<green>¡Has sido curado y purificado!</green>",
        "heal-success-other" to "<green>¡Has curado y purificado a <player>!</green>",
        "heal-success-by-admin" to "<green>¡Un administrador te ha curado y purificado!</green>",
        
        "anvil-opened" to "<green>Abriendo yunque virtual...</green>",
        
        "enderchest-opened" to "<green>Abriendo tu cofre de ender...</green>",
        "enderchest-opened-other" to "<green>Abriendo cofre de ender de <player>...</green>",
        
        "exp-get" to "<gray>Experiencia de <white><player></white>: </gray><green><level> niveles</green> <gray>(<xp> XP totales)</gray>",
        "exp-set" to "<green>Has establecido la experiencia de <player> en <amount> XP.</green>",
        "exp-give" to "<green>Has dado <amount> XP a <player>.</green>",
        "exp-reset" to "<green>Has restablecido la experiencia de <player> a 0.</green>",
        "exp-set-by-admin" to "<green>Tu experiencia ha sido establecida en <amount> XP.</green>",
        "exp-give-by-admin" to "<green>Has recibido <amount> XP.</green>",
        "exp-reset-by-admin" to "<red>Tu experiencia ha sido restablecida a 0.</red>",
        
        "only-players" to "<red>Solo jugadores pueden ejecutar este comando.</red>",
        "no-permission-other" to "<red>No tienes permiso para aplicar esto a otros jugadores.</red>",
        "player-not-found" to "<red>El jugador especificado no existe o está desconectado.</red>",
        
        "speed-reset" to "<green>Tu velocidad de vuelo y caminata ha sido restablecida a los valores por defecto.</green>",
        "speed-reset-other" to "<green>Velocidad de vuelo y caminata de <player> restablecida a los valores por defecto.</green>",
        
        "broadcast-usage" to "<red>Uso: /broadcast <chat/title/actionbar/bossbar> <mensaje></red>",
        "broadcast-invalid-type" to "<red>Tipo de broadcast inválido. Tipos válidos: chat, title, actionbar, bossbar</red>",
        "sendtitle-usage" to "<red>Uso: /sendtitle <player> <titulo>[|subtitulo]</red>"
    )
)
