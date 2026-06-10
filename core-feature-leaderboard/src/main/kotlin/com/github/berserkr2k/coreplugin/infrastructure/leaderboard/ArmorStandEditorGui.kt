package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.common.gui.CustomMenu
import com.github.berserkr2k.coreplugin.common.gui.MenuConfig
import com.github.berserkr2k.coreplugin.common.gui.MenuItemConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.ItemConfig
import com.github.berserkr2k.coreplugin.infrastructure.config.ModularConfigManager
import org.bukkit.Material
import org.bukkit.GameMode
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.Plugin
import org.bukkit.util.EulerAngle
import org.bukkit.Bukkit
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import com.github.berserkr2k.coreplugin.api.state.PlayerStateService
import com.github.berserkr2k.coreplugin.api.di.ServiceRegistry

@ConfigSerializable
data class ArmorStandEditorGuiConfig(
    val main: MenuConfig = MenuConfig(),
    val pose: MenuConfig = MenuConfig(),
    val rotation: MenuConfig = MenuConfig(),
    val position: MenuConfig = MenuConfig(),
    val properties: MenuConfig = MenuConfig(),
    val equipment: MenuConfig = MenuConfig()
)

object ArmorStandEditorGui {

    private lateinit var plugin: Plugin
    private lateinit var configManager: ModularConfigManager
    private lateinit var playerStateService: PlayerStateService

    lateinit var guiConfig: ArmorStandEditorGuiConfig

    fun init(plugin: Plugin, configManager: ModularConfigManager, serviceRegistry: ServiceRegistry) {
        this.plugin = plugin
        this.configManager = configManager
        this.playerStateService = serviceRegistry.get(PlayerStateService::class.java)
        reloadConfigs()
    }

    fun reloadConfigs() {
        guiConfig = configManager.loadModuleConfig(
            "menus/armorstand-editor.conf",
            ArmorStandEditorGuiConfig::class.java,
            createDefaultGuiConfig()
        ).join()
    }

    private fun getEditorState(player: Player): ArmorStandEditorStateContainer {
        return playerStateService.getContainer(player.uniqueId, ArmorStandEditorListener.ARMOR_STAND_EDITOR_STATE)
    }

