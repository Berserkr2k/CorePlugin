package com.github.berserkr2k.coreplugin.api.framework.permission

import java.util.UUID

interface PermissionService {
    /**
     * Checks if a player identified by UUID has the specified permission.
     */
    fun hasPermission(uuid: UUID, permission: String): Boolean

    /**
     * Checks if a command sender/player has the specified permission.
     */
    fun hasPermission(sender: Any, permission: String): Boolean
}
