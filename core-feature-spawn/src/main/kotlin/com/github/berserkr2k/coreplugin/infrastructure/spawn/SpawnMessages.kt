package com.github.berserkr2k.coreplugin.infrastructure.spawn

import com.github.berserkr2k.coreplugin.api.core.message.MessageKey

enum class SpawnMessages(override val path: String) : MessageKey {
    NOT_CONFIGURED("not-configured"),
    SET_SUCCESS("set-success"),
    SUCCESS("success"),
    WARMUP("warmup"),
    CANCELLED_MOVEMENT("cancelled-movement"),
    CANCELLED_DAMAGE("cancelled-damage"),
    FAILURE("failure");

    override val feature: String = "spawn"

    companion object {
        val defaults = mapOf(
            "not-configured" to "<red>❌ El spawn no está configurado o el mundo de destino no existe.</red>",
            "set-success" to "<green>✔ Spawn del servidor establecido correctamente en tu ubicación y guardado en config.</green>",
            "success" to "<green>✔ ¡Teletransportado al spawn con éxito!</green>",
            "warmup" to "<yellow>⚡ Teletransportándote al spawn en <time> segundos... ¡No te muevas ni recibas daño!</yellow>",
            "cancelled-movement" to "<red>❌ Teletransportación cancelada por movimiento.</red>",
            "cancelled-damage" to "<red>❌ Teletransportación cancelada por daño recibido.</red>",
            "failure" to "<red>❌ No se pudo realizar la teletransportación.</red>"
        )
    }
}
