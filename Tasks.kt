package com.eventplugin.tasks

import com.eventplugin.EventPlugin
import com.eventplugin.managers.sendActionBar
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.scheduler.BukkitTask

// ==================== SCOREBOARD TASK (FIXED) ====================
class ScoreboardTask(private val plugin: EventPlugin) {

    private var task: BukkitTask? = null

    fun start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            updateScoreboards()
        }, 0L, 20L) // Update every second
    }

    fun stop() {
        task?.cancel()
    }

    private fun updateScoreboards() {
        if (!plugin.config.getBoolean("scoreboard.enabled", true)) return

        Bukkit.getOnlinePlayers().forEach { player ->
            val data = plugin.statsManager.getPlayerData(player.uniqueId)
            val party = plugin.partyManager.getParty(player.uniqueId)

            // Create or get scoreboard
            val board = player.scoreboard ?: Bukkit.getScoreboardManager()?.newScoreboard ?: return@forEach

            var objective = board.getObjective("eventplugin")
            if (objective == null) {
                objective = board.registerNewObjective("eventplugin", "dummy", "§6§lEVENT STATS")
                objective.displaySlot = org.bukkit.scoreboard.DisplaySlot.SIDEBAR
            }

            // Clear old scores
            board.entries.forEach { entry ->
                board.resetScores(entry)
            }

            var line = 15

            // Title with player's custom title if they have one
            val titleDisplay = if (data.selectedTitle != null) {
                "${data.getTitleColor()}§l${data.selectedTitle!!.uppercase()}"
            } else {
                "§6§lEVENT STATS"
            }

            // Spacer
            objective.getScore("§8§m--------------------").score = line--

            // Event status
            if (plugin.eventManager.isEventActive()) {
                objective.getScore("§6§lEVENT ACTIVE").score = line--
                objective.getScore("§7Alive: §a${plugin.eventManager.getAlivePlayers().size}").score = line--
                objective.getScore("§7Border: §c${player.world.worldBorder.size.toInt()}").score = line--
                objective.getScore("").score = line--
            } else {
                objective.getScore("§7§lWAITING...").score = line--
                objective.getScore(" ").score = line--
            }

            // Player stats
            objective.getScore("§e§lYour Stats:").score = line--
            objective.getScore("§7Kills: §c${data.kills}").score = line--
            objective.getScore("§7Deaths: §8${data.eventDeaths}").score = line--
            objective.getScore("§7Points: §6${data.points}").score = line--

            if (data.killStreak > 0) {
                objective.getScore("§7Streak: §e${data.killStreak} §6★").score = line--
            }

            objective.getScore("  ").score = line--

            // Party info
            if (party != null) {
                val partyAlive = party.members.count { plugin.statsManager.getPlayerData(it).isAlive }
                objective.getScore("${party.color}§lParty (${party.members.size})").score = line--
                objective.getScore("§7Alive: §a$partyAlive§7/§f${party.members.size}").score = line--
                objective.getScore("   ").score = line--
            }

            // Surge info
            if (plugin.surgeManager.isEnabled() && plugin.eventManager.isEventActive()) {
                val required = plugin.surgeManager.getRequiredPops()
                val current = data.surgePops
                val color = if (current >= required) "§a" else "§c"
                objective.getScore("§6§lSurge:").score = line--
                objective.getScore("$color$current§7/$required pops").score = line--
                objective.getScore("    ").score = line--
            }

            // Overall stats
            objective.getScore("§b§lOverall:").score = line--
            objective.getScore("§7Wins: §e${data.totalWins}").score = line--
            objective.getScore("§7K/D: §f${String.format("%.2f", data.getKDRatio())}").score = line--

            // Bottom spacer
            objective.getScore("§8§m--------------------").score = line--

            player.scoreboard = board
        }
    }
}

// ==================== BOSS BAR TASK ====================
class BossBarTask(private val plugin: EventPlugin) {

    private var task: BukkitTask? = null
    private val bossBars = mutableMapOf<String, BossBar>()

