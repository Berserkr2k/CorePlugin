package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import com.github.berserkr2k.coreplugin.common.gui.CustomMenu
import org.bukkit.Material
import org.bukkit.GameMode
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.Plugin
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.util.EulerAngle
import org.bukkit.Bukkit

object ArmorStandEditorGui {

    private val miniMessage = MiniMessage.miniMessage()

    /**
     * Menú Principal del Editor
     */
    fun open(plugin: Plugin, player: Player, stand: ArmorStand) {
        val scale = ArmorStandEditorListener.playerScale.computeIfAbsent(player.uniqueId) { ScaleMode.COARSE }
        
        val menu = CustomMenu(
            miniMessage.deserialize("<gold><bold>Editor: ArmorStand</bold></gold>"),
            27,
            plugin
        )

        // Rellenar bordes decorativos con paneles de vidrio gris
        val panel = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val pMeta = panel.itemMeta
        pMeta.displayName(miniMessage.deserialize(" "))
        panel.itemMeta = pMeta
        for (i in listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25)) {
            menu.setItem(i, panel)
        }

        // Botones principales
        menu.setItem(10, createGuiItem(Material.ARMOR_STAND, "<yellow>Ajustar Pose</yellow>", "<gray>Click para rotar extremidades en X/Y/Z</gray>")) { p, _ ->
            openPoseMenu(plugin, p, stand)
        }

        menu.setItem(11, createGuiItem(Material.FEATHER, "<yellow>Ajustar Posición y Dirección</yellow>", "<gray>Click para trasladar y rotar Yaw</gray>")) { p, _ ->
            openPositionMenu(plugin, p, stand)
        }

        menu.setItem(12, createGuiItem(Material.COMPASS, "<yellow>Modificar Propiedades</yellow>", "<gray>Brazos, Invisibilidad, Tamaño, Gravedad, Invencibilidad</gray>")) { p, _ ->
            openPropertiesMenu(plugin, p, stand)
        }

        menu.setItem(13, createGuiItem(Material.CHEST, "<yellow>Inventario de Equipamiento</yellow>", "<gray>Click para colocar cualquier objeto en cabeza o manos</gray>")) { p, _ ->
            openEquipmentMenu(plugin, p, stand)
        }

        menu.setItem(14, createGuiItem(Material.PAPER, "<green>Copiar Ajustes</green>", "<gray>Copia poses y propiedades físicas</gray>")) { p, _ ->
            val equipMap = mutableMapOf<EquipmentSlot, ItemStack?>()
            for (slot in EquipmentSlot.values()) {
                equipMap[slot] = stand.equipment.getItem(slot)
            }
            ArmorStandEditorListener.copiedSettings[p.uniqueId] = CopiedSettings(
                headPose = stand.headPose,
                bodyPose = stand.bodyPose,
                leftArmPose = stand.leftArmPose,
                rightArmPose = stand.rightArmPose,
                leftLegPose = stand.leftLegPose,
                rightLegPose = stand.rightLegPose,
                isSmall = stand.isSmall,
                hasArms = stand.hasArms(),
                hasBasePlate = stand.hasBasePlate(),
                isVisible = stand.isVisible,
                hasGravity = stand.hasGravity(),
                isInvulnerable = stand.isInvulnerable,
                equipment = equipMap
            )
            p.sendMessage(miniMessage.deserialize("<green>¡Propiedades físicas y de pose copiadas al portapapeles!</green>"))
        }

