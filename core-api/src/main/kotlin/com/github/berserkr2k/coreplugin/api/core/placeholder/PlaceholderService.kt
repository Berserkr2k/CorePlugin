package com.github.berserkr2k.coreplugin.api.core.placeholder

import org.bukkit.entity.Player
import net.kyori.adventure.text.Component

interface PlaceholderService {
    /**
     * Procesa y convierte texto heredado con variables en un componente Adventure de forma segura.
     */
    fun parseLegacyStringSecurely(player: Player, text: String): Component

    /**
     * Resuelve un String con placeholders.
     */
    fun parsePlaceholder(player: Player, text: String): String
}
