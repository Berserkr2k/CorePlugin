package com.github.berserkr2k.coreplugin

import com.github.berserkr2k.coreplugin.admin.AdminChatListener
import com.github.berserkr2k.coreplugin.admin.AdminSessionManager
import com.github.berserkr2k.coreplugin.api.item.ItemFactory
import com.github.berserkr2k.coreplugin.api.nametag.NameTagAdapter
import com.github.berserkr2k.coreplugin.api.ui.ScoreboardAdapter
import com.github.berserkr2k.coreplugin.api.ui.VisualNotifier
import com.github.berserkr2k.coreplugin.chat.ChatManager
import com.github.berserkr2k.coreplugin.chat.PrivateMessageManager
import com.github.berserkr2k.coreplugin.command.cosmetic.ColorCommand
import com.github.berserkr2k.coreplugin.command.admin.CoreCommandRouter
import com.github.berserkr2k.coreplugin.command.chat.MsgCommand
import com.github.berserkr2k.coreplugin.command.chat.ReplyCommand
import com.github.berserkr2k.coreplugin.command.chat.SocialSpyCommand
import com.github.berserkr2k.coreplugin.command.admin.UserAdminCommand
import com.github.berserkr2k.coreplugin.config.YamlConfig
import com.github.berserkr2k.coreplugin.database.DatabaseManager
import com.github.berserkr2k.coreplugin.listener.MenuListener
import com.github.berserkr2k.coreplugin.nametag.NameTagInterceptor
import com.github.berserkr2k.coreplugin.nametag.NameTagManager
import com.github.berserkr2k.coreplugin.scoreboard.SidebarController
import com.github.berserkr2k.coreplugin.v1_21_R3.ui.ModernVisualNotifier
import com.github.berserkr2k.coreplugin.v1_8_R3.ui.LegacyVisualNotifier
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

/**
 * Clase principal del CorePlugin.
 * Actúa como la fachada central (Facade) que inicializa y conecta
 * todos los módulos, bases de datos y adaptadores según la versión del servidor.
 */
class CorePlugin : JavaPlugin() {

    // ========================================================================
    // LIBRERÍAS EXTERNAS
    // ========================================================================
    lateinit var adventure: BukkitAudiences

    // ========================================================================
    // ADAPTADORES DE VERSIÓN (API)
    // ========================================================================
    lateinit var visualNotifier: VisualNotifier
        private set
    lateinit var itemFactory: ItemFactory
        private set
    lateinit var nameTagAdapter: NameTagAdapter
        private set
    lateinit var scoreboardAdapter: ScoreboardAdapter
        private set

    // ========================================================================
    // GESTORES DE CONFIGURACIÓN Y DATOS (MANAGERS)
    // ========================================================================
    lateinit var mainConfig: YamlConfig
        private set
    lateinit var nametagsConfig: YamlConfig
        private set
    lateinit var databaseManager: DatabaseManager
        private set

    // ========================================================================
    // CONTROLADORES DE LÓGICA DE NEGOCIO (CONTROLLERS)
    // ========================================================================
    lateinit var nameTagManager: NameTagManager
        private set
    lateinit var adminSessionManager: AdminSessionManager
        private set
    lateinit var sidebarController: SidebarController
        private set


    // ========================================================================
    // CONTROLADORES DE LÓGICA DE COLOR DEL NICK Y CHAT
    // ========================================================================
    lateinit var chatManager: ChatManager
        private set

    lateinit var privateMessageManager: PrivateMessageManager
        private set

    // ========================================================================
    // CICLO DE VIDA DEL PLUGIN
    // ========================================================================

    override fun onEnable() {
        // 1. Inicializar librerías base
        adventure = BukkitAudiences.create(this)

        // 2. Cargar configuraciones (YAML)
        mainConfig = YamlConfig(this, "config.yml")
        nametagsConfig = YamlConfig(this, "nametags.yml")

        // 3. Inyectar adaptadores según la versión de Minecraft
        if (!setupAdapterPattern()) {
            logger.severe("Versión de servidor no soportada. Apagando plugin...")
            server.pluginManager.disablePlugin(this)
            return
        }

        // 4. Conectar a la Base de Datos
        databaseManager = DatabaseManager(this)
        chatManager = ChatManager(this)
        privateMessageManager = PrivateMessageManager(this)

        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            com.github.berserkr2k.coreplugin.placeholder.CorePlaceholderExpansion(this).register()
            logger.info("Variables %core_...% registradas en PlaceholderAPI exitosamente.")
        }