        menu.setItem(15, createGuiItem(Material.GLOWSTONE_DUST, "<green>Pegar Ajustes</green>", "<gray>Click para aplicar propiedades copiadas</gray>")) { p, _ ->
            val copied = ArmorStandEditorListener.copiedSettings[p.uniqueId]
            if (copied == null) {
                p.sendMessage(miniMessage.deserialize("<red>No tienes ajustes copiados en el portapapeles.</red>"))
                return@setItem
            }
            stand.headPose = copied.headPose
            stand.bodyPose = copied.bodyPose
            stand.leftArmPose = copied.leftArmPose
            stand.rightArmPose = copied.rightArmPose
            stand.leftLegPose = copied.leftLegPose
            stand.rightLegPose = copied.rightLegPose
            stand.isSmall = copied.isSmall
            stand.setArms(copied.hasArms)
            stand.setBasePlate(copied.hasBasePlate)
            stand.isVisible = copied.isVisible
            stand.setGravity(copied.hasGravity)
            stand.isInvulnerable = copied.isInvulnerable

            // Survival-Friendly: Solo duplica o transfiere ítems si el jugador está en modo creativo
            if (p.gameMode == GameMode.CREATIVE && copied.equipment != null) {
                for ((slot, item) in copied.equipment) {
                    stand.equipment.setItem(slot, item)
                }
                p.sendMessage(miniMessage.deserialize("<green>¡Ajustes y equipamiento pegados con éxito!</green>"))
            } else {
                p.sendMessage(miniMessage.deserialize("<green>¡Ajustes de pose aplicados! (Los objetos no se duplicaron por seguridad en Supervivencia)</green>"))
            }
        }

        menu.setItem(16, createGuiItem(Material.NAME_TAG, "<yellow>Cambiar Nombre</yellow>", "<gray>Haz click y escribe en el chat para asignarle nombre</gray>")) { p, _ ->
            p.closeInventory()
            ArmorStandEditorListener.renamingSessions[p.uniqueId] = stand.uniqueId
            p.sendMessage(miniMessage.deserialize("<gold>Escribe el nombre del ArmorStand en el chat (Soporta colores con &):</gold>"))
        }

        val scaleLore = if (scale == ScaleMode.COARSE) "<yellow>Modo actual: <bold>GRUESO</bold></yellow> (<gray>15° / 0.5m</gray>)" else "<yellow>Modo actual: <bold>FINO</bold></yellow> (<gray>1° / 0.05m</gray>)"
        menu.setItem(26, createGuiItem(Material.LEVER, "<aqua>Escala de Ajuste</aqua>", "<gray>Click para alternar precisión</gray>\n$scaleLore")) { p, _ ->
            val nextScale = if (scale == ScaleMode.COARSE) ScaleMode.FINE else ScaleMode.COARSE
            ArmorStandEditorListener.playerScale[p.uniqueId] = nextScale
            p.sendMessage(miniMessage.deserialize("<green>Escala de ajuste cambiada a ${nextScale.name}.</green>"))
            open(plugin, p, stand) // Recargar el menú para actualizar descripción
        }

