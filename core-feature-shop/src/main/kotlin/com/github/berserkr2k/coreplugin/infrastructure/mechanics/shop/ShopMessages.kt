package com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop

import com.github.berserkr2k.coreplugin.api.core.message.MessageKey

enum class ShopMessages(override val path: String) : MessageKey {
    LOCKED("locked"),
    NO_SPACE("no-space"),
    NO_SPACE_QTY("no-space-qty"),
    NO_FUNDS("no-funds"),
    NO_ITEMS("no-items"),
    NO_ITEMS_QTY("no-items-qty"),
    BUY_SUCCESS("buy-success"),
    SELL_SUCCESS("sell-success"),
    ERROR_DB("error-db"),
    MARKET_REGULATING("market-regulating"),
    CATEGORY_NOT_FOUND("category-not-found"),
    CATEGORY_USAGE("category-usage"),
    HISTORY_ERROR("history-error"),
    ONLY_PLAYERS("only-players"),
    RELOAD_STARTING("reload-starting"),
    RELOAD_SUCCESS("reload-success"),
    RELOAD_FAILED("reload-failed");

    override val feature: String = "shop"

    companion object {
        val defaults = mapOf(
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
            "reload-starting" to "<yellow>⏳ Recargando la configuración de tiendas...</yellow>",
            "reload-success" to "<green>✔ ¡Configuración de tiendas recargada con éxito!</green>",
            "reload-failed" to "<red>❌ Falló la recarga de tiendas: <error></red>"
        )
    }
}
