package com.eventplugin

import com.eventplugin.chat.ChatFormatter
import com.eventplugin.commands.*
import com.eventplugin.listeners.*
import com.eventplugin.managers.*
import com.eventplugin.placeholders.EventPlaceholders
import com.eventplugin.tasks.*
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class EventPlugin : JavaPlugin() {

    // Managers - lateinit allows initialization in onEnable
    lateinit var eventManager: EventManager
    lateinit var partyManager: PartyManager
    lateinit var kitManager: KitManager
    lateinit var surgeManager: SurgeManager
    lateinit var borderManager: BorderManager
    lateinit var statsManager: StatsManager
    lateinit var combatManager: CombatManager
    lateinit var spectatorManager: SpectatorManager
    lateinit var adminManager: AdminManager
    lateinit var spawnManager: SpawnManager
    lateinit var leaderboardManager: LeaderboardManager
    lateinit var worldManager: WorldManager
    lateinit var templateManager: TemplateManager

    // Chat formatter
    lateinit var chatFormatter: ChatFormatter

    // Tasks
    private var scoreboardTask: ScoreboardTask? = null
    private var bossBarTask: BossBarTask? = null
    private var heightCheckTask: HeightCheckTask? = null
    private var tabUpdateTask: Int? = null

    override fun onEnable() {
        logger.info("=================================")
        logger.info("EventPlugin v${description.version}")
        logger.info("Enabling...")
        logger.info("=================================")

        // Save default config
        saveDefaultConfig()

        // Initialize managers
        try {
            initializeManagers()
            logger.info("✓ Managers initialized")
        } catch (e: Exception) {
            logger.severe("✗ Failed to initialize managers: ${e.message}")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
            return
        }

        // Initialize chat formatter
        chatFormatter = ChatFormatter(this)

        // Register commands
        try {
            registerCommands()
            logger.info("✓ Commands registered")
        } catch (e: Exception) {
            logger.severe("✗ Failed to register commands: ${e.message}")
            e.printStackTrace()
        }

        // Register listeners
        try {
            registerListeners()
            logger.info("✓ Listeners registered")
        } catch (e: Exception) {
            logger.severe("✗ Failed to register listeners: ${e.message}")
            e.printStackTrace()
        }

        // Start tasks
        try {
            startTasks()
            logger.info("✓ Tasks started")
        } catch (e: Exception) {
            logger.severe("✗ Failed to start tasks: ${e.message}")
            e.printStackTrace()
        }

        // Register PlaceholderAPI expansion
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                EventPlaceholders(this).register()
                logger.info("✓ PlaceholderAPI hooked")
            } catch (e: Exception) {
                logger.warning("✗ Failed to hook PlaceholderAPI: ${e.message}")
            }
        }

        logger.info("=================================")
        logger.info("EventPlugin enabled successfully!")
        logger.info("=================================")
    }

    override fun onDisable() {
        logger.info("Disabling EventPlugin...")

        // Stop tasks
        scoreboardTask?.stop()
        bossBarTask?.stop()
        heightCheckTask?.stop()
        tabUpdateTask?.let { Bukkit.getScheduler().cancelTask(it) }

        // Stop surge
        surgeManager.stop()

        // Save all data
        statsManager.saveAll()
        partyManager.saveAll()

        logger.info("EventPlugin disabled!")
    }

    private fun initializeManagers() {
        // Order matters - some managers depend on others
        statsManager = StatsManager(this)
        partyManager = PartyManager(this)
        kitManager = KitManager(this)
        surgeManager = SurgeManager(this)
        borderManager = BorderManager(this)
        combatManager = CombatManager(this)
        spectatorManager = SpectatorManager(this)
        adminManager = AdminManager(this)
        spawnManager = SpawnManager(this)
        leaderboardManager = LeaderboardManager(this)
        worldManager = WorldManager(this)
        templateManager = TemplateManager(this)
        eventManager = EventManager(this) // Initialize last as it may depend on others

        // Reload managers to load config values
        partyManager.reload()
        surgeManager.reload()
        borderManager.reload()
        combatManager.reload()
        kitManager.reload()
        spawnManager.reload()
        statsManager.reload()
    }

    private fun registerCommands() {
        // Main commands
        getCommand("event")?.setExecutor(EventCommand(this))
        getCommand("party")?.setExecutor(PartyCommand(this))
        getCommand("partychat")?.setExecutor(PartyChatCommand(this))

        // Kit commands
        getCommand("kit")?.setExecutor(KitCommand(this))
        getCommand("k1")?.setExecutor(KitSlotCommand(this, 1))
        getCommand("k2")?.setExecutor(KitSlotCommand(this, 2))
        getCommand("k3")?.setExecutor(KitSlotCommand(this, 3))
        getCommand("kitroom")?.setExecutor(KitRoomCommand(this))

        // Admin commands
        getCommand("border")?.setExecutor(BorderCommand(this))
        getCommand("surge")?.setExecutor(SurgeCommand(this))
        getCommand("setspawn")?.setExecutor(SetSpawnCommand(this))
        getCommand("spawn")?.setExecutor(SpawnCommand(this))
        getCommand("revall")?.setExecutor(ReviveAllCommand(this))
        getCommand("rev")?.setExecutor(ReviveCommand(this))
        getCommand("prerevive")?.setExecutor(PreReviveCommand(this))
        getCommand("invis")?.setExecutor(InvisCommand(this))
        getCommand("clear")?.setExecutor(ClearCommand(this))
        getCommand("broadcast")?.setExecutor(BroadcastCommand(this))
        getCommand("staffchat")?.setExecutor(StaffChatCommand(this))

        // Stats command
        getCommand("stats")?.setExecutor(StatsCommand(this))
    }

    private fun registerListeners() {
        val pm = server.pluginManager

        // Player listeners
        pm.registerEvents(PlayerDeathListener(this), this)
        pm.registerEvents(PlayerJoinListener(this), this)
        pm.registerEvents(PlayerQuitListener(this), this)

        // Combat listeners
        pm.registerEvents(CombatListener(this), this)

        // Block listeners
        pm.registerEvents(BlockListener(this), this)

        // Movement listener
        pm.registerEvents(PlayerMoveListener(this), this)

        // Spectator listener
        pm.registerEvents(SpectatorListener(this), this)

        // Inventory listener
        pm.registerEvents(InventoryListener(this), this)

        // Damage listener
        pm.registerEvents(DamageListener(this), this)

        // Chat listener
        pm.registerEvents(ChatListener(this), this)

        // GUI listener
        pm.registerEvents(GUIListener(this), this)
    }

    private fun startTasks() {
        scoreboardTask = ScoreboardTask(this).also { it.start() }
        bossBarTask = BossBarTask(this).also { it.start() }
        heightCheckTask = HeightCheckTask(this).also { it.start() }

        // Tab list updater - updates every second
        tabUpdateTask = Bukkit.getScheduler().runTaskTimer(this, Runnable {
            updateTabList()
        }, 0L, 20L).taskId
    }

    private fun updateTabList() {
        Bukkit.getOnlinePlayers().forEach { player ->
            val tabName = chatFormatter.getTabListName(player)
            player.setPlayerListName(tabName)
        }
    }

    fun reload() {
        // Reload config
        reloadConfig()

        // Reload all managers
        partyManager.reload()
        surgeManager.reload()
        borderManager.reload()
        combatManager.reload()
        kitManager.reload()
        spawnManager.reload()
        statsManager.reload()

        logger.info("Plugin reloaded!")
    }
}