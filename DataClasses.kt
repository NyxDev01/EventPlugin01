package com.eventplugin.data

import org.bukkit.ChatColor
import java.util.*

// ==================== PLAYER DATA ====================
data class PlayerData(
    val uuid: UUID
) {
    // Current event stats
    var kills: Int = 0
    var points: Int = 0
    var killStreak: Int = 0
    var surgePops: Int = 0
    var isAlive: Boolean = false
    var isSpectator: Boolean = false

    // Persistent stats
    var totalWins: Int = 0
    var totalKills: Int = 0
    var totalDeaths: Int = 0
    var totalPoints: Int = 0
    var bestKillStreak: Int = 0

    // Kit data
    var selectedKits: MutableList<String> = mutableListOf("", "", "")
    var kitCooldown: Long = 0

    // Combat data
    var combatTaggedUntil: Long = 0

    // Admin data
    var isFrozen: Boolean = false
    var isVanished: Boolean = false
    var isBannedFromEvent: Boolean = false

    // Title system
    var selectedTitle: String? = null

    // Auto-rekit tracking
    var eventDeaths: Int = 0 // Deaths in current event for auto-rekit

    // Methods
    fun getKDRatio(): Double {
        return if (totalDeaths == 0) totalKills.toDouble()
        else totalKills.toDouble() / totalDeaths.toDouble()
    }

    fun getWinRate(): Double {
        val totalGames = totalWins + totalDeaths
        return if (totalGames == 0) 0.0
        else (totalWins.toDouble() / totalGames.toDouble()) * 100
    }

    fun canUseKit(): Boolean {
        return System.currentTimeMillis() >= kitCooldown
    }

    fun getRemainingKitCooldown(): Int {
        val remaining = (kitCooldown - System.currentTimeMillis()) / 1000
        return if (remaining > 0) remaining.toInt() else 0
    }

    fun setKitCooldown(seconds: Int) {
        kitCooldown = System.currentTimeMillis() + (seconds * 1000L)
    }

    fun isInCombat(): Boolean {
        return System.currentTimeMillis() < combatTaggedUntil
    }

    fun getRemainingCombatTime(): Int {
        val remaining = (combatTaggedUntil - System.currentTimeMillis()) / 1000
        return if (remaining > 0) remaining.toInt() else 0
    }

    fun setCombatTag(seconds: Int) {
        combatTaggedUntil = System.currentTimeMillis() + (seconds * 1000L)
    }

    fun clearCombatTag() {
        combatTaggedUntil = 0
    }

    fun getTitleColor(): ChatColor {
        return when (selectedTitle?.lowercase()) {
            "crystalranked" -> ChatColor.AQUA
            "aurora" -> ChatColor.LIGHT_PURPLE
            "mythic" -> ChatColor.GOLD
            "legend" -> ChatColor.RED
            "champion" -> ChatColor.YELLOW
            "master" -> ChatColor.DARK_PURPLE
            "elite" -> ChatColor.BLUE
            "veteran" -> ChatColor.GREEN
            else -> ChatColor.WHITE
        }
    }

    fun getFormattedTitle(): String {
        return if (selectedTitle != null) {
            "${getTitleColor()}§l[${selectedTitle!!.uppercase()}] §r"
        } else ""
    }
}

// ==================== PARTY DATA ====================
data class PartyData(
    val partyId: UUID = UUID.randomUUID(),
    var leader: UUID,
    var color: ChatColor = ChatColor.AQUA
) {
    val members: MutableSet<UUID> = mutableSetOf(leader)
    val invites: MutableSet<UUID> = mutableSetOf()

    fun addMember(uuid: UUID) {
        members.add(uuid)
        invites.remove(uuid)
    }

    fun removeMember(uuid: UUID) {
        members.remove(uuid)
    }

    fun addInvite(uuid: UUID) {
        invites.add(uuid)
    }

    fun hasInvite(uuid: UUID): Boolean {
        return invites.contains(uuid)
    }

    fun isMember(uuid: UUID): Boolean {
        return members.contains(uuid)
    }

    fun isLeader(uuid: UUID): Boolean {
        return leader == uuid
    }

    fun isFull(maxSize: Int): Boolean {
        return members.size >= maxSize
    }

    fun getSize(): Int {
        return members.size
    }
}