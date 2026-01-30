package com.eventplugin.managers

import com.eventplugin.EventPlugin
import com.eventplugin.data.PlayerData
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

// ==================== STATS MANAGER ====================
class StatsManager(private val plugin: EventPlugin) {

    private val playerData = mutableMapOf<UUID, PlayerData>()

    fun getPlayerData(uuid: UUID): PlayerData {
        return playerData.getOrPut(uuid) { PlayerData(uuid) }
    }

    fun getAllPlayerData(): Collection<PlayerData> {
        return playerData.values
    }

    fun getTopPlayersByWins(limit: Int): List<PlayerData> {
        return playerData.values.sortedByDescending { it.totalWins }.take(limit)
    }

    fun getTopPlayersByKills(limit: Int): List<PlayerData> {
        return playerData.values.sortedByDescending { it.totalKills }.take(limit)
    }

    fun getTopPlayersByKD(limit: Int): List<PlayerData> {
        return playerData.values.sortedByDescending { it.getKDRatio() }.take(limit)
    }

    fun getPlayerRankByWins(uuid: UUID): Int {
        val sorted = playerData.values.sortedByDescending { it.totalWins }
        return sorted.indexOfFirst { it.uuid == uuid } + 1
    }

    fun getPlayerRankByKills(uuid: UUID): Int {
        val sorted = playerData.values.sortedByDescending { it.kills }
        return sorted.indexOfFirst { it.uuid == uuid } + 1
    }

    fun getPlayerRankByPoints(uuid: UUID): Int {
        val sorted = playerData.values.sortedByDescending { it.points }
        return sorted.indexOfFirst { it.uuid == uuid } + 1
    }

    // Add win to player
    fun addWin(uuid: UUID) {
        val data = getPlayerData(uuid)
        data.totalWins++
    }

    fun saveAll() {
        // Save to file/database
        // Implementation depends on storage choice
    }

    fun loadAll() {
        // Load from file/database
    }

    fun reload() {
        loadAll()
    }
}

// ==================== COMBAT MANAGER ====================
class CombatManager(private val plugin: EventPlugin) {

    private var tagDuration = 15
    private var preventLogout = true
    private var logoutKill = true

    fun tagPlayer(player: UUID, attacker: UUID) {
        val data = plugin.statsManager.getPlayerData(player)
        data.setCombatTag(tagDuration)

        val attackerData = plugin.statsManager.getPlayerData(attacker)
        attackerData.setCombatTag(tagDuration)

        // Send message
        Bukkit.getPlayer(player)?.sendActionBar("§c§lCOMBAT TAG! §eDo not log out!")
        Bukkit.getPlayer(attacker)?.sendActionBar("§c§lCOMBAT TAG! §eDo not log out!")
    }

    fun isInCombat(player: UUID): Boolean {
        return plugin.statsManager.getPlayerData(player).isInCombat()
    }

    fun clearCombatTag(player: UUID) {
        plugin.statsManager.getPlayerData(player).clearCombatTag()
    }

    fun handleLogout(player: Player): Boolean {
        if (!isInCombat(player.uniqueId)) return false

        if (preventLogout && logoutKill) {
            player.health = 0.0
            return true
        }

        return preventLogout
    }

    fun reload() {
        tagDuration = plugin.config.getInt("combat.tag-duration", 15)
        preventLogout = plugin.config.getBoolean("combat.prevent-logout", true)
        logoutKill = plugin.config.getBoolean("combat.logout-kill", true)
    }
}

// ==================== SPECTATOR MANAGER ====================
class SpectatorManager(private val plugin: EventPlugin) {

    private val spectators = mutableSetOf<UUID>()

    fun setSpectator(player: Player, spectator: Boolean) {
        val data = plugin.statsManager.getPlayerData(player.uniqueId)

        if (spectator) {
            spectators.add(player.uniqueId)
            data.isSpectator = true
            data.isAlive = false

            player.gameMode = GameMode.SPECTATOR

            if (plugin.config.getBoolean("spectator.flight-enabled", true)) {
                player.allowFlight = true
                player.isFlying = true
            }

            // Hide from alive players if configured
            if (plugin.config.getBoolean("spectator.hide-from-alive", true)) {
                Bukkit.getOnlinePlayers().forEach { other ->
                    val otherData = plugin.statsManager.getPlayerData(other.uniqueId)
                    if (otherData.isAlive) {
                        other.hidePlayer(plugin, player)
                    }
                }
            }

            // Remove invisibility
            player.removePotionEffect(PotionEffectType.INVISIBILITY)

            // Send message
            val msg = plugin.config.getString("messages.spectator-mode") ?: "§7You are now spectating."
            player.sendMessage(plugin.config.getString("messages.prefix") + msg)

        } else {
            spectators.remove(player.uniqueId)
            data.isSpectator = false

            player.gameMode = GameMode.SURVIVAL

            // Show to all players
            Bukkit.getOnlinePlayers().forEach { other ->
                other.showPlayer(plugin, player)
            }
        }
    }

