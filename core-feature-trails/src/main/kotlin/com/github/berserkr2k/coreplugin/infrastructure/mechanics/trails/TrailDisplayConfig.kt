package com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails

import com.github.berserkr2k.coreplugin.api.core.config.ConfigDefinition
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class TrailDisplayConfig(
    val schemaVersion: Int = 1,
    
    val statusEquipped: String = "<green>⭐ ¡Estela Equipada!</green>",
    val statusEquippedLore: String = "<gray>Tu proyectil ya tiene este efecto.</gray>",
    val statusSelect: String = "<yellow>⚡ Click para Equipar</yellow>",
    val statusLocked: String = "<red>❌ Bloqueado</red>",
    val statusLockedLore: String = "<gray>Requiere permiso: <red><permission></red></gray>",
    
    val clearName: String = "<red><bold>❌ Quitar Estela</bold></red>",
    val clearLore: List<String> = listOf(
        "<gray>Haz click aquí para remover tu</gray>",
        "<gray>estela de partículas activa.</gray>",
        "",
        "<yellow>⚡ Click para remover</yellow>"
    )
)

object TrailDisplayConfigDefinition : ConfigDefinition<TrailDisplayConfig> {
    override val fileName = "display.conf"
    override val schemaVersion = 1
    override val configType = TrailDisplayConfig::class.java
}