    /**
     * Menú Principal del Editor
     */
    fun open(plugin: Plugin, player: Player, stand: ArmorStand) {
        val state = getEditorState(player)
        val scale = state.scaleMode
        
        val scaleLore = if (scale == ScaleMode.COARSE) {
            "<yellow>Modo actual: <bold>GRUESO</bold></yellow> (<gray>15° / 0.5m</gray>)"
        } else {
            "<yellow>Modo actual: <bold>FINO</bold></yellow> (<gray>1° / 0.05m</gray>)"
        }

        val placeholders = mapOf("%scale_status%" to scaleLore)

        val menu = CustomMenu(
            ColorUtility.parse(guiConfig.main.title),
            guiConfig.main.size,
            plugin
        )

        // Registrar acciones locales
        menu.registerLocalAction("pose") { p, _ -> openPoseMenu(plugin, p, stand) }
        menu.registerLocalAction("position") { p, _ -> openPositionMenu(plugin, p, stand) }
        menu.registerLocalAction("properties") { p, _ -> openPropertiesMenu(plugin, p, stand) }
        menu.registerLocalAction("equipment") { p, _ -> openEquipmentMenu(plugin, p, stand) }
        
        menu.registerLocalAction("copy") { p, _ ->
            val equipMap = mutableMapOf<EquipmentSlot, ItemStack?>()
            for (slot in EquipmentSlot.values()) {
                equipMap[slot] = stand.equipment.getItem(slot)
            }
            val st = getEditorState(p)
            st.copiedSettings = CopiedSettings(
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
            p.sendMessage(ColorUtility.parse("<green>✔ ¡Propiedades físicas y de pose copiadas al portapapeles!</green>"))
        }

        menu.registerLocalAction("paste") { p, _ ->
            val st = getEditorState(p)
            val copied = st.copiedSettings
            if (copied == null) {
                p.sendMessage(ColorUtility.parse("<red>❌ No tienes ajustes copiados en el portapapeles.</red>"))
                return@registerLocalAction
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

            if (p.gameMode == GameMode.CREATIVE && copied.equipment != null) {
                for ((slot, item) in copied.equipment) {
                    stand.equipment.setItem(slot, item)
                }
                p.sendMessage(ColorUtility.parse("<green>✔ ¡Ajustes y equipamiento pegados con éxito!</green>"))
            } else {
                p.sendMessage(ColorUtility.parse("<green>✔ ¡Ajustes de pose aplicados! (Los objetos no se duplicaron por seguridad en Supervivencia)</green>"))
            }
        }

        menu.registerLocalAction("rename") { p, _ ->
            p.closeInventory()
            val st = getEditorState(p)
            st.renamingStandUuid = stand.uniqueId
            p.sendMessage(ColorUtility.parse("<gold>✏ Escribe el nombre del ArmorStand en el chat (Soporta colores con &):</gold>"))
        }

        menu.registerLocalAction("scale") { p, _ ->
            val nextScale = if (scale == ScaleMode.COARSE) ScaleMode.FINE else ScaleMode.COARSE
            val st = getEditorState(p)
            st.scaleMode = nextScale
            p.sendMessage(ColorUtility.parse("<green>✔ Escala de ajuste cambiada a ${nextScale.name}.</green>"))
            open(plugin, p, stand)
        }

        menu.loadFromConfig(guiConfig.main, placeholders)
        menu.open(player)
    }

    /**
     * Sub-Menú: Ajustar Poses de Extremidades
     */
    private fun openPoseMenu(plugin: Plugin, player: Player, stand: ArmorStand) {
        val menu = CustomMenu(
            ColorUtility.parse(guiConfig.pose.title),
            guiConfig.pose.size,
            plugin
        )

        menu.registerLocalAction("pose_head") { p, _ -> openPartRotationMenu(plugin, p, stand, "HEAD") }
        menu.registerLocalAction("pose_body") { p, _ -> openPartRotationMenu(plugin, p, stand, "BODY") }
        menu.registerLocalAction("pose_left_arm") { p, _ -> openPartRotationMenu(plugin, p, stand, "LEFT_ARM") }
        menu.registerLocalAction("pose_right_arm") { p, _ -> openPartRotationMenu(plugin, p, stand, "RIGHT_ARM") }
        menu.registerLocalAction("pose_left_leg") { p, _ -> openPartRotationMenu(plugin, p, stand, "LEFT_LEG") }
        menu.registerLocalAction("pose_right_leg") { p, _ -> openPartRotationMenu(plugin, p, stand, "RIGHT_LEG") }
        menu.registerLocalAction("back_to_main") { p, _ -> open(plugin, p, stand) }

        menu.loadFromConfig(guiConfig.pose)
        menu.open(player)
    }

    /**
     * Sub-Menú: Rotación de una parte específica en X/Y/Z
     */
    private fun openPartRotationMenu(plugin: Plugin, player: Player, stand: ArmorStand, part: String) {
        val partsName = when(part) {
            "HEAD" -> "Cabeza"
            "BODY" -> "Cuerpo"
            "LEFT_ARM" -> "Brazo Izquierdo"
            "RIGHT_ARM" -> "Brazo Derecho"
            "LEFT_LEG" -> "Pierna Izquierda"
            "RIGHT_LEG" -> "Pierna Derecha"
            else -> part
        }

        val placeholders = mapOf("%part_name%" to partsName)

        val menu = CustomMenu(
            ColorUtility.parse(guiConfig.rotation.title.replace("%part_name%", partsName)),
            guiConfig.rotation.size,
            plugin
        )

        menu.registerLocalAction("adjust_x") { p, _ -> givePoseTool(p, stand, part, "X") }
        menu.registerLocalAction("adjust_y") { p, _ -> givePoseTool(p, stand, part, "Y") }
        menu.registerLocalAction("adjust_z") { p, _ -> givePoseTool(p, stand, part, "Z") }
        menu.registerLocalAction("back_to_pose") { p, _ -> openPoseMenu(plugin, p, stand) }

        menu.loadFromConfig(guiConfig.rotation, placeholders)
        menu.open(player)
    }

    private fun givePoseTool(player: Player, stand: ArmorStand, part: String, axis: String) {
        player.closeInventory()

        // 1. Guardar backup de la mano actual del jugador
        val currentItem = player.inventory.itemInMainHand
        val st = getEditorState(player)
        st.originalHandItem = currentItem.clone()

        // 2. Crear la herramienta de pose (Palanca)
        val tool = ItemStack(Material.LEVER)
        val meta = tool.itemMeta ?: return
        
        val partsName = when(part.uppercase()) {
            "HEAD" -> "Cabeza"
            "BODY" -> "Cuerpo"
            "LEFT_ARM" -> "Brazo Izquierdo"
            "RIGHT_ARM" -> "Brazo Derecho"
            "LEFT_LEG" -> "Pierna Izquierda"
            "RIGHT_LEG" -> "Pierna Derecha"
            else -> part
        }

        meta.displayName(ColorUtility.parse("<gold><bold>Herramienta de Pose: $partsName ($axis)</bold></gold>"))
        meta.lore(listOf(
            ColorUtility.parse("<yellow>Parte: <white>$partsName</white></yellow>"),
            ColorUtility.parse("<yellow>Eje: <white>$axis</white></yellow>"),
            ColorUtility.parse(""),
            ColorUtility.parse("<green>◀ Click Izquierdo: Aumentar ángulo</green>"),
            ColorUtility.parse("<red>▶ Click Derecho: Disminuir ángulo</red>"),
            ColorUtility.parse("<gray>Sneak + Click Derecho: Volver al menú</gray>")
        ))

        // Guardar estado en PDC
        meta.persistentDataContainer.set(
            ArmorStandEditorListener.poseToolKey,
            org.bukkit.persistence.PersistentDataType.STRING,
            "${stand.uniqueId}:$part:$axis"
        )
        tool.itemMeta = meta

        // 3. Colocar en la mano del jugador
        player.inventory.setItemInMainHand(tool)
        player.sendMessage(ColorUtility.parse("<green>✔ ¡Recibiste la <gold>Herramienta de Pose</gold>! Ajusta libremente. Sneak + Click Derecho para volver.</green>"))
    }

    /**
     * Sub-Menú: Trasladación Física y Yaw
     */
    private fun openPositionMenu(plugin: Plugin, player: Player, stand: ArmorStand) {
        val st = getEditorState(player)
        val scale = st.scaleMode
        val dist = scale.moveDistance
        val deg = scale.rotationAngle

        val placeholders = mapOf(
            "%dist%" to dist.toString(),
            "%deg%" to deg.toString()
        )

        val menu = CustomMenu(
            ColorUtility.parse(guiConfig.position.title),
            guiConfig.position.size,
            plugin
        )

        menu.registerLocalAction("move_x_plus") { _, _ ->
            val loc = stand.location.clone().add(dist, 0.0, 0.0)
            stand.teleport(loc)
        }
        menu.registerLocalAction("move_x_minus") { _, _ ->
            val loc = stand.location.clone().add(-dist, 0.0, 0.0)
            stand.teleport(loc)
        }
        menu.registerLocalAction("move_y_plus") { _, _ ->
            val loc = stand.location.clone().add(0.0, dist, 0.0)
            stand.teleport(loc)
        }
        menu.registerLocalAction("move_y_minus") { _, _ ->
            val loc = stand.location.clone().add(0.0, -dist, 0.0)
            stand.teleport(loc)
        }
        menu.registerLocalAction("move_z_plus") { _, _ ->
            val loc = stand.location.clone().add(0.0, 0.0, dist)
            stand.teleport(loc)
        }
        menu.registerLocalAction("move_z_minus") { _, _ ->
            val loc = stand.location.clone().add(0.0, 0.0, -dist)
            stand.teleport(loc)
        }
        menu.registerLocalAction("yaw_plus") { _, _ ->
            val loc = stand.location.clone()
            loc.yaw = (loc.yaw + deg).toFloat()
            stand.teleport(loc)
        }
        menu.registerLocalAction("yaw_minus") { _, _ ->
            val loc = stand.location.clone()
            loc.yaw = (loc.yaw - deg).toFloat()
            stand.teleport(loc)
        }
        menu.registerLocalAction("back_to_main") { p, _ -> open(plugin, p, stand) }

        menu.loadFromConfig(guiConfig.position, placeholders)
        menu.open(player)
    }

    /**
     * Sub-Menú: Toggles de Propiedades Físicas
     */
    private fun openPropertiesMenu(plugin: Plugin, player: Player, stand: ArmorStand) {
        val yes = "<green>✔ SÍ</green>"
        val no = "<red>❌ NO</red>"

        val placeholders = mapOf(
            "%arms_status%" to if (stand.hasArms()) yes else no,
            "%visibility_status%" to if (stand.isVisible) yes else no,
            "%baseplate_status%" to if (stand.hasBasePlate()) yes else no,
            "%small_status%" to if (stand.isSmall) yes else no,
            "%gravity_status%" to if (stand.hasGravity()) yes else no,
            "%invulnerable_status%" to if (stand.isInvulnerable) yes else no
        )

        val menu = CustomMenu(
            ColorUtility.parse(guiConfig.properties.title),
            guiConfig.properties.size,
            plugin
        )

        menu.registerLocalAction("toggle_arms") { p, _ ->
            stand.setArms(!stand.hasArms())
            openPropertiesMenu(plugin, p, stand)
        }
        menu.registerLocalAction("toggle_visibility") { p, _ ->
            stand.isVisible = !stand.isVisible
            openPropertiesMenu(plugin, p, stand)
        }
        menu.registerLocalAction("toggle_baseplate") { p, _ ->
            stand.setBasePlate(!stand.hasBasePlate())
            openPropertiesMenu(plugin, p, stand)
        }
        menu.registerLocalAction("toggle_small") { p, _ ->
            stand.isSmall = !stand.isSmall
            openPropertiesMenu(plugin, p, stand)
        }
        menu.registerLocalAction("toggle_gravity") { p, _ ->
            stand.setGravity(!stand.hasGravity())
            openPropertiesMenu(plugin, p, stand)
        }
        menu.registerLocalAction("toggle_invulnerable") { p, _ ->
            stand.isInvulnerable = !stand.isInvulnerable
            openPropertiesMenu(plugin, p, stand)
        }
        menu.registerLocalAction("back_to_main") { p, _ -> open(plugin, p, stand) }

        menu.loadFromConfig(guiConfig.properties, placeholders)
        menu.open(player)
    }

    /**
     * Sub-Menú: Colocación de Equipamiento Avanzado
     */
    private fun openEquipmentMenu(plugin: Plugin, player: Player, stand: ArmorStand) {
        val menu = CustomMenu(
            ColorUtility.parse(guiConfig.equipment.title),
            guiConfig.equipment.size,
            plugin
        )

        // Cargar ítems actuales del ArmorStand
        val head = stand.equipment.helmet ?: ItemStack(Material.AIR)
        val chest = stand.equipment.chestplate ?: ItemStack(Material.AIR)
        val legs = stand.equipment.leggings ?: ItemStack(Material.AIR)
        val feet = stand.equipment.boots ?: ItemStack(Material.AIR)
        val mainHand = stand.equipment.itemInMainHand ?: ItemStack(Material.AIR)
        val offHand = stand.equipment.itemInOffHand ?: ItemStack(Material.AIR)

        menu.setItem(10, head)
        menu.setItem(11, chest)
        menu.setItem(12, legs)
        menu.setItem(13, feet)
        menu.setItem(14, mainHand)
        menu.setItem(15, offHand)

        // Marcar slots como interactuables
        menu.interactableSlots.addAll(listOf(10, 11, 12, 13, 14, 15))

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

            p.sendMessage(ColorUtility.parse("<green>✔ ¡El equipamiento del ArmorStand se ha sincronizado exitosamente!</green>"))
        }

        // Cargar estructura base de la configuración (etiquetas, etc.)
        menu.loadFromConfig(guiConfig.equipment, ignoreSlots = listOf(10, 11, 12, 13, 14, 15))
        menu.open(player)
    }

    private fun createDefaultGuiConfig(): ArmorStandEditorGuiConfig {
        return ArmorStandEditorGuiConfig(
            main = MenuConfig(
                title = "<gold><bold>Editor: ArmorStand</bold></gold>",
                size = 27,
                items = mapOf(
                    "pose" to MenuItemConfig(slots = listOf(10), item = ItemConfig(material = "ARMOR_STAND", displayName = "<yellow>Ajustar Pose</yellow>", lore = listOf("<gray>Click para rotar extremidades en X/Y/Z</gray>")), action = "pose"),
                    "position" to MenuItemConfig(slots = listOf(11), item = ItemConfig(material = "FEATHER", displayName = "<yellow>Ajustar Posición y Dirección</yellow>", lore = listOf("<gray>Click para trasladar y rotar Yaw</gray>")), action = "position"),
                    "properties" to MenuItemConfig(slots = listOf(12), item = ItemConfig(material = "COMPASS", displayName = "<yellow>Modificar Propiedades</yellow>", lore = listOf("<gray>Brazos, Invisibilidad, Tamaño, Gravedad, Invencibilidad</gray>")), action = "properties"),
                    "equipment" to MenuItemConfig(slots = listOf(13), item = ItemConfig(material = "CHEST", displayName = "<yellow>Inventario de Equipamiento</yellow>", lore = listOf("<gray>Click para colocar cualquier objeto en cabeza o manos</gray>")), action = "equipment"),
                    "copy" to MenuItemConfig(slots = listOf(14), item = ItemConfig(material = "PAPER", displayName = "<green>Copiar Ajustes</green>", lore = listOf("<gray>Copia poses y propiedades físicas</gray>")), action = "copy"),
                    "paste" to MenuItemConfig(slots = listOf(15), item = ItemConfig(material = "GLOWSTONE_DUST", displayName = "<green>Pegar Ajustes</green>", lore = listOf("<gray>Click para aplicar propiedades copiadas</gray>")), action = "paste"),
                    "rename" to MenuItemConfig(slots = listOf(16), item = ItemConfig(material = "NAME_TAG", displayName = "<yellow>Cambiar Nombre</yellow>", lore = listOf("<gray>Haz click y escribe en el chat para asignarle nombre</gray>")), action = "rename"),
                    "scale" to MenuItemConfig(slots = listOf(26), item = ItemConfig(material = "LEVER", displayName = "<aqua>Escala de Ajuste</aqua>", lore = listOf("<gray>Click para alternar precisión</gray>", "%scale_status%")), action = "scale")
                )
            ),
            pose = MenuConfig(
                title = "<gold>Editor: Ajustar Pose</gold>",
                size = 27,
                items = mapOf(
                    "head" to MenuItemConfig(slots = listOf(10), item = ItemConfig(material = "PLAYER_HEAD", displayName = "<yellow>Cabeza</yellow>", lore = listOf("<gray>Rotar cabeza</gray>")), action = "pose_head"),
                    "body" to MenuItemConfig(slots = listOf(11), item = ItemConfig(material = "CHAINMAIL_CHESTPLATE", displayName = "<yellow>Cuerpo</yellow>", lore = listOf("<gray>Rotar torso</gray>")), action = "pose_body"),
                    "left_arm" to MenuItemConfig(slots = listOf(12), item = ItemConfig(material = "STICK", displayName = "<yellow>Brazo Izquierdo</yellow>", lore = listOf("<gray>Rotar brazo izquierdo</gray>")), action = "pose_left_arm"),
                    "right_arm" to MenuItemConfig(slots = listOf(13), item = ItemConfig(material = "STICK", displayName = "<yellow>Brazo Derecho</yellow>", lore = listOf("<gray>Rotar brazo derecho</gray>")), action = "pose_right_arm"),
                    "left_leg" to MenuItemConfig(slots = listOf(14), item = ItemConfig(material = "LEATHER_BOOTS", displayName = "<yellow>Pierna Izquierda</yellow>", lore = listOf("<gray>Rotar pierna izquierda</gray>")), action = "pose_left_leg"),
                    "right_leg" to MenuItemConfig(slots = listOf(15), item = ItemConfig(material = "LEATHER_BOOTS", displayName = "<yellow>Pierna Derecha</yellow>", lore = listOf("<gray>Rotar pierna derecha</gray>")), action = "pose_right_leg"),
                    "back" to MenuItemConfig(slots = listOf(26), item = ItemConfig(material = "BARRIER", displayName = "<red>Volver al Menú Principal</red>", lore = listOf("<gray>Regresa al menú anterior</gray>")), action = "back_to_main")
                )
            ),
            rotation = MenuConfig(
                title = "<gold>Rotar: %part_name%</gold>",
                size = 27,
                items = mapOf(
                    "adjust_x" to MenuItemConfig(slots = listOf(11), item = ItemConfig(material = "RED_WOOL", displayName = "<red>Ajustar X</red>", lore = listOf("<gray>Click para recibir la herramienta</gray>", "<gray>y ajustar rotación en el eje X.</gray>")), action = "adjust_x"),
                    "adjust_y" to MenuItemConfig(slots = listOf(13), item = ItemConfig(material = "GREEN_WOOL", displayName = "<green>Ajustar Y</green>", lore = listOf("<gray>Click para recibir la herramienta</gray>", "<gray>y ajustar rotación en el eje Y.</gray>")), action = "adjust_y"),
                    "adjust_z" to MenuItemConfig(slots = listOf(15), item = ItemConfig(material = "BLUE_WOOL", displayName = "<blue>Ajustar Z</blue>", lore = listOf("<gray>Click para recibir la herramienta</gray>", "<gray>y ajustar rotación en el eje Z.</gray>")), action = "adjust_z"),
                    "back" to MenuItemConfig(slots = listOf(26), item = ItemConfig(material = "BARRIER", displayName = "<red>Volver a Selección de Partes</red>", lore = listOf("<gray>Regresa al menú de poses</gray>")), action = "back_to_pose")
                )
            ),
            position = MenuConfig(
                title = "<gold>Física: Posición y Rotación</gold>",
                size = 27,
                items = mapOf(
                    "move_x_plus" to MenuItemConfig(slots = listOf(10), item = ItemConfig(material = "COAL", displayName = "<yellow>Mover X + (Este)</yellow>", lore = listOf("<gray>Mueve %dist% bloques al Este</gray>")), action = "move_x_plus"),
                    "move_x_minus" to MenuItemConfig(slots = listOf(11), item = ItemConfig(material = "COAL_ORE", displayName = "<yellow>Mover X - (Oeste)</yellow>", lore = listOf("<gray>Mueve %dist% bloques al Oeste</gray>")), action = "move_x_minus"),
                    "move_y_plus" to MenuItemConfig(slots = listOf(12), item = ItemConfig(material = "IRON_INGOT", displayName = "<yellow>Mover Y + (Arriba)</yellow>", lore = listOf("<gray>Mueve %dist% bloques hacia arriba</gray>")), action = "move_y_plus"),
                    "move_y_minus" to MenuItemConfig(slots = listOf(13), item = ItemConfig(material = "IRON_ORE", displayName = "<yellow>Mover Y - (Abajo)</yellow>", lore = listOf("<gray>Mueve %dist% bloques hacia abajo</gray>")), action = "move_y_minus"),
                    "move_z_plus" to MenuItemConfig(slots = listOf(14), item = ItemConfig(material = "GOLD_INGOT", displayName = "<yellow>Mover Z + (Sur)</yellow>", lore = listOf("<gray>Mueve %dist% bloques al Sur</gray>")), action = "move_z_plus"),
                    "move_z_minus" to MenuItemConfig(slots = listOf(15), item = ItemConfig(material = "GOLD_ORE", displayName = "<yellow>Mover Z - (Norte)</yellow>", lore = listOf("<gray>Mueve %dist% bloques al Norte</gray>")), action = "move_z_minus"),
                    "yaw_plus" to MenuItemConfig(slots = listOf(16), item = ItemConfig(material = "OAK_LOG", displayName = "<gold>Rotar Dirección + (Derecha)</gold>", lore = listOf("<gray>Rota %deg% grados a la derecha</gray>")), action = "yaw_plus"),
                    "yaw_minus" to MenuItemConfig(slots = listOf(17), item = ItemConfig(material = "STRIPPED_OAK_LOG", displayName = "<gold>Rotar Dirección - (Izquierda)</gold>", lore = listOf("<gray>Rota %deg% grados a la izquierda</gray>")), action = "yaw_minus"),
                    "back" to MenuItemConfig(slots = listOf(26), item = ItemConfig(material = "BARRIER", displayName = "<red>Volver al Menú Principal</red>", lore = listOf("<gray>Regresa al menú anterior</gray>")), action = "back_to_main")
                )
            ),
            properties = MenuConfig(
                title = "<gold>Modificar Propiedades Físicas</gold>",
                size = 27,
                items = mapOf(
                    "toggle_arms" to MenuItemConfig(slots = listOf(10), item = ItemConfig(material = "ARMOR_STAND", displayName = "<yellow>Alternar Brazos</yellow>", lore = listOf("<gray>Brazos activos: %arms_status%</gray>")), action = "toggle_arms"),
                    "toggle_visibility" to MenuItemConfig(slots = listOf(11), item = ItemConfig(material = "GLASS", displayName = "<yellow>Alternar Invisibilidad</yellow>", lore = listOf("<gray>Visibilidad activa: %visibility_status%</gray>")), action = "toggle_visibility"),
                    "toggle_baseplate" to MenuItemConfig(slots = listOf(12), item = ItemConfig(material = "STONE_SLAB", displayName = "<yellow>Alternar Baseplate</yellow>", lore = listOf("<gray>Plato base activo: %baseplate_status%</gray>")), action = "toggle_baseplate"),
                    "toggle_small" to MenuItemConfig(slots = listOf(13), item = ItemConfig(material = "RABBIT_FOOT", displayName = "<yellow>Alternar Tamaño</yellow>", lore = listOf("<gray>Tamaño pequeño: %small_status%</gray>")), action = "toggle_small"),
                    "toggle_gravity" to MenuItemConfig(slots = listOf(14), item = ItemConfig(material = "ANVIL", displayName = "<yellow>Alternar Gravedad</yellow>", lore = listOf("<gray>Gravedad activa: %gravity_status%</gray>")), action = "toggle_gravity"),
                    "toggle_invulnerable" to MenuItemConfig(slots = listOf(15), item = ItemConfig(material = "NETHERITE_INGOT", displayName = "<yellow>Alternar Invulnerabilidad</yellow>", lore = listOf("<gray>Invencible: %invulnerable_status%</gray>")), action = "toggle_invulnerable"),
                    "back" to MenuItemConfig(slots = listOf(26), item = ItemConfig(material = "BARRIER", displayName = "<red>Volver al Menú Principal</red>", lore = listOf("<gray>Regresa al menú anterior</gray>")), action = "back_to_main")
                )
            ),
            equipment = MenuConfig(
                title = "<gold>Equipamiento Avanzado</gold>",
                size = 27,
                items = mapOf(
                    "label_head" to MenuItemConfig(slots = listOf(1), item = ItemConfig(material = "PLAYER_HEAD", displayName = "<aqua>Ranura: Cabeza (Cascos, Calabazas, Banners)</aqua>", lore = listOf("<gray>Coloca ítems abajo en el slot #10</gray>"))),
                    "label_chest" to MenuItemConfig(slots = listOf(2), item = ItemConfig(material = "NETHERITE_CHESTPLATE", displayName = "<aqua>Ranura: Pecho (Pecheras)</aqua>", lore = listOf("<gray>Coloca ítems abajo en el slot #11</gray>"))),
                    "label_legs" to MenuItemConfig(slots = listOf(3), item = ItemConfig(material = "NETHERITE_LEGGINGS", displayName = "<aqua>Ranura: Piernas (Pantalones)</aqua>", lore = listOf("<gray>Coloca ítems abajo en el slot #12</gray>"))),
                    "label_feet" to MenuItemConfig(slots = listOf(4), item = ItemConfig(material = "NETHERITE_BOOTS", displayName = "<aqua>Ranura: Pies (Botas)</aqua>", lore = listOf("<gray>Coloca ítems abajo en el slot #13</gray>"))),
                    "label_mainhand" to MenuItemConfig(slots = listOf(5), item = ItemConfig(material = "DIAMOND_SWORD", displayName = "<aqua>Ranura: Mano Principal (Espadas, Herramientas, Bloques)</aqua>", lore = listOf("<gray>Coloca ítems abajo en el slot #14</gray>"))),
                    "label_offhand" to MenuItemConfig(slots = listOf(6), item = ItemConfig(material = "SHIELD", displayName = "<aqua>Ranura: Mano Secundaria (Escudos, Dual Wielding)</aqua>", lore = listOf("<gray>Coloca ítems abajo en el slot #15</gray>")))
                )
            )
        )
    }
}
