package com.github.berserkr2k.coreplugin.infrastructure.kits

import com.github.berserkr2k.coreplugin.api.core.config.ConfigDefinition
import com.github.berserkr2k.coreplugin.api.framework.menu.MenuConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class KitsSelectorMenuConfig(
    val schemaVersion: Int = 1,
    val menu: MenuConfig = MenuConfig(
        title = "<gold><bold>Kits Disponibles</bold></gold>",
        size = 27,
        filler = com.github.berserkr2k.coreplugin.api.framework.menu.FillerConfig(
            enabled = true,
            item = com.github.berserkr2k.coreplugin.api.config.ItemConfig(material = "GRAY_STAINED_GLASS_PANE", displayName = " ")
        )
    ),
    
    val statusLocked: String = "<red>❌ Bloqueado (Requiere Rango)</red>",
    val statusCooldown: String = "<red>⏳ En Cooldown (Espera: <time>)</red>",
    val statusCost: String = "<gold>⚖ Costo: <cost> <currency></gold>",
    val statusBypass: String = "<green>✔ ¡Disponible! (<yellow>Bypass de Cooldown Activo</yellow>)</green>",
    val statusAvailable: String = "<green>✔ ¡Disponible para Reclamar!</green>",
    
    val actionClaim: String = "<yellow>⚡ Click Izquierdo para Reclamar</yellow>",
    val actionPreview: String = "<aqua>⚡ Click Derecho para Previsualizar</aqua>"
)

object KitsSelectorMenuConfigDefinition : ConfigDefinition<KitsSelectorMenuConfig> {
    override val fileName = "menus/selector.conf"
    override val schemaVersion = 1
    override val configType = KitsSelectorMenuConfig::class.java
}
