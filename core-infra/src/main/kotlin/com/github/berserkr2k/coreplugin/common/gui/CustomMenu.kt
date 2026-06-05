package com.github.berserkr2k.coreplugin.common.gui

import com.github.berserkr2k.coreplugin.common.ColorUtility
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.util.concurrent.ConcurrentHashMap
import com.github.berserkr2k.coreplugin.infrastructure.config.ItemConfig

@ConfigSerializable
data class FillerConfig(
    val enabled: Boolean = true,
    val item: ItemConfig = ItemConfig(
        material = "GRAY_STAINED_GLASS_PANE",
        displayName = " "
    )
)

@ConfigSerializable
data class MenuItemConfig(
    val slots: List<Int> = emptyList(),
    val item: ItemConfig = ItemConfig(),
    val action: String? = null,
    val sound: String? = null,
    val permission: String? = null
)

@ConfigSerializable
data class MenuConfig(
    val title: String = "<gold>Menú</gold>",
    val size: Int = 27,
    val filler: FillerConfig = FillerConfig(),
    val items: Map<String, MenuItemConfig> = emptyMap(),
    val paginated: Boolean = false,
    val dynamicSlots: List<Int> = emptyList(),
    val previousPageSlot: Int? = null,
    val nextPageSlot: Int? = null,
    val previousPageItem: ItemConfig = ItemConfig(material = "ARROW", displayName = "<yellow>Página Anterior</yellow>"),
    val nextPageItem: ItemConfig = ItemConfig(material = "ARROW", displayName = "<yellow>Siguiente Página</yellow>")
)

object MenuActionRegistry {
    private val globalActions = ConcurrentHashMap<String, (Player, InventoryClickEvent) -> Unit>()

    fun register(actionName: String, handler: (Player, InventoryClickEvent) -> Unit) {
        globalActions[actionName.lowercase()] = handler
    }

    fun getAction(actionName: String): ((Player, InventoryClickEvent) -> Unit)? {
        return globalActions[actionName.lowercase()]
    }

    fun unregister(actionName: String) {
        globalActions.remove(actionName.lowercase())
    }
}

