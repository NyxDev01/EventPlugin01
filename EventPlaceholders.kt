package com.eventplugin.placeholders

import com.eventplugin.EventPlugin
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer

class EventPlaceholders(private val plugin: EventPlugin) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "eventplugin"

    override fun getAuthor(): String = "EventPlugin"

    override fun getVersion(): String = plugin.description.version

    override fun persist(): Boolean = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (player == null) return handleGlobalPlaceholder(params)

        val data = plugin.statsManager.getPlayerData(player.uniqueId)
        val party = plugin.partyManager.getParty(player.uniqueId)

        return when (params.lowercase()) {
            // ==================== PLAYER STATS ====================
            "kills" -> data.kills.toString()
            "points" -> data.points.toString()
            "wins" -> data.totalWins.toString()
            "deaths" -> data.totalDeaths.toString()
            "total_kills" -> data.totalKills.toString()
            "kd" -> String.format("%.2f", data.getKDRatio())
            "killstreak" -> data.killStreak.toString()
            "best_killstreak" -> data.bestKillStreak.toString()
            "win_rate" -> String.format("%.1f", data.getWinRate())
            "total_points" -> data.totalPoints.toString()
            "games_played" -> (data.totalWins + data.totalDeaths).toString()

            // ==================== PLAYER STATUS ====================
            "is_alive" -> data.isAlive.toString()
            "is_spectator" -> data.isSpectator.toString()
            "is_frozen" -> data.isFrozen.toString()
            "is_vanished" -> data.isVanished.toString()
            "is_in_combat" -> data.isInCombat().toString()
            "combat_time" -> if (data.isInCombat()) data.getRemainingCombatTime().toString() else "0"

            // ==================== KIT INFO ====================
            "kit_cooldown" -> if (data.canUseKit()) "0" else data.getRemainingKitCooldown().toString()
            "kit1" -> data.selectedKits.getOrNull(0) ?: "None"
            "kit2" -> data.selectedKits.getOrNull(1) ?: "None"
            "kit3" -> data.selectedKits.getOrNull(2) ?: "None"
            "can_use_kit" -> data.canUseKit().toString()

            // ==================== SURGE INFO ====================
            "surge_pops" -> data.surgePops.toString()
            "surge_required" -> plugin.surgeManager.getRequiredPops().toString()
            "surge_remaining" -> maxOf(0, plugin.surgeManager.getRequiredPops() - data.surgePops).toString()
            "surge_enabled" -> plugin.surgeManager.isEnabled().toString()

            // ==================== PARTY INFO ====================
            "party_size" -> (party?.members?.size ?: 0).toString()
            "party_leader" -> if (party != null) {
                Bukkit.getOfflinePlayer(party.leader).name ?: "Unknown"
            } else "None"
            "party_alive" -> (party?.members?.count { uuid ->
                plugin.statsManager.getPlayerData(uuid).isAlive
            } ?: 0).toString()
            "has_party" -> (party != null).toString()
            "is_party_leader" -> (party?.isLeader(player.uniqueId) ?: false).toString()
            "party_color" -> party?.color?.toString() ?: "§7"

            // ==================== RANKINGS ====================
            "rank_wins" -> plugin.statsManager.getPlayerRankByWins(player.uniqueId).toString()
            "rank_kills" -> plugin.statsManager.getPlayerRankByKills(player.uniqueId).toString()
            "rank_points" -> plugin.statsManager.getPlayerRankByPoints(player.uniqueId).toString()

            // ==================== EVENT STATUS ====================
            "event_active" -> plugin.eventManager.isEventActive().toString()
            "event_mode" -> plugin.eventManager.getEventMode()
            "event_state" -> plugin.eventManager.getEventState()
            "grace_period" -> if (plugin.eventManager.isGracePeriodActive())
                plugin.eventManager.getGracePeriodRemaining().toString() else "0"
            "players_alive" -> plugin.eventManager.getAlivePlayers().size.toString()
            "spectators" -> plugin.eventManager.getSpectators().size.toString()
            "total_players" -> Bukkit.getOnlinePlayers().size.toString()

            // ==================== BORDER INFO ====================
            "border_size" -> {
                val world = player.player?.world ?: Bukkit.getWorlds().firstOrNull()
                world?.worldBorder?.size?.toInt()?.toString() ?: "0"
            }
            "border_phase" -> plugin.borderManager.getCurrentPhase().toString()
            "border_center_x" -> {
                val world = player.player?.world ?: Bukkit.getWorlds().firstOrNull()
                world?.worldBorder?.center?.blockX?.toString() ?: "0"
            }
            "border_center_z" -> {
                val world = player.player?.world ?: Bukkit.getWorlds().firstOrNull()
                world?.worldBorder?.center?.blockZ?.toString() ?: "0"
            }

            // ==================== LEADERBOARD TOP 10 ====================
            "top_wins_1" -> getTopPlayer(plugin.statsManager.getTopPlayersByWins(10), 0)
            "top_wins_2" -> getTopPlayer(plugin.statsManager.getTopPlayersByWins(10), 1)
            "top_wins_3" -> getTopPlayer(plugin.statsManager.getTopPlayersByWins(10), 2)
            "top_wins_4" -> getTopPlayer(plugin.statsManager.getTopPlayersByWins(10), 3)
            "top_wins_5" -> getTopPlayer(plugin.statsManager.getTopPlayersByWins(10), 4)

            "top_kills_1" -> getTopPlayer(plugin.statsManager.getTopPlayersByKills(10), 0)
            "top_kills_2" -> getTopPlayer(plugin.statsManager.getTopPlayersByKills(10), 1)
            "top_kills_3" -> getTopPlayer(plugin.statsManager.getTopPlayersByKills(10), 2)
            "top_kills_4" -> getTopPlayer(plugin.statsManager.getTopPlayersByKills(10), 3)
            "top_kills_5" -> getTopPlayer(plugin.statsManager.getTopPlayersByKills(10), 4)

            "top_kd_1" -> getTopPlayer(plugin.statsManager.getTopPlayersByKD(10), 0)
            "top_kd_2" -> getTopPlayer(plugin.statsManager.getTopPlayersByKD(10), 1)
            "top_kd_3" -> getTopPlayer(plugin.statsManager.getTopPlayersByKD(10), 2)
            "top_kd_4" -> getTopPlayer(plugin.statsManager.getTopPlayersByKD(10), 3)
            "top_kd_5" -> getTopPlayer(plugin.statsManager.getTopPlayersByKD(10), 4)

            else -> null
        }
    }

    private fun handleGlobalPlaceholder(params: String): String? {
        return when (params.lowercase()) {
            "event_active" -> plugin.eventManager.isEventActive().toString()
            "event_mode" -> plugin.eventManager.getEventMode()
            "players_alive" -> plugin.eventManager.getAlivePlayers().size.toString()
            "spectators" -> plugin.eventManager.getSpectators().size.toString()
            "total_players" -> Bukkit.getOnlinePlayers().size.toString()
            "border_size" -> {
                val world = Bukkit.getWorlds().firstOrNull()
                world?.worldBorder?.size?.toInt()?.toString() ?: "0"
            }
            "surge_enabled" -> plugin.surgeManager.isEnabled().toString()
            "max_party_size" -> plugin.partyManager.getMaxPartySize().toString()
            else -> null
        }
    }

    private fun getTopPlayer(list: List<com.eventplugin.data.PlayerData>, index: Int): String {
        if (index >= list.size) return "None"
        val data = list[index]
        val name = Bukkit.getOfflinePlayer(data.uuid).name ?: "Unknown"
        return name
    }
}