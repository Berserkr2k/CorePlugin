package com.github.berserkr2k.coreplugin.api.framework.command

import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.bukkit.command.CommandSender

interface CommandService {
    val manager: LegacyPaperCommandManager<CommandSender>
}
