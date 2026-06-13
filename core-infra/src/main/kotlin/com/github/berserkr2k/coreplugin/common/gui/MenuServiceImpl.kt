package com.github.berserkr2k.coreplugin.common.gui

import com.github.berserkr2k.coreplugin.api.framework.menu.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import com.github.berserkr2k.coreplugin.api.config.ItemConfig
import com.github.berserkr2k.coreplugin.api.framework.item.ItemBuilderFactory
import com.github.berserkr2k.coreplugin.common.ColorUtility
import com.github.berserkr2k.coreplugin.api.core.message.MessageService
import com.github.berserkr2k.coreplugin.api.core.message.CoreMessages
import java.util.concurrent.ConcurrentHashMap

class ButtonImpl(
    override val icon: ItemStack,
    private val clickAction: (Player, org.bukkit.event.inventory.ClickType) -> Unit
) : Button {
    override fun onClick(player: Player) {
        clickAction(player, org.bukkit.event.inventory.ClickType.LEFT)
    }

    override fun onClick(player: Player, clickType: org.bukkit.event.inventory.ClickType) {
        clickAction(player, clickType)
    }
}

class ButtonBuilderImpl : Button.Builder {
    private var icon: ItemStack? = null
    private var clickAction: (Player, org.bukkit.event.inventory.ClickType) -> Unit = { _, _ -> }

    override fun icon(item: ItemStack): Button.Builder = apply { this.icon = item }
    
    override fun onClick(action: (Player) -> Unit): Button.Builder = apply {
        this.clickAction = { p, _ -> action(p) }
    }

    override fun onClick(action: (Player, org.bukkit.event.inventory.ClickType) -> Unit): Button.Builder = apply {
        this.clickAction = action
    }

    override fun build(): Button {
        val finalIcon = icon ?: throw IllegalStateException("Button must have an icon!")
        return ButtonImpl(finalIcon, clickAction)
    }
}

class MenuImpl(private val customMenu: CustomMenu) : Menu {
    override fun open(player: Player) {
        customMenu.open(player)
    }

    override fun update(player: Player) {
        player.updateInventory()
    }

    override fun getItem(slot: Int): ItemStack? {
        return customMenu.inventory.getItem(slot)
    }
}

class MenuBuilderImpl(private val plugin: Plugin) : MenuBuilder {
    private var title: Component = Component.empty()
    private var slots: Int = 27
    private val buttons = mutableMapOf<Int, Button>()
    private var fillerButton: Button? = null
    private var closeAction: ((Player) -> Unit)? = null
    private val interactableSlots = mutableSetOf<Int>()
    private val localActions = mutableMapOf<String, (Player) -> Unit>()
    
    private var paginatedRunner: ((CustomMenu) -> Unit)? = null
    private var configLoader: ((CustomMenu) -> Unit)? = null

    override fun title(title: Component) = apply { this.title = title }
    
    override fun slots(slots: Int) = apply { this.slots = slots }

    override fun rows(rows: Int) = apply { this.slots = rows * 9 }
    
    override fun button(slot: Int, button: Button) = apply { this.buttons[slot] = button }
    
    override fun fill(button: Button) = apply { this.fillerButton = button }
    
    override fun closeAction(action: (Player) -> Unit) = apply { this.closeAction = action }
    
    override fun interactableSlots(slots: Collection<Int>) = apply {
        this.interactableSlots.clear()
        this.interactableSlots.addAll(slots)
    }
    
    override fun registerAction(actionName: String, handler: (Player) -> Unit) = apply {
        this.localActions[actionName.lowercase()] = handler
    }
    
    override fun loadFromConfig(
        config: MenuConfig,
        placeholders: Map<String, String>,
        ignoreSlots: List<Int>
    ) = apply {
        this.configLoader = { customMenu ->
            customMenu.loadFromConfig(config, placeholders, ignoreSlots)
        }
    }
    
    override fun <T> placePaginatedItems(
        config: MenuConfig,
        items: List<T>,
        previousPageItem: ItemConfig,
        nextPageItem: ItemConfig,
        render: (T, Int) -> Unit
    ) = apply {
        this.paginatedRunner = { customMenu ->
            customMenu.placePaginatedItems(config, items, previousPageItem.toItemStack(), nextPageItem.toItemStack(), render)
        }
    }

    private var dynamicRunner: ((CustomMenu) -> Unit)? = null

    override fun <T> placeDynamicItems(
        config: MenuConfig,
        items: List<T>,
        getGuiSlot: (T) -> Int,
        startSlot: Int,
        render: (T, Int) -> Unit
    ) = apply {
        this.dynamicRunner = { customMenu ->
            customMenu.placeDynamicItems(config, items, getGuiSlot, startSlot, render)
        }
    }