        menu.open(player)
    }

    /**
     * Sub-Menú: Ajustar Poses de Extremidades
     */
    private fun openPoseMenu(plugin: Plugin, player: Player, stand: ArmorStand) {
        val menu = CustomMenu(
            miniMessage.deserialize("<gold>Editor: Ajustar Pose</gold>"),
            27,
            plugin
        )

        menu.setItem(10, createGuiItem(Material.PLAYER_HEAD, "<yellow>Cabeza</yellow>", "<gray>Rotar cabeza</gray>")) { p, _ -> openPartRotationMenu(plugin, p, stand, "HEAD") }
        menu.setItem(11, createGuiItem(Material.CHAINMAIL_CHESTPLATE, "<yellow>Cuerpo</yellow>", "<gray>Rotar torso</gray>")) { p, _ -> openPartRotationMenu(plugin, p, stand, "BODY") }
        menu.setItem(12, createGuiItem(Material.STICK, "<yellow>Brazo Izquierdo</yellow>", "<gray>Rotar brazo izquierdo</gray>")) { p, _ -> openPartRotationMenu(plugin, p, stand, "LEFT_ARM") }
        menu.setItem(13, createGuiItem(Material.STICK, "<yellow>Brazo Derecho</yellow>", "<gray>Rotar brazo derecho</gray>")) { p, _ -> openPartRotationMenu(plugin, p, stand, "RIGHT_ARM") }
        menu.setItem(14, createGuiItem(Material.LEATHER_BOOTS, "<yellow>Pierna Izquierda</yellow>", "<gray>Rotar pierna izquierda</gray>")) { p, _ -> openPartRotationMenu(plugin, p, stand, "LEFT_LEG") }
        menu.setItem(15, createGuiItem(Material.LEATHER_BOOTS, "<yellow>Pierna Derecha</yellow>", "<gray>Rotar pierna derecha</gray>")) { p, _ -> openPartRotationMenu(plugin, p, stand, "RIGHT_LEG") }

        menu.setItem(26, createGuiItem(Material.BARRIER, "<red>Volver al Menú Principal</red>", "<gray>Regresa al menú anterior</gray>")) { p, _ ->
            open(plugin, p, stand)
        }
        menu.open(player)
    }

    /**
     * Sub-Menú: Rotación de una parte específica en X/Y/Z
     */
    private fun openPartRotationMenu(plugin: Plugin, player: Player, stand: ArmorStand, part: String) {
        val scale = ArmorStandEditorListener.playerScale[player.uniqueId] ?: ScaleMode.COARSE
        val angleRad = Math.toRadians(scale.rotationAngle)

        val menu = CustomMenu(
            miniMessage.deserialize("<gold>Rotar: $part</gold>"),
            27,
            plugin
        )

        val partsName = when(part) {
            "HEAD" -> "Cabeza"
            "BODY" -> "Cuerpo"
            "LEFT_ARM" -> "Brazo Izquierdo"
            "RIGHT_ARM" -> "Brazo Derecho"
            "LEFT_LEG" -> "Pierna Izquierda"
            "RIGHT_LEG" -> "Pierna Derecha"
            else -> part
        }

        fun getPose(): EulerAngle = when(part) {
            "HEAD" -> stand.headPose
            "BODY" -> stand.bodyPose
            "LEFT_ARM" -> stand.leftArmPose
            "RIGHT_ARM" -> stand.rightArmPose
            "LEFT_LEG" -> stand.leftLegPose
            "RIGHT_LEG" -> stand.rightLegPose
            else -> EulerAngle.ZERO
        }

        fun setPose(pose: EulerAngle) = when(part) {
            "HEAD" -> stand.headPose = pose
            "BODY" -> stand.bodyPose = pose
            "LEFT_ARM" -> stand.leftArmPose = pose
            "RIGHT_ARM" -> stand.rightArmPose = pose
            "LEFT_LEG" -> stand.leftLegPose = pose
            "RIGHT_LEG" -> stand.rightLegPose = pose
            else -> {}
        }

        // Eje X
        menu.setItem(10, createGuiItem(Material.RED_WOOL, "<red>Rotar X +</red>", "<gray>Rotar en eje X adelante</gray>")) { p, _ ->
            val pz = getPose()
            setPose(EulerAngle(pz.x + angleRad, pz.y, pz.z))
            p.sendMessage(miniMessage.deserialize("<green>¡$partsName rotada en X+!</green>"))
        }
        menu.setItem(11, createGuiItem(Material.RED_CONCRETE, "<red>Rotar X -</red>", "<gray>Rotar en eje X atrás</gray>")) { p, _ ->
            val pz = getPose()
            setPose(EulerAngle(pz.x - angleRad, pz.y, pz.z))
            p.sendMessage(miniMessage.deserialize("<green>¡$partsName rotada en X-!</green>"))
        }

        // Eje Y
        menu.setItem(12, createGuiItem(Material.GREEN_WOOL, "<green>Rotar Y +</green>", "<gray>Rotar en eje Y derecha</gray>")) { p, _ ->
            val pz = getPose()
            setPose(EulerAngle(pz.x, pz.y + angleRad, pz.z))
            p.sendMessage(miniMessage.deserialize("<green>¡$partsName rotada en Y+!</green>"))
        }
        menu.setItem(13, createGuiItem(Material.GREEN_CONCRETE, "<green>Rotar Y -</green>", "<gray>Rotar en eje Y izquierda</gray>")) { p, _ ->
            val pz = getPose()
            setPose(EulerAngle(pz.x, pz.y - angleRad, pz.z))
            p.sendMessage(miniMessage.deserialize("<green>¡$partsName rotada en Y-!</green>"))
        }

        // Eje Z
        menu.setItem(14, createGuiItem(Material.BLUE_WOOL, "<blue>Rotar Z +</blue>", "<gray>Rotar en eje Z inclinar derecha</gray>")) { p, _ ->
            val pz = getPose()
            setPose(EulerAngle(pz.x, pz.y, pz.z + angleRad))
            p.sendMessage(miniMessage.deserialize("<green>¡$partsName rotada en Z+!</green>"))
        }
        menu.setItem(15, createGuiItem(Material.BLUE_CONCRETE, "<blue>Rotar Z -</blue>", "<gray>Rotar en eje Z inclinar izquierda</gray>")) { p, _ ->
            val pz = getPose()
            setPose(EulerAngle(pz.x, pz.y, pz.z - angleRad))
            p.sendMessage(miniMessage.deserialize("<green>¡$partsName rotada en Z-!</green>"))
        }

        menu.setItem(26, createGuiItem(Material.BARRIER, "<red>Volver a Selección de Partes</red>", "<gray>Regresa al menú de poses</gray>")) { p, _ ->
            openPoseMenu(plugin, p, stand)
        }
        menu.open(player)
    }

    /**
     * Sub-Menú: Trasladación Física y Yaw
     */
    private fun openPositionMenu(plugin: Plugin, player: Player, stand: ArmorStand) {
        val scale = ArmorStandEditorListener.playerScale[player.uniqueId] ?: ScaleMode.COARSE
        val dist = scale.moveDistance

        val menu = CustomMenu(
            miniMessage.deserialize("<gold>Física: Posición y Rotación</gold>"),
            27,
            plugin
        )

        // Eje X (Este/Oeste)
        menu.setItem(10, createGuiItem(Material.COAL, "<yellow>Mover X + (Este)</yellow>", "<gray>Mueve $dist bloques al Este</gray>")) { p, _ ->
            val loc = stand.location.clone().add(dist, 0.0, 0.0)
            stand.teleport(loc)
        }
        menu.setItem(11, createGuiItem(Material.COAL_ORE, "<yellow>Mover X - (Oeste)</yellow>", "<gray>Mueve $dist bloques al Oeste</gray>")) { p, _ ->
            val loc = stand.location.clone().add(-dist, 0.0, 0.0)
            stand.teleport(loc)
        }

        // Eje Y (Arriba/Abajo)
        menu.setItem(12, createGuiItem(Material.IRON_INGOT, "<yellow>Mover Y + (Arriba)</yellow>", "<gray>Mueve $dist bloques hacia arriba</gray>")) { p, _ ->
            val loc = stand.location.clone().add(0.0, dist, 0.0)
            stand.teleport(loc)
        }
        menu.setItem(13, createGuiItem(Material.IRON_ORE, "<yellow>Mover Y - (Abajo)</yellow>", "<gray>Mueve $dist bloques hacia abajo</gray>")) { p, _ ->
            val loc = stand.location.clone().add(0.0, -dist, 0.0)
            stand.teleport(loc)
        }

        // Eje Z (Sur/Norte)
        menu.setItem(14, createGuiItem(Material.GOLD_INGOT, "<yellow>Mover Z + (Sur)</yellow>", "<gray>Mueve $dist bloques al Sur</gray>")) { p, _ ->
            val loc = stand.location.clone().add(0.0, 0.0, dist)
            stand.teleport(loc)
        }
        menu.setItem(15, createGuiItem(Material.GOLD_ORE, "<yellow>Mover Z - (Norte)</yellow>", "<gray>Mueve $dist bloques al Norte</gray>")) { p, _ ->
            val loc = stand.location.clone().add(0.0, 0.0, -dist)
            stand.teleport(loc)
        }

        // Rotar Dirección (Yaw)
        val deg = scale.rotationAngle
        menu.setItem(16, createGuiItem(Material.OAK_LOG, "<gold>Rotar Dirección + (Derecha)</gold>", "<gray>Rota $deg grados a la derecha</gray>")) { p, _ ->
            val loc = stand.location.clone()
            loc.yaw = (loc.yaw + deg).toFloat()
            stand.teleport(loc)
        }
        menu.setItem(17, createGuiItem(Material.STRIPPED_OAK_LOG, "<gold>Rotar Dirección - (Izquierda)</gold>", "<gray>Rota $deg grados a la izquierda</gray>")) { p, _ ->
            val loc = stand.location.clone()
            loc.yaw = (loc.yaw - deg).toFloat()
            stand.teleport(loc)
        }

        menu.setItem(26, createGuiItem(Material.BARRIER, "<red>Volver al Menú Principal</red>", "<gray>Regresa al menú anterior</gray>")) { p, _ ->
            open(plugin, p, stand)
        }
        menu.open(player)
    }

    /**
     * Sub-Menú: Toggles de Propiedades Físicas
     */
    private fun openPropertiesMenu(plugin: Plugin, player: Player, stand: ArmorStand) {
        val menu = CustomMenu(
            miniMessage.deserialize("<gold>Modificar Propiedades Físicas</gold>"),
            27,
            plugin
        )

        menu.setItem(10, createGuiItem(Material.ARMOR_STAND, "<yellow>Alternar Brazos</yellow>", "<gray>Brazos activos: ${stand.hasArms()}</gray>")) { p, _ ->
            stand.setArms(!stand.hasArms())
            openPropertiesMenu(plugin, p, stand)
        }
        menu.setItem(11, createGuiItem(Material.GLASS, "<yellow>Alternar Invisibilidad</yellow>", "<gray>Visibilidad activa: ${stand.isVisible}</gray>")) { p, _ ->
            stand.isVisible = !stand.isVisible
            openPropertiesMenu(plugin, p, stand)
        }
        menu.setItem(12, createGuiItem(Material.STONE_SLAB, "<yellow>Alternar Baseplate</yellow>", "<gray>Plato base activo: ${stand.hasBasePlate()}</gray>")) { p, _ ->
            stand.setBasePlate(!stand.hasBasePlate())
            openPropertiesMenu(plugin, p, stand)
        }
        menu.setItem(13, createGuiItem(Material.RABBIT_FOOT, "<yellow>Alternar Tamaño</yellow>", "<gray>Tamaño pequeño: ${stand.isSmall}</gray>")) { p, _ ->
            stand.isSmall = !stand.isSmall
            openPropertiesMenu(plugin, p, stand)
        }
        menu.setItem(14, createGuiItem(Material.ANVIL, "<yellow>Alternar Gravedad</yellow>", "<gray>Gravedad activa: ${stand.hasGravity()}</gray>")) { p, _ ->
            stand.setGravity(!stand.hasGravity())
            openPropertiesMenu(plugin, p, stand)
        }
        menu.setItem(15, createGuiItem(Material.NETHERITE_INGOT, "<yellow>Alternar Invulnerabilidad</yellow>", "<gray>Invencible: ${stand.isInvulnerable}</gray>")) { p, _ ->
            stand.isInvulnerable = !stand.isInvulnerable
            openPropertiesMenu(plugin, p, stand)
        }

        menu.setItem(26, createGuiItem(Material.BARRIER, "<red>Volver al Menú Principal</red>", "<gray>Regresa al menú anterior</gray>")) { p, _ ->
            open(plugin, p, stand)
        }
        menu.open(player)
    }

    /**
     * Sub-Menú: Colocación de Equipamiento Avanzado (Dual Wielding, Pumpkins)
     */
    private fun openEquipmentMenu(plugin: Plugin, player: Player, stand: ArmorStand) {
        val menu = CustomMenu(
            miniMessage.deserialize("<gold>Equipamiento Avanzado</gold>"),
            27,
            plugin
        )

        // Cargar ítems actuales de la armadura (usando ItemStack(AIR) si son nulos)
        val head = stand.equipment.helmet ?: ItemStack(Material.AIR)
        val chest = stand.equipment.chestplate ?: ItemStack(Material.AIR)
        val legs = stand.equipment.leggings ?: ItemStack(Material.AIR)
        val feet = stand.equipment.boots ?: ItemStack(Material.AIR)
        val mainHand = stand.equipment.itemInMainHand ?: ItemStack(Material.AIR)
        val offHand = stand.equipment.itemInOffHand ?: ItemStack(Material.AIR)

        menu.inventory.setItem(10, head)
        menu.inventory.setItem(11, chest)
        menu.inventory.setItem(12, legs)
        menu.inventory.setItem(13, feet)
        menu.inventory.setItem(14, mainHand)
        menu.inventory.setItem(15, offHand)

        // Marcar slots como interactuables para que el jugador pueda poner/quitar ítems libremente
        menu.interactableSlots.addAll(listOf(10, 11, 12, 13, 14, 15))

        // Rellenar bordes con paneles informativos
        val panel = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val pMeta = panel.itemMeta
        pMeta.displayName(miniMessage.deserialize("<gray>Coloca ítems aquí</gray>"))
        panel.itemMeta = pMeta

        for (i in 0 until 27) {
            if (i !in 10..15) {
                // Rótulos informativos específicos para orientar al jugador
                val label = when(i) {
                    1 -> createGuiItem(Material.PLAYER_HEAD, "<aqua>Ranura: Cabeza (Cascos, Calabazas, Banners)</aqua>", "<gray>Coloca ítems abajo en el slot #10</gray>")
                    2 -> createGuiItem(Material.NETHERITE_CHESTPLATE, "<aqua>Ranura: Pecho (Pecheras)</aqua>", "<gray>Coloca ítems abajo en el slot #11</gray>")
                    3 -> createGuiItem(Material.NETHERITE_LEGGINGS, "<aqua>Ranura: Piernas (Pantalones)</aqua>", "<gray>Coloca ítems abajo en el slot #12</gray>")
                    4 -> createGuiItem(Material.NETHERITE_BOOTS, "<aqua>Ranura: Pies (Botas)</aqua>", "<gray>Coloca ítems abajo en el slot #13</gray>")
                    5 -> createGuiItem(Material.DIAMOND_SWORD, "<aqua>Ranura: Mano Principal (Espadas, Herramientas, Bloques)</aqua>", "<gray>Coloca ítems abajo en el slot #14</gray>")
                    6 -> createGuiItem(Material.SHIELD, "<aqua>Ranura: Mano Secundaria (Escudos, Dual Wielding)</aqua>", "<gray>Coloca ítems abajo en el slot #15</gray>")
                    else -> panel
                }
                menu.setItem(i, label)
            }
        }

        // Al cerrar el inventario, aplicar todos los cambios de equipamiento directamente al ArmorStand
        menu.onClose = { p ->
            val newHead = menu.inventory.getItem(10) ?: ItemStack(Material.AIR)
            val newChest = menu.inventory.getItem(11) ?: ItemStack(Material.AIR)
            val newLegs = menu.inventory.getItem(12) ?: ItemStack(Material.AIR)
            val newFeet = menu.inventory.getItem(13) ?: ItemStack(Material.AIR)
            val newMainHand = menu.inventory.getItem(14) ?: ItemStack(Material.AIR)
            val newOffHand = menu.inventory.getItem(15) ?: ItemStack(Material.AIR)

            stand.equipment.helmet = newHead
            stand.equipment.chestplate = newChest
            stand.equipment.leggings = newLegs
            stand.equipment.boots = newFeet
            stand.equipment.setItemInMainHand(newMainHand)
            stand.equipment.setItemInOffHand(newOffHand)

            p.sendMessage(miniMessage.deserialize("<green>¡El equipamiento del ArmorStand se ha sincronizado exitosamente!</green>"))
        }

        menu.open(player)
    }

    private fun createGuiItem(material: Material, title: String, loreLine: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item
        meta.displayName(miniMessage.deserialize(title))
        meta.lore(listOf(miniMessage.deserialize(loreLine)))
        item.itemMeta = meta
        return item
    }
}
