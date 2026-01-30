package com.eventplugin.chat

import com.eventplugin.EventPlugin
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.util.*

class ChatFormatter(private val plugin: EventPlugin) {

    // Format player chat with all the fancy stuff
    fun formatChat(player: Player, message: String): String {
        val data = plugin.statsManager.getPlayerData(player.uniqueId)
        val party = plugin.partyManager.getParty(player.uniqueId)

        // Build the prefix
        val prefix = buildString {
            // Add rank/status badges
            if (player.hasPermission("event.admin")) {
                append("§c§l[ADMIN] ")
            } else if (player.hasPermission("event.staff")) {
                append("§6§l[STAFF] ")
            } else if (player.hasPermission("event.vip")) {
                append("§b§l[VIP] ")
            }

            // Add event status
            if (plugin.eventManager.isEventActive()) {
                when {
                    data.isSpectator -> append("§7[SPEC] ")
                    data.isAlive -> {
                        // Add kill count badge
                        when {
                            data.kills >= 10 -> append("§c§l[${data.kills}] ")
                            data.kills >= 5 -> append("§6§l[${data.kills}] ")
                            data.kills > 0 -> append("§e[${data.kills}] ")
                        }
                    }
                }
            }

            // Add party indicator
            if (party != null) {
                append("${party.color}§l[P] §r")
            }

            // Add killstreak badge
            if (data.killStreak >= 5) {
                when {
                    data.killStreak >= 15 -> append("§c§l★★★ ")
                    data.killStreak >= 10 -> append("§6§l★★ ")
                    data.killStreak >= 5 -> append("§e§l★ ")
                }
            }
        }

        // Get player display name
        val displayName = getColoredName(player, data, party)

        // Format the message
        return "$prefix$displayName§8: §f$message"
    }

    private fun getColoredName(player: Player, data: com.eventplugin.data.PlayerData, party: com.eventplugin.data.PartyData?): String {
        // Color based on party
        if (party != null) {
            return "${party.color}${player.name}§r"
        }

        // Color based on status
        return when {
            data.isSpectator -> "§7${player.name}§r"
            data.kills >= 10 -> "§c${player.name}§r"
            data.kills >= 5 -> "§6${player.name}§r"
            data.isAlive -> "§a${player.name}§r"
            else -> "§f${player.name}§r"
        }
    }

    // Format party chat
    fun formatPartyChat(player: Player, message: String, party: com.eventplugin.data.PartyData): String {
        val data = plugin.statsManager.getPlayerData(player.uniqueId)

        val leaderBadge = if (party.isLeader(player.uniqueId)) {
            "${party.color}§l[LEADER] "
        } else {
            ""
        }

        val kills = if (data.kills > 0) {
            "${party.color}[${data.kills}] "
        } else {
            ""
        }

        return "${party.color}§l[PARTY] §r$leaderBadge$kills${party.color}${player.name}§8: §f$message"
    }

    // Format staff chat
    fun formatStaffChat(player: Player, message: String): String {
        val rank = when {
            player.hasPermission("event.admin") -> "§c§l[ADMIN]"
            player.hasPermission("event.staff") -> "§6§l[STAFF]"
            else -> "§7§l[MOD]"
        }

        return "§c§l[STAFF] §r$rank §f${player.name}§8: §7$message"
    }

    // Format spectator chat
    fun formatSpectatorChat(player: Player, message: String): String {
        return "§7§l[SPECTATOR] §7${player.name}§8: §7$message"
    }

    // Get player tab list name (for tab formatting)
    fun getTabListName(player: Player): String {
        val data = plugin.statsManager.getPlayerData(player.uniqueId)
        val party = plugin.partyManager.getParty(player.uniqueId)

        return buildString {
            // Prefix with status
            if (plugin.eventManager.isEventActive()) {
                when {
                    data.isSpectator -> append("§7")
                    party != null -> append(party.color.toString())
                    data.isAlive && data.kills >= 5 -> append("§c")
                    data.isAlive -> append("§a")
                    else -> append("§7")
                }
            } else {
                append("§f")
            }

            // Add name
            append(player.name)

            // Add suffix with kills
            if (plugin.eventManager.isEventActive() && data.isAlive && data.kills > 0) {
                append(" §7[§c${data.kills}§7]")
            }
        }
    }