    override fun build(): Menu {
        val customMenu = CustomMenu(title, slots, plugin)
        
        // 1. Cargar relleno si existe
        fillerButton?.let { filler ->
            for (i in 0 until slots) {
                customMenu.setItem(i, filler.icon.clone()) { player, event ->
                    filler.onClick(player, event.click)
                }
            }
        }

        // 2. Registrar acciones locales
        localActions.forEach { (actionName, handler) ->
            customMenu.registerLocalAction(actionName) { player, _ ->
                handler(player)
            }
        }

        // 3. Cargar configuración si existe
        configLoader?.invoke(customMenu)

        // 4. Registrar interactable slots
        customMenu.interactableSlots.addAll(interactableSlots)

        // 5. Cargar botones definidos
        buttons.forEach { (slot, button) ->
            customMenu.setItem(slot, button.icon) { player, event ->
                button.onClick(player, event.click)
            }
        }

        // 6. Cargar paginados si existen
        paginatedRunner?.invoke(customMenu)
        dynamicRunner?.invoke(customMenu)

        // 7. Cargar acción de cierre
        closeAction?.let { action ->
            customMenu.onClose = action
        }

        return MenuImpl(customMenu)
    }
}

class MenuServiceImpl(private val plugin: Plugin) : MenuService {
    init {
        Button.setFactory { ButtonBuilderImpl() }
    }
    
    override fun createBuilder(): MenuBuilder = MenuBuilderImpl(plugin)
}

fun ItemConfig.toItemStack(): ItemStack {
    val factory = Bukkit.getServicesManager().load(ItemBuilderFactory::class.java)
        ?: throw IllegalStateException("ItemBuilderFactory service is not registered in Bukkit's ServiceManager!")
    return factory.builder(this).build()
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

    fun loadFromConfig(config: MenuConfig, placeholders: Map<String, String> = emptyMap(), ignoreSlots: List<Int> = emptyList()) {
        if (config.filler.enabled) {
            val fillerItem = config.filler.item.toItemStack()
            for (i in 0 until slots) {
                if (i !in ignoreSlots) {
                    setItem(i, fillerItem.clone())
                }
            }
        }

        config.items.forEach { (_, menuItemConfig) ->
            val processedItemConfig = applyPlaceholders(menuItemConfig.item, placeholders)
            val itemStack = processedItemConfig.toItemStack()

            menuItemConfig.slots.forEach { slot ->
                if (slot in 0 until slots) {
                    setItem(slot, itemStack.clone()) { player, event ->
                        val permission = menuItemConfig.permission
                        val sound = menuItemConfig.sound
                        val action = menuItemConfig.action
                        if (permission != null && !player.hasPermission(permission)) {
                            Bukkit.getServicesManager().load(MessageService::class.java)?.send(player, CoreMessages.NO_PERMISSION)
                            return@setItem
                        }

                        if (sound != null) {
                            try {
                                val soundEnum = org.bukkit.Sound.valueOf(sound.uppercase())
                                player.playSound(player.location, soundEnum, 1.0f, 1.0f)
                            } catch (e: Exception) {}
                        }

                        if (action != null) {
                            val handler = getActionHandler(action)
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

    fun handleInventoryClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        val slot = event.rawSlot
        val handler = clickHandlers[slot]
        if (handler != null) {
            handler(player, event)
        }
    }

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
                    return@forEach
                }
                nextFreeSlot
            }
            placedSlots.add(targetSlot)
            render(item, targetSlot)
        }
    }

    fun <T> placePaginatedItems(
        config: MenuConfig,
        items: List<T>,
        previousPageItem: ItemStack,
        nextPageItem: ItemStack,
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
            val emptyItem = ItemStack(org.bukkit.Material.AIR)
            dynamicSlots.forEach { setItem(it, emptyItem.clone(), null) }
            config.previousPageSlot?.let { if (it in 0 until slots) setItem(it, emptyItem.clone(), null) }
            config.nextPageSlot?.let { if (it in 0 until slots) setItem(it, emptyItem.clone(), null) }

            val start = currentPage * pageSize
            val end = minOf(start + pageSize, items.size)
            val pageItems = if (start < items.size) items.subList(start, end) else emptyList()

            pageItems.forEachIndexed { idx, item ->
                val slot = dynamicSlots[idx]
                render(item, slot)
            }

            config.previousPageSlot?.let { prevSlot ->
                if (prevSlot in 0 until slots && currentPage > 0) {
                    val prevBtn = previousPageItem
                    setItem(prevSlot, prevBtn) { p, _ ->
                        previousPage()
                    }
                }
            }

            config.nextPageSlot?.let { nextSlot ->
                if (nextSlot in 0 until slots && currentPage < totalPages - 1) {
                    val nextBtn = nextPageItem
                    setItem(nextSlot, nextBtn) { p, _ ->
                        nextPage()
                    }
                }
            }
            Unit
        }

        setPageRedrawer(redraw)
        redraw()
    }
}

object MenuManager : Listener {
    private val activeMenus = ConcurrentHashMap<Inventory, CustomMenu>()

    fun registerActiveMenu(inventory: Inventory, menu: CustomMenu) {
        activeMenus[inventory] = menu
    }

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
                event.isCancelled = true
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
