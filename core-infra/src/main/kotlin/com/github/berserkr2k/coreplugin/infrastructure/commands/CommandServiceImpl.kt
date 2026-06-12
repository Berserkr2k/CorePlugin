package com.github.berserkr2k.coreplugin.infrastructure.commands

import com.github.berserkr2k.coreplugin.api.framework.command.CommandService
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.bukkit.command.CommandSender

class CommandServiceImpl(
    override val manager: LegacyPaperCommandManager<CommandSender>
) : CommandService
