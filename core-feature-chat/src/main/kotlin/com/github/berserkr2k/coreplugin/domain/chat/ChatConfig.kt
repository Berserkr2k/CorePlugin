package com.github.berserkr2k.coreplugin.domain.chat

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class ChatConfig(
    @Setting("join-global-chat") val joinGlobalChat: Boolean = true,
    @Setting("private-message-format") val pmFormat: String = "<gold>[PM] <sender> -> <recipient>:</gold> <white><message>",
    @Setting("social-spy-format") val socialSpyFormat: String = "<red>[SPY] <sender> -> <recipient>:</red> <white><message>",
    val formats: Map<String, ChatFormatSection> = mapOf(
        "default" to ChatFormatSection(100, "<gray>[Jugador]</gray> ", "<player> » ", "<white>", listOf("<yellow>Rango default</yellow>", "<gray>Click para enviar mensaje privado</gray>"), "SUGGEST_COMMAND", "/msg <player> "),
        "admin" to ChatFormatSection(1, "<red><bold>[Admin]</bold></red> ", "<player> » ", "<red>", listOf("<red>Rango Administrador</red>", "<gray>Click para enviar mensaje privado</gray>"), "SUGGEST_COMMAND", "/msg <player> ")
    ),
    val filter: ChatFilterConfig = ChatFilterConfig()
) {
    @ConfigSerializable
    data class ChatFormatSection(
        val priority: Int = 100,
        val prefix: String = "",
        @Setting("name-format") val nameFormat: String = "<player>",
        @Setting("chat-color") val chatColor: String = "<white>",
        @Setting("tooltip-lines") val tooltipLines: List<String> = emptyList(),
        @Setting("click-action-type") val clickActionType: String = "SUGGEST_COMMAND",
        @Setting("click-action-value") val clickActionValue: String = ""
    )

    @ConfigSerializable
    data class ChatFilterConfig(
        val enabled: Boolean = true,
        @Setting("cooldown-seconds") val cooldownSeconds: Double = 1.0,
        @Setting("block-links") val blockLinks: Boolean = true,
        @Setting("prevent-caps") val preventCaps: Boolean = true,
        @Setting("caps-percentage") val capsPercentage: Int = 60,
        @Setting("caps-min-length") val capsMinLength: Int = 5,
        @Setting("blocked-words") val blockedWords: List<String> = listOf("ez", "hack", "noob"),
        @Setting("swear-replacement") val swearReplacement: String = "****"
    )
}