    // Death message formatter
    fun formatDeathMessage(victim: Player, killer: Player?, finalKiller: Player?): String {
        val victimData = plugin.statsManager.getPlayerData(victim.uniqueId)
        val victimParty = plugin.partyManager.getParty(victim.uniqueId)

        val victimColor = victimParty?.color ?: ChatColor.RED
        val victimName = "$victimColor${victim.name}§r"

        return when {
            killer != null && finalKiller != null && killer != finalKiller -> {
                // Assisted kill
                val killerData = plugin.statsManager.getPlayerData(killer.uniqueId)
                val finalData = plugin.statsManager.getPlayerData(finalKiller.uniqueId)
                val killerParty = plugin.partyManager.getParty(killer.uniqueId)
                val finalParty = plugin.partyManager.getParty(finalKiller.uniqueId)

                val killerColor = killerParty?.color ?: ChatColor.GREEN
                val finalColor = finalParty?.color ?: ChatColor.GREEN

                "§7☠ $victimName §7was slain by ${finalColor}${finalKiller.name}§r §7with help from ${killerColor}${killer.name}§r §8[§6+${victimData.points}§8]"
            }
            killer != null -> {
                // Single kill
                val killerData = plugin.statsManager.getPlayerData(killer.uniqueId)
                val killerParty = plugin.partyManager.getParty(killer.uniqueId)
                val killerColor = killerParty?.color ?: ChatColor.GREEN

                val weapon = killer.inventory.itemInMainHand
                val weaponName = if (weapon.hasItemMeta() && weapon.itemMeta!!.hasDisplayName()) {
                    weapon.itemMeta!!.displayName
                } else {
                    when (weapon.type) {
                        org.bukkit.Material.DIAMOND_SWORD -> "§bDiamond Sword"
                        org.bukkit.Material.NETHERITE_SWORD -> "§5Netherite Sword"
                        org.bukkit.Material.IRON_SWORD -> "§fIron Sword"
                        org.bukkit.Material.BOW -> "§6Bow"
                        org.bukkit.Material.CROSSBOW -> "§6Crossbow"
                        org.bukkit.Material.TRIDENT -> "§bTrident"
                        else -> "§7Fists"
                    }
                }

                val streakText = if (killerData.killStreak >= 5) {
                    " §7[§e${killerData.killStreak} STREAK§7]"
                } else ""

                "§7☠ $victimName §7was slain by ${killerColor}${killer.name}§r §7using $weaponName §8[§6+${victimData.points}§8]$streakText"
            }
            else -> {
                // Natural death
                "§7☠ $victimName §7died"
            }
        }
    }