    fun start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            updateBossBars()
        }, 0L, 10L) // Update twice per second
    }

    fun stop() {
        task?.cancel()
        bossBars.values.forEach { it.removeAll() }
        bossBars.clear()
    }

    private fun updateBossBars() {
        if (!plugin.config.getBoolean("bossbar.enabled", true)) return

        Bukkit.getOnlinePlayers().forEach { player ->
            val data = plugin.statsManager.getPlayerData(player.uniqueId)

            // Grace period boss bar
            if (plugin.eventManager.isGracePeriodActive()) {
                val remaining = plugin.eventManager.getGracePeriodRemaining()
                val total = plugin.config.getInt("event.grace-period", 60)
                val progress = remaining.toDouble() / total.toDouble()

                val bar = getBossBar("grace")
                bar.setTitle("§e§lGRACE PERIOD: §f${remaining}s")
                bar.color = BarColor.YELLOW
                bar.style = BarStyle.SOLID
                bar.progress = progress.coerceIn(0.0, 1.0)

                if (!bar.players.contains(player)) {
                    bar.addPlayer(player)
                }
            } else {
                getBossBar("grace").removePlayer(player)
            }

            // Combat tag boss bar
            if (data.isInCombat()) {
                val remaining = data.getRemainingCombatTime()
                val total = plugin.config.getInt("combat.tag-duration", 15)
                val progress = remaining.toDouble() / total.toDouble()

                val bar = getBossBar("combat_${player.uniqueId}")
                bar.setTitle("§c§lCOMBAT TAG: §f${remaining}s")
                bar.color = BarColor.RED
                bar.style = BarStyle.SOLID
                bar.progress = progress.coerceIn(0.0, 1.0)

                if (!bar.players.contains(player)) {
                    bar.addPlayer(player)
                }
            } else {
                getBossBar("combat_${player.uniqueId}").removePlayer(player)
            }

            // Border shrinking boss bar
            if (plugin.eventManager.isEventActive()) {
                val border = player.world.worldBorder
                val size = border.size.toInt()

                val bar = getBossBar("border")
                bar.setTitle("§c§lBORDER: §f${size} blocks")
                bar.color = BarColor.RED
                bar.style = BarStyle.SEGMENTED_10
                bar.progress = (size.toDouble() / 300.0).coerceIn(0.0, 1.0)

                if (!bar.players.contains(player) && data.isAlive) {
                    bar.addPlayer(player)
                }
            } else {
                getBossBar("border").removePlayer(player)
            }
        }
    }

    private fun getBossBar(id: String): BossBar {
        return bossBars.getOrPut(id) {
            Bukkit.createBossBar("", BarColor.WHITE, BarStyle.SOLID)
        }
    }
}

// ==================== HEIGHT CHECK TASK ====================
class HeightCheckTask(private val plugin: EventPlugin) {

    private var task: BukkitTask? = null
    private val warnings = mutableMapOf<java.util.UUID, Long>()

    fun start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            checkHeights()
        }, 0L, 20L) // Check every second
    }

    fun stop() {
        task?.cancel()
        warnings.clear()
    }

    private fun checkHeights() {
        if (!plugin.eventManager.isEventActive()) return

        val maxHeight = plugin.config.getInt("event.max-height", 320)
        val warningHeight = plugin.config.getInt("event.warning-height", 300)
        val damage = plugin.config.getDouble("event.height-damage", 10.0)

        Bukkit.getOnlinePlayers().forEach { player ->
            val data = plugin.statsManager.getPlayerData(player.uniqueId)

            if (!data.isAlive || data.isSpectator) return@forEach

            val height = player.location.y

            when {
                height > maxHeight -> {
                    // Apply damage
                    player.damage(damage)
                    player.sendTitle("§c§lTOO HIGH!", "§eGet below Y=$maxHeight!", 0, 20, 10)
                    player.sendActionBar("§c§l⚠ HEIGHT LIMIT EXCEEDED! Taking damage! ⚠")
                }
                height > warningHeight -> {
                    // Warning
                    val now = System.currentTimeMillis()
                    val lastWarning = warnings[player.uniqueId] ?: 0

                    if (now - lastWarning > 3000) { // Warn every 3 seconds
                        player.sendActionBar("§e§lWARNING: §cYou are too high! Get below Y=$maxHeight")
                        player.playSound(player.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f)
                        warnings[player.uniqueId] = now
                    }
                }
                else -> {
                    warnings.remove(player.uniqueId)
                }
            }
        }
    }
}