package com.eventplugin.managers

import com.eventplugin.EventPlugin
import com.eventplugin.data.EventTemplate
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.*

// ==================== SPAWN MANAGER ====================
class SpawnManager(private val plugin: EventPlugin) {

    private val spawns = mutableMapOf<String, Location>()

    fun setSpawn(world: String, location: Location) {
        spawns[world] = location
        // Save to config
    }

    fun getSpawn(world: String): Location? {
        return spawns[world]
    }

    fun teleportToSpawn(player: Player): Boolean {
        val world = player.world.name
        val spawn = spawns[world] ?: return false

        player.teleport(spawn)
        return true
    }

    fun hasSpawn(world: String): Boolean {
        return spawns.containsKey(world)
    }

    fun reload() {
        // Load spawns from config
    }
}

// ==================== LEADERBOARD MANAGER ====================
class LeaderboardManager(private val plugin: EventPlugin) {

    private val leaderboards = mutableMapOf<Location, LeaderboardType>()

    enum class LeaderboardType {
        WINS, KILLS, KD, POINTS
    }

    data class LeaderboardEntry(
        val uuid: UUID,
        val name: String,
        val value: Double,
        val rank: Int
    )

    fun createLeaderboard(location: Location, type: LeaderboardType) {
        leaderboards[location] = type
        updateLeaderboard(location, type)
    }

    fun removeLeaderboard(location: Location) {
        leaderboards.remove(location)
        // Remove sign/hologram
    }

    fun updateLeaderboards() {
        leaderboards.forEach { (location, type) ->
            updateLeaderboard(location, type)
        }
    }

    private fun updateLeaderboard(location: Location, type: LeaderboardType) {
        val entries = when (type) {
            LeaderboardType.WINS -> {
                plugin.statsManager.getTopPlayersByWins(10).mapIndexed { index, data ->
                    LeaderboardEntry(
                        uuid = data.uuid,
                        name = Bukkit.getOfflinePlayer(data.uuid).name ?: "Unknown",
                        value = data.totalWins.toDouble(),
                        rank = index + 1
                    )
                }
            }
            LeaderboardType.KILLS -> {
                plugin.statsManager.getTopPlayersByKills(10).mapIndexed { index, data ->
                    LeaderboardEntry(
                        uuid = data.uuid,
                        name = Bukkit.getOfflinePlayer(data.uuid).name ?: "Unknown",
                        value = data.totalKills.toDouble(),
                        rank = index + 1
                    )
                }
            }
            LeaderboardType.KD -> {
                plugin.statsManager.getTopPlayersByKD(10).mapIndexed { index, data ->
                    LeaderboardEntry(
                        uuid = data.uuid,
                        name = Bukkit.getOfflinePlayer(data.uuid).name ?: "Unknown",
                        value = data.getKDRatio(),
                        rank = index + 1
                    )
                }
            }
            LeaderboardType.POINTS -> {
                plugin.statsManager.getAllPlayerData()
                    .sortedByDescending { it.totalPoints }
                    .take(10)
                    .mapIndexed { index, data ->
                        LeaderboardEntry(
                            uuid = data.uuid,
                            name = Bukkit.getOfflinePlayer(data.uuid).name ?: "Unknown",
                            value = data.totalPoints.toDouble(),
                            rank = index + 1
                        )
                    }
            }
        }

        // Update sign at location
        updateSign(location, type, entries)
    }

    private fun updateSign(location: Location, type: LeaderboardType, entries: List<LeaderboardEntry>) {
        val block = location.block
        if (block.type != Material.OAK_WALL_SIGN && block.type != Material.OAK_SIGN) {
            return
        }

        val sign = block.state as? org.bukkit.block.Sign ?: return

        val title = when (type) {
            LeaderboardType.WINS -> "§6§lTOP WINS"
            LeaderboardType.KILLS -> "§c§lTOP KILLS"
            LeaderboardType.KD -> "§e§lTOP K/D"
            LeaderboardType.POINTS -> "§a§lTOP POINTS"
        }

        sign.setLine(0, title)

        entries.take(3).forEachIndexed { index, entry ->
            val medal = when (index) {
                0 -> "§6§l1."
                1 -> "§7§l2."
                2 -> "§c§l3."
                else -> "§f${index + 1}."
            }
            sign.setLine(index + 1, "$medal §f${entry.name} §7${entry.value.toInt()}")
        }

        sign.update()
    }
}

