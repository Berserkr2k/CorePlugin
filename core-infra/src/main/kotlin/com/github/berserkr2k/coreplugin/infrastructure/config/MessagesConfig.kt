package com.github.berserkr2k.coreplugin.infrastructure.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MessagesConfig(
    val leaderboards: Map<String, String> = mapOf(
        "loading" to "<gold>Cargando datos del podio...</gold>",
        "vacant" to "<gray>#<pos> - Vacante</gray>"
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
        "sendtitle-usage" to "<red>Uso: /sendtitle <player> <titulo>[|subtitulo]</red>",
        "chair-occupied" to "<red>❌ ¡Esta silla ya está ocupada!</red>",
        "chair-unsafe" to "<red>❌ No puedes sentarte aquí (espacio obstruido o inseguro).</red>"
    ),
    val shops: Map<String, String> = mapOf(
        "locked" to "<red>⚠️ Por favor espera a que se complete tu transacción anterior.</red>",
        "no-space" to "<red>❌ No tienes espacio libre en tu inventario.</red>",
        "no-space-qty" to "<red>❌ No tienes suficiente espacio para esta compra (<qty>).</red>",
        "no-funds" to "<red>❌ No tienes suficientes fondos.</red>",
        "no-items" to "<red>❌ No tienes este ítem en tu inventario.</red>",
        "no-items-qty" to "<red>❌ No tienes suficientes unidades para vender (<qty>).</red>",
        "buy-success" to "<green>✔ ¡Compra exitosa! Comprado x<qty> por <price>.</green>",
        "sell-success" to "<green>✔ ¡Venta exitosa! Vendido x<qty> por <payout>.</green>",
        "error-db" to "<red>❌ Ocurrió un error al procesar tu transacción. Revertido.</red>",
        "market-regulating" to "<red>❌ El mercado se está regulando, por favor espera unos segundos...</red>",
        "category-not-found" to "<red>❌ La categoría de tienda '<category>' no existe.</red>",
        "category-usage" to "<gray>Usa /shop para ver las categorías disponibles.</gray>",
        "history-error" to "<red>❌ Ocurrió un error al cargar tu historial de transacciones.</red>",
        "only-players" to "<red>❌ Solo los jugadores pueden abrir las tiendas.</red>",
        "back-item-material" to "BARRIER",
        "back-item-name" to "<red>Volver</red>",
        "back-item-lore" to "<gray>Haz clic para regresar</gray>",
        "buy-price-format" to "<gray>Precio Compra: <green><price></green>",
        "buy-tax-format" to "<dark_gray>  (IVA incl.: <tax>)</dark_gray>",
        "buy-disabled" to "<red>No se puede comprar</red>",
        "sell-price-format" to "<gray>Precio Venta:  <red><price></red>",
        "sell-tax-format" to "<dark_gray>  (IVA incl.: <tax>)</dark_gray>",
        "sell-disabled" to "<red>No se puede vender</red>",
        "lore-separator" to "<gray>──────────────────────────────</gray>",
        "lore-volume" to "<gray>Volumen (24h): <gold><volume> uds.</gold>",
        "lore-trend" to "<gray>Tendencia:     <trend>",
        "lore-click" to "<yellow>▶ Haz clic para transaccionar</yellow>"
    ),
    val chat: Map<String, String> = mapOf(
        "socialspy-enabled" to "<green>SocialSpy habilitado.</green>",
        "socialspy-disabled" to "<red>SocialSpy deshabilitado.</red>",
        "pm-usage" to "<red>Uso: /msg <jugador> <mensaje></red>",
        "pm-player-not-found" to "<red>Jugador no encontrado o desconectado.</red>",
        "pm-cannot-msg-self" to "<red>No puedes enviarte mensajes privados a ti mismo.</red>",
        "reply-usage" to "<red>Uso: /reply <mensaje></red>",
        "reply-no-target" to "<red>No tienes a nadie a quien responder.</red>",
        "color-changed" to "<green>¡Tu color de chat ha sido cambiado a <color>!</green>",
        "color-menu-title" to "<dark_gray><bold>Selecciona un Color</bold></dark_gray>",
        "pm-sent-format" to "<gray>[Yo -> <target>]: <message></gray>",
        "pm-received-format" to "<gray>[<sender> -> Yo]: <message></gray>",
        "pm-socialspy-format" to "<dark_gray>[Spy] [<sender> -> <target>]: <message></dark_gray>"
    ),
    val warps: Map<String, String> = mapOf(
        "set" to "<green>¡Warp '<name>' establecido correctamente en tu posición!</green>",
        "deleted" to "<red>¡Warp '<name>' eliminado correctamente!</red>",
        "not-found" to "<red>El warp '<name>' no existe.</red>",
        "success" to "<green>¡Teletransportado al warp '<name>' con éxito!</green>",
        "no-permission" to "<red>No tienes permiso para usar este warp.</red>",
        "warmup" to "<yellow>Teletransportándote en <time> segundos... ¡No te muevas ni recibas daño!</yellow>",
        "cooldown-active" to "<red>Debes esperar <time> segundos antes de volver a usar este warp.</red>",
        "cancelled-movement" to "<red>Teletransportación cancelada por movimiento.</red>",
        "cancelled-damage" to "<red>Teletransportación cancelada por daño recibido.</red>",
        "menu-title" to "<dark_gray>Puntos de Teletransporte</dark_gray>"
    )
)

/**
 * Obtiene y formatea un mensaje del mapa de economía reemplazando marcadores <key>.
 */
fun MessagesConfig.getEconomy(key: String, vararg placeholders: Pair<String, Any>): String {
    var msg = this.economy[key] ?: ""
    for (ph in placeholders) {
        msg = msg.replace("<${ph.first}>", ph.second.toString())
    }
    return msg
}

/**
 * Obtiene y formatea un mensaje del mapa de tiendas reemplazando marcadores <key>.
 */
fun MessagesConfig.getShops(key: String, vararg placeholders: Pair<String, Any>): String {
    var msg = this.shops[key] ?: ""
    for (ph in placeholders) {
        msg = msg.replace("<${ph.first}>", ph.second.toString())
    }
    return msg
}

/**
 * Obtiene y formatea un mensaje del mapa de utilidades reemplazando marcadores <key>.
 */
fun MessagesConfig.getUtility(key: String, vararg placeholders: Pair<String, Any>): String {
    var msg = this.utility[key] ?: ""
    for (ph in placeholders) {
        msg = msg.replace("<${ph.first}>", ph.second.toString())
    }
    return msg
}

/**
 * Obtiene y formatea un mensaje del mapa de chat reemplazando marcadores <key>.
 */
fun MessagesConfig.getChat(key: String, vararg placeholders: Pair<String, Any>): String {
    var msg = this.chat[key] ?: ""
    for (ph in placeholders) {
        msg = msg.replace("<${ph.first}>", ph.second.toString())
    }
    return msg
}

/**
 * Obtiene y formatea un mensaje del mapa de warps reemplazando marcadores <key>.
 */
fun MessagesConfig.getWarps(key: String, vararg placeholders: Pair<String, Any>): String {
    var msg = this.warps[key] ?: ""
    for (ph in placeholders) {
        msg = msg.replace("<${ph.first}>", ph.second.toString())
    }
    return msg
}


