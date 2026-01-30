package com.eventplugin.managers

import com.eventplugin.EventPlugin
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import java.util.*

class EventManager(private val plugin: EventPlugin) {

    private var eventActive = false
    private var eventPaused = false
    private var eventMode: String = "FFA" // FFA, PARTY, etc.
    private var gracePeriodActive = false
    private var gracePeriodEndTime: Long = 0

    private val alivePlayers = mutableSetOf<UUID>()
    private val spectators = mutableSetOf<UUID>()

    companion object {
        private var instance: EventManager? = null

        fun getInstance(): EventManager {
            return instance ?: throw IllegalStateException("EventManager not initialized")
        }

        fun setInstance(manager: EventManager) {
            instance = manager
        }
    }

    init {
        setInstance(this)
    }

    // Event state management - FIXED to match command calls
    fun startEvent(force: Boolean = false, mode: String = "FFA", gracePeriod: Int = 0): Boolean {
        if (eventActive) {
            return false
        }

        // Check minimum players unless forced
        if (!force && Bukkit.getOnlinePlayers().size < plugin.config.getInt("event.min-players", 2)) {
            return false
        }

        eventActive = true
        eventPaused = false
        eventMode = mode
        alivePlayers.clear()
        spectators.clear()

        // Add all online players as alive
        Bukkit.getOnlinePlayers().forEach { player ->
            alivePlayers.add(player.uniqueId)
        }

        // Start grace period if specified
        if (gracePeriod > 0) {
            startGracePeriod(gracePeriod)
        }

        plugin.logger.info("Event started in $mode mode with ${alivePlayers.size} players")
        return true
    }

    fun stopEvent() {
        if (!eventActive) {
            return
        }

        eventActive = false
        eventPaused = false
        gracePeriodActive = false
        alivePlayers.clear()
        spectators.clear()

        plugin.logger.info("Event stopped")
    }

    fun pauseEvent() {
        if (!eventActive || eventPaused) {
            return
        }

        eventPaused = true
        plugin.logger.info("Event paused")
    }

    fun resumeEvent() {
        if (!eventActive || !eventPaused) {
            return
        }

        eventPaused = false
        plugin.logger.info("Event resumed")
    }

    // Grace period management
    private fun startGracePeriod(seconds: Int) {
        gracePeriodActive = true
        gracePeriodEndTime = System.currentTimeMillis() + (seconds * 1000L)
    }

    fun endGracePeriod() {
        gracePeriodActive = false
        gracePeriodEndTime = 0
    }

    fun getGracePeriodRemaining(): Int {
        if (!gracePeriodActive) {
            return 0
        }

        val remaining = ((gracePeriodEndTime - System.currentTimeMillis()) / 1000).toInt()

        if (remaining <= 0) {
            endGracePeriod()
            return 0
        }

        return remaining
    }

    // Player management
    fun addPlayer(player: Player) {
        alivePlayers.add(player.uniqueId)
        spectators.remove(player.uniqueId)
    }

    fun removePlayer(player: Player) {
        alivePlayers.remove(player.uniqueId)
    }

    fun addSpectator(player: Player) {
        spectators.add(player.uniqueId)
        alivePlayers.remove(player.uniqueId)

        player.gameMode = GameMode.SPECTATOR
        player.health = 20.0
        player.foodLevel = 20
    }

    fun removeSpectator(player: Player) {
        spectators.remove(player.uniqueId)
    }

    // Win condition checking
    fun checkWinCondition() {
        if (!eventActive || eventPaused) {
            return
        }

        when (eventMode.uppercase()) {
            "FFA" -> checkFFAWinCondition()
            "PARTY" -> checkPartyWinCondition()
            else -> checkFFAWinCondition()
        }
    }

    private fun checkFFAWinCondition() {
        if (alivePlayers.size <= 1) {
            val winner = alivePlayers.firstOrNull()?.let { Bukkit.getPlayer(it) }

            if (winner != null) {
                // Use fancy formatter
                plugin.chatFormatter.broadcastWinner(winner)
                plugin.statsManager.addWin(winner.uniqueId)
            } else {
                Bukkit.broadcastMessage("§6§lThe event has ended with no winner!")
            }

            stopEvent()
        }
    }

    private fun checkPartyWinCondition() {
        val aliveParties = alivePlayers.mapNotNull { uuid ->
            Bukkit.getPlayer(uuid)?.let { plugin.partyManager.getParty(it.uniqueId) }
        }.toSet()

        if (aliveParties.size <= 1) {
            val winningParty = aliveParties.firstOrNull()

            if (winningParty != null) {
                val leaderUuid = winningParty.leader
                val leader = Bukkit.getPlayer(leaderUuid)

                if (leader != null) {
                    plugin.chatFormatter.broadcastWinner(leader)
                }

                winningParty.members.forEach { memberId ->
                    plugin.statsManager.addWin(memberId)
                }
            } else {
                Bukkit.broadcastMessage("§6§lThe event has ended with no winner!")
            }

            stopEvent()
        }
    }

    // Getters
    fun isEventActive(): Boolean = eventActive

    fun isEventPaused(): Boolean = eventPaused

    fun getEventMode(): String = eventMode

    fun getEventState(): String {
        return when {
            !eventActive -> "INACTIVE"
            eventPaused -> "PAUSED"
            gracePeriodActive -> "GRACE_PERIOD"
            else -> "ACTIVE"
        }
    }

    fun isGracePeriodActive(): Boolean {
        if (gracePeriodActive && getGracePeriodRemaining() <= 0) {
            endGracePeriod()
        }
        return gracePeriodActive
    }

    fun getAlivePlayers(): Set<UUID> = alivePlayers.toSet()

    fun getSpectators(): Set<UUID> = spectators.toSet()

    fun isPlayerAlive(player: Player): Boolean = alivePlayers.contains(player.uniqueId)

    fun isPlayerSpectator(player: Player): Boolean = spectators.contains(player.uniqueId)

    fun getAlivePlayerCount(): Int = alivePlayers.size

    fun getSpectatorCount(): Int = spectators.size
}