// ==================== WORLD MANAGER ====================
class WorldManager(private val plugin: EventPlugin) {

    private var eventWorld: World? = null

    fun regenerateWorld() {
        val worldName = plugin.config.getString("world.event-world-name", "world") ?: "world"
        eventWorld = Bukkit.getWorld(worldName)

        if (eventWorld == null) return

        // Clear entities
        if (plugin.config.getBoolean("world.clear-entities-on-reset", true)) {
            eventWorld?.entities?.forEach { entity ->
                if (entity !is Player) {
                    entity.remove()
                }
            }
        }

        // Clear drops
        if (plugin.config.getBoolean("world.clear-drops-on-reset", true)) {
            eventWorld?.entities?.forEach { entity ->
                if (entity is org.bukkit.entity.Item) {
                    entity.remove()
                }
            }
        }

        Bukkit.broadcastMessage("§a§lWorld has been regenerated!")
    }

    fun clearBlocks(materials: List<Material>) {
        val worldName = plugin.config.getString("world.event-world-name", "world") ?: "world"
        val world = Bukkit.getWorld(worldName) ?: return
        val border = world.worldBorder

        val centerX = border.center.blockX
        val centerZ = border.center.blockZ
        val radius = (border.size / 2).toInt()

        var cleared = 0

        for (x in (centerX - radius)..(centerX + radius)) {
            for (z in (centerZ - radius)..(centerZ + radius)) {
                for (y in -64..319) { // 1.21 world height
                    val block = world.getBlockAt(x, y, z)
                    if (materials.contains(block.type)) {
                        block.type = Material.AIR
                        cleared++
                    }
                }
            }
        }

        Bukkit.broadcastMessage("§a§lCleared $cleared blocks!")
    }

    fun rollbackWorld() {
        // This would require a backup system
        // For now, just regenerate
        regenerateWorld()
    }
}

// ==================== TEMPLATE MANAGER ====================
class TemplateManager(private val plugin: EventPlugin) {

    private val templates = mutableMapOf<String, EventTemplate>()

    fun createTemplate(name: String): EventTemplate {
        val template = EventTemplate(
            name = name,
            eventMode = plugin.eventManager.getEventMode(),
            minPlayers = plugin.config.getInt("event.min-players"),
            gracePeriod = plugin.config.getInt("event.grace-period"),
            borderPhases = mutableMapOf(),
            kitSettings = mutableMapOf(),
            surgeSettings = mutableMapOf(),
            partySettings = mutableMapOf()
        )

        // Copy current border settings
        for (i in 1..11) {
            val size = plugin.config.getInt("border.phases.$i.size")
            val time = plugin.config.getInt("border.phases.$i.default-time")
            template.borderPhases[i] = Pair(size, time)
        }

        templates[name.lowercase()] = template
        return template
    }

    fun deleteTemplate(name: String): Boolean {
        return templates.remove(name.lowercase()) != null
    }

    fun getTemplate(name: String): EventTemplate? {
        return templates[name.lowercase()]
    }

    fun loadTemplate(name: String): Boolean {
        val template = templates[name.lowercase()] ?: return false

        // Apply template settings to config
        plugin.config.set("event.min-players", template.minPlayers)
        plugin.config.set("event.grace-period", template.gracePeriod)

        template.borderPhases.forEach { (phase, settings) ->
            plugin.config.set("border.phases.$phase.size", settings.first)
            plugin.config.set("border.phases.$phase.default-time", settings.second)
        }

        plugin.saveConfig()
        plugin.reload()

        return true
    }

    fun getAllTemplates(): Set<String> {
        return templates.keys
    }

    fun copyToClipboard(template: EventTemplate): String {
        // Serialize template to JSON or similar
        return "Template: ${template.name} - Mode: ${template.eventMode}"
    }

    fun pasteFromClipboard(data: String): EventTemplate? {
        // Deserialize template from string
        return null
    }

    fun saveAll() {
        // Save templates to file
    }

    fun loadAll() {
        // Load templates from file
    }
}