class CustomMenu(
    val title: Component,
    val slots: Int,
    private val plugin: Plugin
) {
    val inventory: Inventory = Bukkit.createInventory(null, slots, title)
    private val clickHandlers = ConcurrentHashMap<Int, (Player, InventoryClickEvent) -> Unit>()
    val interactableSlots = ConcurrentHashMap.newKeySet<Int>()
    var onClose: ((Player) -> Unit)? = null
    
    private val localActions = ConcurrentHashMap<String, (Player, InventoryClickEvent) -> Unit>()

    var currentPage: Int = 0
    var totalPages: Int = 1
    private var pageRedrawer: (() -> Unit)? = null

    fun setPageRedrawer(redrawer: () -> Unit) {
        this.pageRedrawer = redrawer
    }

    fun nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++
            pageRedrawer?.invoke()
        }
    }

    fun previousPage() {
        if (currentPage > 0) {
            currentPage--
            pageRedrawer?.invoke()
        }
    }

    fun registerLocalAction(actionName: String, handler: (Player, InventoryClickEvent) -> Unit) {
        localActions[actionName.lowercase()] = handler
    }

    fun getActionHandler(actionName: String): ((Player, InventoryClickEvent) -> Unit)? {
        return localActions[actionName.lowercase()] ?: MenuActionRegistry.getAction(actionName)
    }

    /**
     * Define un ítem en un slot y su manejador de evento al hacer clic.
     */
    fun setItem(slot: Int, item: ItemStack, onClick: ((Player, InventoryClickEvent) -> Unit)? = null) {
        if (slot in 0 until slots) {
            inventory.setItem(slot, item)
            if (onClick != null) {
                clickHandlers[slot] = onClick
            } else {
                clickHandlers.remove(slot)
            }
        }
    }

    /**
     * Carga el layout y comportamiento desde una MenuConfig, soportando placeholders locales opcionales.
     */
    fun loadFromConfig(config: MenuConfig, placeholders: Map<String, String> = emptyMap(), ignoreSlots: List<Int> = emptyList()) {
        // 1. Relleno de fondo si está activo
        if (config.filler.enabled) {
            val fillerItem = config.filler.item.toItemStack()
            for (i in 0 until slots) {
                if (i !in ignoreSlots) {
                    setItem(i, fillerItem.clone())
                }
            }
        }

        // 2. Cargar ítems configurados
        config.items.forEach { (_, menuItemConfig) ->
            val processedItemConfig = applyPlaceholders(menuItemConfig.item, placeholders)
            val itemStack = processedItemConfig.toItemStack()

            menuItemConfig.slots.forEach { slot ->
                if (slot in 0 until slots) {
                    setItem(slot, itemStack.clone()) { player, event ->
                        // Verificar permiso
                        if (menuItemConfig.permission != null && !player.hasPermission(menuItemConfig.permission)) {
                            player.sendMessage(ColorUtility.parse("<red>❌ No tienes permiso para usar esto.</red>"))
                            return@setItem
                        }

                        // Sonido
                        if (menuItemConfig.sound != null) {
                            try {
                                val soundEnum = org.bukkit.Sound.valueOf(menuItemConfig.sound.uppercase())
                                player.playSound(player.location, soundEnum, 1.0f, 1.0f)
                            } catch (e: Exception) {}
                        }

                        // Acción
                        if (menuItemConfig.action != null) {
                            val handler = getActionHandler(menuItemConfig.action)
                            if (handler != null) {
                                handler(player, event)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun applyPlaceholders(config: ItemConfig, placeholders: Map<String, String>): ItemConfig {
        if (placeholders.isEmpty()) return config
        
        var display = config.displayName
        if (display != null) {
            placeholders.forEach { (k, v) ->
                display = display!!.replace(k, v)
            }
        }
        
        val processedLore = config.lore.map { line ->
            var temp = line
            placeholders.forEach { (k, v) ->
                temp = temp.replace(k, v)
            }
            temp
        }

        return config.copy(
            displayName = display,
            lore = processedLore
        )
    }

    fun open(player: Player) {
        player.openInventory(inventory)
        MenuManager.registerActiveMenu(inventory, this)
    }

    /**
     * Procesa de forma segura el evento de clic en el inventario del menú.
     */
    fun handleInventoryClick(event: InventoryClickEvent) {
        event.isCancelled = true // Antisteal estricto por defecto
        val player = event.whoClicked as? Player ?: return
        val slot = event.rawSlot
        val handler = clickHandlers[slot]
        if (handler != null) {
            handler(player, event)
        }
    }

    /**
     * Posiciona dinámicamente una lista de elementos en el inventario basándose en el guiSlot
     * configurado por cada elemento, y cayendo en slots libres de forma secuencial.
     * Omitirá y protegerá los slots reservados para ítems estáticos cargados desde HOCON.
     */
    fun <T> placeDynamicItems(
        config: MenuConfig,
        items: List<T>,
        getGuiSlot: (T) -> Int,
        startSlot: Int = 10,
        render: (T, Int) -> Unit
    ) {
        val occupiedSlots = mutableSetOf<Int>()
        config.items.values.forEach { occupiedSlots.addAll(it.slots) }

        val placedSlots = mutableSetOf<Int>()
        var nextFreeSlot = startSlot

        items.forEach { item ->
            val suggestedSlot = getGuiSlot(item)
            val targetSlot = if (suggestedSlot in 0 until slots && !occupiedSlots.contains(suggestedSlot)) {
                suggestedSlot
            } else {
                while (nextFreeSlot in occupiedSlots || placedSlots.contains(nextFreeSlot)) {
                    nextFreeSlot++
                }
                if (nextFreeSlot >= slots) {
                    return@forEach // No hay más espacio libre
                }
                nextFreeSlot
            }
            placedSlots.add(targetSlot)
            render(item, targetSlot)
        }
    }

    /**
     * Posiciona dinámicamente una lista de elementos aplicando paginación automática y
     * gestionando los botones de Siguiente/Anterior según la configuración y el estado.
     */
    fun <T> placePaginatedItems(
        config: MenuConfig,
        items: List<T>,
        previousPageItem: ItemConfig = ItemConfig(material = "ARROW", displayName = "<yellow>Página Anterior</yellow>"),
        nextPageItem: ItemConfig = ItemConfig(material = "ARROW", displayName = "<yellow>Siguiente Página</yellow>"),
        render: (T, Int) -> Unit
    ) {
        val dynamicSlots = config.dynamicSlots.ifEmpty {
            val occupied = mutableSetOf<Int>()
            config.items.values.forEach { occupied.addAll(it.slots) }
            (0 until slots).filter { 
                it !in occupied && 
                it != config.previousPageSlot && 
                it != config.nextPageSlot
            }
        }

        val pageSize = dynamicSlots.size
        if (pageSize <= 0) return

        totalPages = maxOf(1, java.lang.Math.ceil(items.size.toDouble() / pageSize).toInt())

        val redraw = {
            // 1. Limpiar slots dinámicos
            val emptyItem = ItemStack(org.bukkit.Material.AIR)
            dynamicSlots.forEach { setItem(it, emptyItem.clone(), null) }
            config.previousPageSlot?.let { if (it in 0 until slots) setItem(it, emptyItem.clone(), null) }
            config.nextPageSlot?.let { if (it in 0 until slots) setItem(it, emptyItem.clone(), null) }

            // 2. Obtener elementos de la página actual
            val start = currentPage * pageSize
            val end = minOf(start + pageSize, items.size)
            val pageItems = if (start < items.size) items.subList(start, end) else emptyList()

            // 3. Renderizar elementos
            pageItems.forEachIndexed { idx, item ->
                val slot = dynamicSlots[idx]
                render(item, slot)
            }

            // 4. Botón de Página Anterior
            config.previousPageSlot?.let { prevSlot ->
                if (prevSlot in 0 until slots && currentPage > 0) {
                    val prevBtn = previousPageItem.toItemStack()
                    setItem(prevSlot, prevBtn) { p, _ ->
                        previousPage()
                    }
                }
            }

            // 5. Botón de Siguiente Página
            config.nextPageSlot?.let { nextSlot ->
                if (nextSlot in 0 until slots && currentPage < totalPages - 1) {
                    val nextBtn = nextPageItem.toItemStack()
                    setItem(nextSlot, nextBtn) { p, _ ->
                        nextPage()
                    }
                }
            }
            Unit
        }

        setPageRedrawer(redraw)
        redraw() // Renderizado inicial
    }
}

object MenuManager : Listener {
    private val activeMenus = ConcurrentHashMap<Inventory, CustomMenu>()

    fun registerActiveMenu(inventory: Inventory, menu: CustomMenu) {
        activeMenus[inventory] = menu
    }

    /**
     * Inicializa y registra el MenuManager en el servidor.
     */
    fun init(plugin: Plugin) {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val inventory = event.inventory
        val menu = activeMenus[inventory] ?: return

        if (event.rawSlot < 0) return

        if (event.rawSlot < event.inventory.size) {
            if (menu.interactableSlots.contains(event.rawSlot)) {
                event.isCancelled = false
            } else {
                event.isCancelled = true
                menu.handleInventoryClick(event)
            }
        } else {
            if (event.isShiftClick) {
                event.isCancelled = true // Previene bypasses mediante Shift-Click
            } else {
                event.isCancelled = false
            }
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val inventory = event.inventory
        val menu = activeMenus[inventory] ?: return
        
        val hasNonInteractableDrag = event.rawSlots.any { it < inventory.size && !menu.interactableSlots.contains(it) }
        if (hasNonInteractableDrag) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val menu = activeMenus.remove(event.inventory)
        menu?.onClose?.invoke(player)
    }
}