    fun isSpectator(player: UUID): Boolean {
        return spectators.contains(player)
    }

    fun teleportToRandomPlayer(spectator: Player) {
        val alivePlayers = Bukkit.getOnlinePlayers().filter { player ->
            val data = plugin.statsManager.getPlayerData(player.uniqueId)
            data.isAlive && !data.isSpectator
        }

        if (alivePlayers.isEmpty()) {
            spectator.sendMessage("§c✖ No alive players to spectate!")
            return
        }

        val target = alivePlayers.random()
        spectator.teleport(target)
        spectator.sendMessage("§7Now spectating §e${target.name}")
    }

    fun clearAllSpectators() {
        spectators.toList().forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { setSpectator(it, false) }
        }
    }
}

// ==================== ADMIN MANAGER ====================
class AdminManager(private val plugin: EventPlugin) {

    private val frozenPlayers = mutableSetOf<UUID>()
    private val vanishedPlayers = mutableSetOf<UUID>()
    private val bannedPlayers = mutableSetOf<UUID>()
    private var whitelistMode = false
    private val whitelistedPlayers = mutableSetOf<UUID>()

    fun freezePlayer(player: UUID, freeze: Boolean) {
        if (freeze) {
            frozenPlayers.add(player)
            Bukkit.getPlayer(player)?.let {
                it.walkSpeed = 0f
                it.sendMessage("§c§lYou have been frozen by an administrator!")
            }
        } else {
            frozenPlayers.remove(player)
            Bukkit.getPlayer(player)?.let {
                it.walkSpeed = 0.2f
                it.sendMessage("§a§lYou have been unfrozen!")
            }
        }

        val data = plugin.statsManager.getPlayerData(player)
        data.isFrozen = freeze
    }

    fun isFrozen(player: UUID): Boolean {
        return frozenPlayers.contains(player)
    }

    fun vanishPlayer(player: Player, vanish: Boolean) {
        if (vanish) {
            vanishedPlayers.add(player.uniqueId)

            Bukkit.getOnlinePlayers().forEach { other ->
                if (!other.hasPermission("event.staff")) {
                    other.hidePlayer(plugin, player)
                }
            }

            player.sendMessage("§7You are now vanished.")

        } else {
            vanishedPlayers.remove(player.uniqueId)

            Bukkit.getOnlinePlayers().forEach { other ->
                other.showPlayer(plugin, player)
            }

            player.sendMessage("§7You are no longer vanished.")
        }

        val data = plugin.statsManager.getPlayerData(player.uniqueId)
        data.isVanished = vanish
    }

    fun isVanished(player: UUID): Boolean {
        return vanishedPlayers.contains(player)
    }

    fun banFromEvent(player: UUID, banned: Boolean) {
        if (banned) {
            bannedPlayers.add(player)
        } else {
            bannedPlayers.remove(player)
        }

        val data = plugin.statsManager.getPlayerData(player)
        data.isBannedFromEvent = banned
    }

    fun isBannedFromEvent(player: UUID): Boolean {
        return bannedPlayers.contains(player)
    }

    fun setWhitelistMode(enabled: Boolean) {
        whitelistMode = enabled
    }

    fun isWhitelistMode(): Boolean {
        return whitelistMode
    }

    fun addToWhitelist(player: UUID) {
        whitelistedPlayers.add(player)
    }

    fun removeFromWhitelist(player: UUID) {
        whitelistedPlayers.remove(player)
    }

    fun isWhitelisted(player: UUID): Boolean {
        return whitelistedPlayers.contains(player)
    }

    fun canJoinEvent(player: UUID): Boolean {
        if (isBannedFromEvent(player)) return false
        if (whitelistMode && !isWhitelisted(player)) return false
        return true
    }

    fun sendStaffAlert(message: String) {
        Bukkit.getOnlinePlayers()
            .filter { it.hasPermission("event.staff") }
            .forEach { it.sendMessage("§c§l[ALERT] §r$message") }
    }
}