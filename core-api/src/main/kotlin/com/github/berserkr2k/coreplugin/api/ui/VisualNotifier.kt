package com.github.berserkr2k.coreplugin.api.ui

import org.bukkit.entity.Player

/**
 * Contrato base para enviar notificaciones visuales a los jugadores.
 * Independientemente de la versión del servidor, los mensajes de texto
 * utilizarán el formato moderno de MiniMessage (ej: "<red>¡Alerta!</red>").
 */
interface VisualNotifier {

    /**
     * Envía un mensaje en la barra de acción (justo encima del inventario).
     *
     * @param player El jugador que recibirá el mensaje.
     * @param message El texto en formato MiniMessage.
     */
    fun sendActionBar(player: Player, message: String)

    /**
     * Envía un título gigante en el centro de la pantalla.
     *
     * @param player El jugador que recibirá el título.
     * @param title El texto principal (MiniMessage). Usa un string vacío ("") si solo quieres el subtítulo.
     * @param subtitle El texto secundario (MiniMessage). Usa un string vacío ("") si solo quieres el título.
     * @param fadeInTicks Tiempo que tarda en aparecer (20 ticks = 1 segundo). Por defecto 10 (medio segundo).
     * @param stayTicks Tiempo que se queda congelado en pantalla. Por defecto 70 (3.5 segundos).
     * @param fadeOutTicks Tiempo que tarda en desvanecerse. Por defecto 20 (1 segundo).
     */
    fun sendTitle(
        player: Player,
        title: String,
        subtitle: String,
        fadeInTicks: Int = 10,
        stayTicks: Int = 70,
        fadeOutTicks: Int = 20
    )

    /**
     * Modifica la cabecera (arriba) y el pie (abajo) de la lista de jugadores (TAB).
     *
     * @param player El jugador que verá el cambio.
     * @param header Texto superior en formato MiniMessage. Usa "" para limpiar.
     * @param footer Texto inferior en formato MiniMessage. Usa "" para limpiar.
     */
    fun sendTabList(player: Player, header: String, footer: String)
}