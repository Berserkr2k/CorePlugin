package com.github.berserkr2k.coreplugin.platform.paper

import com.github.berserkr2k.coreplugin.api.framework.permission.PermissionService
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import java.util.UUID

class PaperPermissionService : PermissionService {
    override fun hasPermission(uuid: UUID, permission: String): Boolean {
        val player = Bukkit.getPlayer(uuid) ?: return false
        return player.hasPermission(permission)
    }

    override fun hasPermission(sender: Any, permission: String): Boolean {
        return (sender as? CommandSender)?.hasPermission(permission) ?: false
    }
}