    // Kill announcement with fancy formatting
    fun announceKill(killer: Player, victim: Player, streak: Int, points: Int) {
        val killerData = plugin.statsManager.getPlayerData(killer.uniqueId)
        val killerParty = plugin.partyManager.getParty(killer.uniqueId)
        val killerColor = killerParty?.color ?: ChatColor.GREEN

        // First blood
        if (killerData.totalKills == 1 && plugin.config.getBoolean("killstreak.announce-first-blood", true)) {
            killer.sendTitle(
                "§c§l⚔ FIRST BLOOD ⚔",
                "§e+$points points",
                10, 40, 20
            )
        }

        // Killstreak milestones
        when (streak) {
            5 -> {
                org.bukkit.Bukkit.broadcastMessage("§6§l⚡ ${killerColor}${killer.name} §6§lis on a §e§l5 KILL STREAK§6§l!")
                killer.playSound(killer.location, org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.5f)
            }
            10 -> {
                org.bukkit.Bukkit.broadcastMessage("§c§l⚡⚡ ${killerColor}${killer.name} §c§lis §4§lUNSTOPPABLE §c§lwith §4§l10 KILLS§c§l!")
                killer.playSound(killer.location, org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.2f)
                killer.sendTitle("§4§l⚡ UNSTOPPABLE ⚡", "§c10 Kill Streak!", 10, 50, 20)
            }
            15 -> {
                org.bukkit.Bukkit.broadcastMessage("§4§l⚡⚡⚡ ${killerColor}${killer.name} §4§lis §c§lGODLIKE §4§lwith §c§l15 KILLS§4§l!")
                killer.playSound(killer.location, org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f)
                killer.sendTitle("§c§l⚡⚡ GODLIKE ⚡⚡", "§415 Kill Streak!", 10, 60, 20)
            }
            20 -> {
                org.bukkit.Bukkit.broadcastMessage("§c§l⚡⚡⚡⚡ ${killerColor}${killer.name} §c§lis §4§l§nLEGENDARY§c§l with §4§l20 KILLS§c§l!")
                killer.playSound(killer.location, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
                killer.sendTitle("§4§l⚡⚡⚡ LEGENDARY ⚡⚡⚡", "§c20 KILL STREAK!", 10, 70, 20)
            }
        }
    }

    // Broadcast fancy event messages
    fun broadcastEventStart(gracePeriod: Int, playerCount: Int) {
        org.bukkit.Bukkit.getOnlinePlayers().forEach { player ->
            player.sendTitle(
                "§6§l⚔ EVENT STARTING ⚔",
                "§e$playerCount players • ${gracePeriod}s grace period",
                10, 60, 20
            )
            player.playSound(player.location, org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f)
        }

        org.bukkit.Bukkit.broadcastMessage("")
        org.bukkit.Bukkit.broadcastMessage("§6§l════════════════════════════════")
        org.bukkit.Bukkit.broadcastMessage("§e§l         EVENT STARTING!")
        org.bukkit.Bukkit.broadcastMessage("")
        org.bukkit.Bukkit.broadcastMessage("  §7Players: §f$playerCount")
        org.bukkit.Bukkit.broadcastMessage("  §7Grace Period: §e${gracePeriod}s")
        org.bukkit.Bukkit.broadcastMessage("  §7Mode: §6${plugin.eventManager.getEventMode()}")
        org.bukkit.Bukkit.broadcastMessage("")
        org.bukkit.Bukkit.broadcastMessage("§6§l════════════════════════════════")
        org.bukkit.Bukkit.broadcastMessage("")
    }

    fun broadcastGracePeriodEnd() {
        org.bukkit.Bukkit.getOnlinePlayers().forEach { player ->
            player.sendTitle(
                "§c§l⚔ PVP ENABLED ⚔",
                "§eFight to survive!",
                10, 50, 20
            )
            player.playSound(player.location, org.bukkit.Sound.BLOCK_BELL_USE, 1f, 0.8f)
        }

        org.bukkit.Bukkit.broadcastMessage("")
        org.bukkit.Bukkit.broadcastMessage("§c§l⚔ GRACE PERIOD ENDED - PVP ENABLED! ⚔")
        org.bukkit.Bukkit.broadcastMessage("")
    }

    fun broadcastWinner(winner: Player) {
        val winnerData = plugin.statsManager.getPlayerData(winner.uniqueId)
        val party = plugin.partyManager.getParty(winner.uniqueId)

        org.bukkit.Bukkit.getOnlinePlayers().forEach { player ->
            player.sendTitle(
                "§6§l👑 WINNER 👑",
                "${party?.color ?: "§e"}${winner.name}",
                10, 100, 30
            )
            player.playSound(player.location, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
        }

        org.bukkit.Bukkit.broadcastMessage("")
        org.bukkit.Bukkit.broadcastMessage("§6§l════════════════════════════════")
        org.bukkit.Bukkit.broadcastMessage("§e§l           WINNER!")
        org.bukkit.Bukkit.broadcastMessage("")
        org.bukkit.Bukkit.broadcastMessage("  §7Player: ${party?.color ?: "§e"}§l${winner.name}")
        org.bukkit.Bukkit.broadcastMessage("  §7Kills: §c${winnerData.kills}")
        org.bukkit.Bukkit.broadcastMessage("  §7Points: §6${winnerData.points}")
        if (party != null) {
            org.bukkit.Bukkit.broadcastMessage("  §7Party: ${party.color}${party.members.size} members")
        }
        org.bukkit.Bukkit.broadcastMessage("")
        org.bukkit.Bukkit.broadcastMessage("§6§l════════════════════════════════")
        org.bukkit.Bukkit.broadcastMessage("")
    }

    fun broadcastBorderShrink(phase: Int, size: Int, time: Int) {
        val playersAlive = plugin.eventManager.getAlivePlayerCount()

        org.bukkit.Bukkit.getOnlinePlayers().forEach { player ->
            player.playSound(player.location, org.bukkit.Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.8f)
        }

        org.bukkit.Bukkit.broadcastMessage("")
        org.bukkit.Bukkit.broadcastMessage("§c§l⚠ BORDER SHRINKING ⚠")
        org.bukkit.Bukkit.broadcastMessage("§7Phase §e$phase §7→ Size: §c${size} blocks §7(${time}s)")
        org.bukkit.Bukkit.broadcastMessage("§7Players Remaining: §a$playersAlive")
        org.bukkit.Bukkit.broadcastMessage("")
    }
}