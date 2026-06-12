package com.github.berserkr2k.coreplugin.infrastructure.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MessagesConfig(
    val leaderboards: Map<String, String> = mapOf(
        "only-players" to "<red>Solo jugadores pueden ejecutar este comando.</red>",
        "registered" to "<green>¡ArmorStand registrado con éxito como podio para '<id>' (Top <rank>)!</green>",
        "created" to "<green>¡Nuevo ArmorStand de podio creado para '<id>' (Top <rank>)!</green>",
        "deleted" to "<green>¡Podio para '<id>' (Top <rank>) eliminado con éxito!</green>",
        "not-found" to "<red>No se encontró ningún podio para la clasificación '<id>' con Rank <rank>.</red>",
        "reloaded" to "<green>✔ ¡Configuraciones de clasificaciones recargadas y actualizadas con éxito!</green>",
        "vara-recibida" to "<green>¡Has recibido la Vara de Edición!</green>",
        "copied" to "<green>✔ ¡Propiedades físicas y de pose copiadas al portapapeles!</green>",
        "no-clipboard" to "<red>❌ No tienes ajustes copiados en el portapapeles.</red>",
        "pasted-all" to "<green>✔ ¡Ajustes y equipamiento pegados con éxito!</green>",
        "pasted-pose" to "<green>✔ ¡Ajustes de pose aplicados! (Los objetos no se duplicaron por seguridad en Supervivencia)</green>",
        "write-name" to "<gold>✏ Escribe el nombre del ArmorStand en el chat (Soporta colores con &):</gold>",
        "scale-changed" to "<green>✔ Escala de ajuste cambiada a <scale>.</green>",
        "equip-synced" to "<green>✔ ¡El equipamiento del ArmorStand se ha sincronizado exitosamente!</green>",
        "pose-tool" to "<green>✔ ¡Recibiste la <gold>Herramienta de Pose</gold>! Ajusta libremente. Sneak + Click Derecho para volver.</green>",
        "loading" to "<gold>Cargando datos del podio...</gold>",
        "vacant" to "<gray>#<pos> - Vacante</gray>"
    ),
    val holograms: Map<String, String> = mapOf(
        "only-players" to "<red>Solo jugadores pueden ejecutar este comando.</red>",
        "created" to "<green>¡Holograma '<id>' creado con éxito en tu posición!</green>",
        "deleted" to "<green>¡Holograma '<id>' eliminado con éxito!</green>",
        "not-found" to "<red>No se encontró ningún holograma activo con el ID '<id>'.</red>",
        "list-empty" to "<yellow>No hay hologramas activos en el servidor.</yellow>",
        "list-header" to "<gold><bold>Hologramas Activos (<size>):</bold></gold>",
        "list-item" to " <gray>-</gray> <yellow><id></yellow> <gray>(Mundo: <world>, X: <x>, Y: <y>, Z: <z>)</gray>",
        "edit-success" to "<green>¡Holograma '<id>' editado con éxito!</green>",
        "move-success" to "<green>¡Holograma '<id>' trasladado a tu posición actual!</green>",
        "center-success" to "<green>¡Holograma '<id>' centrado en el bloque (X: <x>, Z: <z>)!</green>",
        "center-error" to "<red>Error al intentar trasladar el holograma '<id>'.</red>",
        "reload-start" to "<yellow>Recargando hologramas desde la configuración...</yellow>",
        "reload-success" to "<green>¡Todos los hologramas han sido recargados exitosamente!</green>",
        "reload-error" to "<red>Error crítico al recargar hologramas: <error></red>"
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
        "lore-click" to "<yellow>▶ Haz clic para transaccionar</yellow>",
        "quantity-gui-title" to "<dark_gray>Transacción Simétrica</dark_gray>",
        "buy-max-name" to "<green><bold>Comprar Máximo</bold></green>",
        "buy-max-lore" to "<gray>Llena tu inventario con este ítem.</gray>\n\n<gray>Cantidad a comprar: <gold><qty> uds.</gold></gray>\n<gray>Costo Estimado:    <green><price></green></gray>\n\n<yellow>▶ Haz clic para comprar</yellow>",
        "sell-all-name" to "<red><bold>Vender Todo</bold></red>",
        "sell-all-lore" to "<gray>Vacía tu inventario de este ítem.</gray>\n\n<gray>Cantidad a vender: <gold><qty> uds.</gold></gray>\n<gray>Valor Estimado:    <red><price></red></gray>\n\n<yellow>▶ Haz clic para vender</yellow>",
        "buy-qty-name" to "<green><bold>Comprar <qty></bold></green>",
        "buy-qty-lore" to "<gray>Compra una cantidad de <qty> unidades.</gray>\n\n<gray>Costo Total: <green><price></green></gray>\n\n<yellow>▶ Haz clic para comprar</yellow>",
        "sell-qty-name" to "<red><bold>Vender <qty></bold></red>",
        "sell-qty-lore" to "<gray>Vende una cantidad de <qty> unidades.</gray>\n\n<gray>Valor Total: <red><price></red></gray>\n\n<yellow>▶ Haz clic para vender</yellow>",
        "buy-disabled-name" to "<red>Compra Deshabilitada</red>",
        "sell-disabled-name" to "<red>Venta Deshabilitada</red>",
        "quantity-gui-background-material" to "GRAY_STAINED_GLASS_PANE",
        "quantity-gui-divisor-material" to "BLACK_STAINED_GLASS_PANE",
        "buy-qty-material" to "GREEN_STAINED_GLASS_PANE",
        "sell-qty-material" to "RED_STAINED_GLASS_PANE",
        "buy-max-material" to "EMERALD_BLOCK",
        "sell-all-material" to "REDSTONE_BLOCK",
        "disabled-material" to "RED_STAINED_GLASS_PANE"
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
        "pm-socialspy-format" to "<dark_gray>[Spy] [<sender> -> <target>]: <message></dark_gray>",
        "cooldown" to "<red>⚠️ Por favor espera <cooldown> segundos antes de enviar otro mensaje.</red>",
        "link-blocked" to "<red>❌ No está permitido enviar enlaces o URLs en el chat.</red>"
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
    ),
    val scoreboard: Map<String, String> = mapOf(
        "toggle-on" to "<green>✔ Scoreboard activado.</green>",
        "toggle-off" to "<red>❌ Scoreboard desactivado.</red>",
        "reloaded" to "<green>✔ Configuración de scoreboard recargada con éxito.</green>"
    ),
    val regions: Map<String, String> = mapOf(
        "selection-incomplete" to "<red>❌ Error: Selección incompleta. Usa la herramienta de selección.</red>",
        "compiling" to "<yellow>⚡ Compilando datos geométricos y guardando configuración...</yellow>",
        "created" to "<green>✔ Región '<id>' creada e inyectada con éxito.</green>",
        "flag-updated" to "<green>✔ Bandera '<flag>' de la región '<id>' establecida en <value>.</green>",
        "flag-removed" to "<green>✔ Bandera '<flag>' removida de la región '<id>'.</green>",
        "region-not-found" to "<red>❌ La región '<id>' no existe.</red>",
        "invalid-flag" to "<red>❌ La bandera '<flag>' es inválida.</red>",
        "invalid-value" to "<red>❌ Valor inválido. Usa: allow, deny o remove</red>",
        "no-break" to "<red>❌ No tienes permiso para romper bloques aquí.</red>",
        "no-place" to "<red>❌ No tienes permiso para colocar bloques aquí.</red>",
        "no-pvp" to "<red>❌ No se permite el PVP en esta región.</red>",
        "no-projectile-damage" to "<red>❌ No se permite el daño por proyectiles en esta región.</red>",
        "no-chest-access" to "<red>❌ No tienes permiso para abrir cofres aquí.</red>",
        "no-enderchest-access" to "<red>❌ No tienes permiso para abrir tu cofre de ender aquí.</red>",
        "no-anvil-use" to "<red>❌ No tienes permiso para usar yunques aquí.</red>",
        "no-enchanting-use" to "<red>❌ No tienes permiso para usar la mesa de encantamientos aquí.</red>",
        "no-redstone-interaction" to "<red>❌ No tienes permiso para usar mecanismos de redstone aquí.</red>",
        "no-interact" to "<red>❌ No tienes permiso para interactuar aquí.</red>",
        "no-item-drop" to "<red>❌ No tienes permiso para tirar objetos aquí.</red>",
        "no-elytra-usage" to "<red>❌ El uso de Elytra está denegado en esta región.</red>",
        "no-vehicle-usage" to "<red>❌ El uso de vehículos está denegado en esta región.</red>",
        "no-armor-stand-interaction" to "<red>❌ No tienes permiso para interactuar con soportes de armadura aquí.</red>",
        "no-entity-interaction" to "<red>❌ No tienes permiso para interactuar con entidades aquí.</red>",
        "no-container-interaction" to "<red>❌ No tienes permiso para usar contenedores aquí.</red>",
        "no-item-frame-interaction" to "<red>❌ No tienes permiso para interactuar con marcos de ítems aquí.</red>",
        "selection-pos1" to "<green>[!] Posición 1 establecida en <x>, <y>, <z>.</green>",
        "selection-pos2" to "<green>[!] Posición 2 establecida en <x>, <y>, <z>.</green>",
        "deleted" to "<green>✔ Región '<id>' eliminada de forma permanente.</green>",
        "reloaded" to "<green>[!] Configuración de regiones recargada de disco y Spatial Index reconstruido con éxito.</green>",
        "not-in-region" to "<red>❌ No te encuentras dentro de ninguna región registrada.</red>",
        "console-only-id" to "<red>❌ Debes especificar la id de la región desde la consola.</red>",
        "flags-list" to "<yellow>Banderas individuales:</yellow>\n<gray>- PVP, BLOCK_BREAK, BLOCK_PLACE, INTERACT, CHEST_ACCESS, ENDERCHEST_ACCESS, ANVIL_USE, ENCHANTING_USE\n- USE_WITHOUT_BREAK, BLOCK_PHYSICS, ITEM_DROP, ITEM_PICKUP, PROJECTILE_DAMAGE, PLAYER_COLLISION, MOB_TARGETING\n- LIQUID_FLOW, FALL_DAMAGE, ELYTRA_USAGE, REDSTONE_INTERACTION, VEHICLE_USAGE, EXP_GAIN, HUNGER_LOSS, HOSTILE_SPAWN, PASSIVE_SPAWN\n- ARMOR_STAND_INTERACTION, ENTITY_INTERACTION, CONTAINER_INTERACTION, ITEM_FRAME_INTERACTION</gray>\n<yellow>Categorías de Banderas:</yellow>\n<gray>- COMBAT_FLAGS, WORLD_FLAGS, INTERACTION_FLAGS, PLAYER_FLAGS, ENTITY_FLAGS</gray>",
        "region-info" to "<dark_gray>===========================================</dark_gray>\n<gold><bold>Región:</bold></gold> <yellow><id></yellow>\n<gray>Prioridad:</gray> <white><priority></white>\n<gray>Mundo:</gray> <white><world></white>\n<gray>Límites:</gray> <white>(<min_x>, <min_y>, <min_z>) -> (<max_x>, <max_y>, <max_z>)</white>\n<gray>Banderas Permitidas:</gray> <green><allow_flags></green>\n<gray>Banderas Denegadas:</gray> <red><deny_flags></red>\n<dark_gray>===========================================</dark_gray>",
        "debug-here-header" to "<yellow>Regiones activas aquí (Count: <count>):</yellow>",
        "debug-here-item" to "<gray>- <id> (Prio: <priority>)</gray>",
        "debug-flags" to "<yellow>Bypass general: <bypass>\nGameMode: <gamemode></yellow>"
    ),
    val spawn: Map<String, String> = mapOf(
        "not-configured" to "<red>❌ El spawn no está configurado o el mundo de destino no existe.</red>",
        "set-success" to "<green>✔ Spawn del servidor establecido correctamente en tu ubicación y guardado en config.</green>",
        "success" to "<green>✔ ¡Teletransportado al spawn con éxito!</green>",
        "warmup" to "<yellow>⚡ Teletransportándote al spawn en <time> segundos... ¡No te muevas ni recibas daño!</yellow>",
        "cancelled-movement" to "<red>❌ Teletransportación cancelada por movimiento.</red>",
        "cancelled-damage" to "<red>❌ Teletransportación cancelada por daño recibido.</red>",
        "failure" to "<red>❌ No se pudo realizar la teletransportación.</red>"
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

/**
 * Obtiene y formatea un mensaje del mapa de scoreboard reemplazando marcadores <key>.
 */
fun MessagesConfig.getScoreboard(key: String, vararg placeholders: Pair<String, Any>): String {
    var msg = this.scoreboard[key] ?: ""
    for (ph in placeholders) {
        msg = msg.replace("<${ph.first}>", ph.second.toString())
    }
    return msg
}

/**
 * Obtiene y formatea un mensaje del mapa de regiones reemplazando marcadores <key>.
 */
fun MessagesConfig.getRegions(key: String, vararg placeholders: Pair<String, Any>): String {
    var msg = this.regions[key] ?: ""
    for (ph in placeholders) {
        msg = msg.replace("<${ph.first}>", ph.second.toString())
    }
    return msg
}

/**
 * Obtiene y formatea un mensaje del mapa de spawn reemplazando marcadores <key>.
 */
fun MessagesConfig.getSpawn(key: String, vararg placeholders: Pair<String, Any>): String {
    var msg = this.spawn[key] ?: ""
    for (ph in placeholders) {
        msg = msg.replace("<${ph.first}>", ph.second.toString())
    }
    return msg
}


