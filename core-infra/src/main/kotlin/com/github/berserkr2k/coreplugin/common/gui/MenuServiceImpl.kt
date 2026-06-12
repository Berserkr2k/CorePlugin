package com.github.berserkr2k.coreplugin.common.gui

import com.github.berserkr2k.coreplugin.api.framework.menu.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import net.kyori.adventure.text.Component

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
        previousPageItem: ItemStack,
        nextPageItem: ItemStack,
        render: (T, Int) -> Unit
    ) = apply {
        this.paginatedRunner = { customMenu ->
            customMenu.placePaginatedItems(config, items, previousPageItem, nextPageItem, render)
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
