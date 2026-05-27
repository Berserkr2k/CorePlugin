package com.github.berserkr2k.coreplugin.admin

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AdminSessionManager {
    // Mapa seguro para hilos que guarda: <UUID del Admin, UUID del Jugador a editar>
    val pendingCoinEdits = ConcurrentHashMap<UUID, UUID>()
}