        // 5. Inicializar Controladores y Gestores
        adminSessionManager = AdminSessionManager()
        nameTagManager = NameTagManager(this)
        sidebarController = SidebarController(this)

        // 6. Registrar Eventos e Interceptores
        NameTagInterceptor(this).register()
        server.pluginManager.registerEvents(AdminChatListener(this), this)
        server.pluginManager.registerEvents(MenuListener(), this)

        // 7. Registrar Comandos
        val router = CoreCommandRouter() // Asegúrate de que el router no requiera 'this' en su constructor
        router.register(UserAdminCommand(this))
        getCommand("core")?.setExecutor(router)
        getCommand("color")?.setExecutor(ColorCommand(this))

        getCommand("msg")?.setExecutor(MsgCommand(this))
        getCommand("reply")?.setExecutor(ReplyCommand(this))
        getCommand("socialspy")?.setExecutor(SocialSpyCommand(this))

        // 8. Mensaje de éxito
        printStartupBanner()
    }

    override fun onDisable() {
        if (this::adventure.isInitialized) {
            adventure.close()
        }

        if (this::sidebarController.isInitialized) {
            sidebarController.shutdown()
        }

        if (this::databaseManager.isInitialized) {
            databaseManager.close()
            logger.info("Base de datos desconectada de forma segura.")
        }
    }

    // ========================================================================
    // MÉTODOS INTERNOS (SETUP & UTILS)
    // ========================================================================

    /**
     * Detecta la versión del servidor y asigna las implementaciones correctas
     * a las variables de la API (Patrón Adapter).
     * @return true si la versión es compatible, false en caso contrario.
     */
    private fun setupAdapterPattern(): Boolean {
        val version = Bukkit.getServer().bukkitVersion
        return when {
            version.contains("1.8.8") -> {
                visualNotifier = LegacyVisualNotifier(adventure)
                itemFactory = com.github.berserkr2k.coreplugin.v1_8_R3.item.LegacyItemFactory()
                nameTagAdapter = com.github.berserkr2k.coreplugin.v1_8_R3.nametag.LegacyNameTagAdapter()

                // AQUÍ ESTABA EL ERROR: Instanciamos el Scoreboard de la 1.8
                //scoreboardAdapter = com.github.berserkr2k.coreplugin.v1_8_R3.ui.LegacyScoreboardAdapter()
                true
            }
            version.contains("1.21") -> {
                visualNotifier = ModernVisualNotifier()
                itemFactory = com.github.berserkr2k.coreplugin.v1_21_R3.item.ModernItemFactory()
                nameTagAdapter = com.github.berserkr2k.coreplugin.v1_21_R3.nametag.ModernNameTagAdapter()

                // AQUÍ ESTABA EL ERROR: Instanciamos el Scoreboard moderno
                scoreboardAdapter = com.github.berserkr2k.coreplugin.v1_21_R3.ui.ModernScoreboardAdapter()
                true
            }
            else -> false
        }
    }

    /**
     * Dibuja un banner ASCII en la consola usando la API Adventure
     * para soportar gradientes y colores hexadecimales.
     */
    private fun printStartupBanner() {
        val console = adventure.console()
        val mm = MiniMessage.miniMessage()
        val version = description.version
        val mcVersion = server.bukkitVersion

        val banner = """
            <dark_gray>====================================================
            <green>       ____               <gold>CorePlugin
            <green>      / __ \              <gray>Versión: <white>v$version
            <green>     | |  | |             <gray>Motor: <aqua>$mcVersion
            <green>     | |__| |             <gray>Estado: <green><bold>SISTEMA ACTIVO
            <green>      \____/              <gray>Autor: <white>Berserkr2k
            <dark_gray>====================================================
        """.trimIndent()

        banner.lines().forEach { line ->
            console.sendMessage(mm.deserialize(line))
        }
    }
}