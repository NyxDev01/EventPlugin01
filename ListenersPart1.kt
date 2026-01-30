package com.eventplugin.listeners

import com.eventplugin.EventPlugin
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent

// ==================== PLAYER DEATH LISTENER (WITH AUTO-REKIT) ====================
class PlayerDeathListener(private val plugin: EventPlugin) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val victim = event.entity
        val killer = victim.killer
        val victimData = plugin.statsManager.getPlayerData(victim.uniqueId)

        if (!plugin.eventManager.isEventActive()) return

        // Update victim stats
        victimData.isAlive = false
        victimData.totalDeaths++
        victimData.eventDeaths++ // Track deaths in current event for auto-rekit

        val stolenPoints = victimData.points
        victimData.points = 0
        victimData.killStreak = 0

        // Update killer stats
        if (killer != null && killer is Player) {
            val killerData = plugin.statsManager.getPlayerData(killer.uniqueId)

            killerData.kills++
            killerData.totalKills++
            killerData.killStreak++
            killerData.points += stolenPoints + 1

            // Update best killstreak
            if (killerData.killStreak > killerData.bestKillStreak) {
                killerData.bestKillStreak = killerData.killStreak
            }

            // Use fancy formatter for death message
            event.deathMessage = plugin.chatFormatter.formatDeathMessage(victim, killer, killer)

            // Announce kill with fancy effects
            plugin.chatFormatter.announceKill(killer, victim, killerData.killStreak, stolenPoints + 1)

            // Play sound
            killer.playSound(killer.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        } else {
            // Natural death
            event.deathMessage = plugin.chatFormatter.formatDeathMessage(victim, null, null)
        }

        // Set victim to spectator
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            plugin.spectatorManager.setSpectator(victim, true)

            // Give spectator compass
            if (plugin.config.getBoolean("spectator.give-compass", true)) {
                victim.inventory.setItem(0, org.bukkit.inventory.ItemStack(Material.COMPASS))
            }
        }, 1L)

        // Check win condition
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            plugin.eventManager.checkWinCondition()
        }, 20L)

        // Keep inventory empty
        event.drops.clear()
        event.droppedExp = 0
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val data = plugin.statsManager.getPlayerData(player.uniqueId)

        if (!plugin.eventManager.isEventActive()) return

        // Check for auto-rekit
        if (plugin.config.getBoolean("kits.auto-rekit-enabled", false)) {
            val requiredDeaths = plugin.config.getInt("kits.auto-rekit-deaths", 3)

            if (data.eventDeaths >= requiredDeaths && data.eventDeaths % requiredDeaths == 0) {
                // Give kit from slot 1 if available
                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    if (data.selectedKits.isNotEmpty() && data.selectedKits[0].isNotEmpty()) {
                        val kitName = data.selectedKits[0]
                        if (plugin.kitManager.giveKitForce(player, kitName)) {
                            player.sendMessage("§a§l✔ AUTO-REKIT! §7Kit '§f$kitName§7' given!")
                            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f)
                        }
                    }
                }, 10L) // Delay to ensure player is fully respawned
            }
        }
    }
}

// ==================== PLAYER JOIN LISTENER ====================
class PlayerJoinListener(private val plugin: EventPlugin) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // Initialize player data if not exists
        val data = plugin.statsManager.getPlayerData(player.uniqueId)

        // Handle vanished players
        Bukkit.getOnlinePlayers().forEach { p ->
            if (plugin.adminManager.isVanished(p.uniqueId) && !player.hasPermission("event.staff")) {
                player.hidePlayer(plugin, p)
            }
        }

        // Check if should join as spectator
        val joinAsSpectator = plugin.config.getBoolean("spectator.join-as-spectator", false)

        // If event is active and player joins mid-event OR join-as-spectator is enabled
        if (plugin.eventManager.isEventActive() || joinAsSpectator) {
            data.isAlive = false
            data.isSpectator = true
            plugin.spectatorManager.setSpectator(player, true)

            if (plugin.eventManager.isEventActive()) {
                player.sendMessage(plugin.config.getString("messages.prefix") + "§7Event is currently active. You are now spectating.")
            } else if (joinAsSpectator) {
                player.sendMessage(plugin.config.getString("messages.prefix") + "§7You have joined as a spectator.")
            }
        }

        // Custom join message
        if (plugin.config.getBoolean("chat.custom-join-message", false)) {
            event.joinMessage = null
        }
    }
}

// ==================== PLAYER QUIT LISTENER ====================
class PlayerQuitListener(private val plugin: EventPlugin) : Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val data = plugin.statsManager.getPlayerData(player.uniqueId)

        // Handle combat logging
        if (plugin.combatManager.handleLogout(player)) {
            Bukkit.broadcastMessage(plugin.config.getString("messages.prefix") +
                    plugin.config.getString("messages.combat-logout")?.replace("{player}", player.name))
        }

        // Leave party
        plugin.partyManager.leaveParty(player.uniqueId)

        // Check win condition if player was alive
        if (data.isAlive && plugin.eventManager.isEventActive()) {
            data.isAlive = false
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                plugin.eventManager.checkWinCondition()
            }, 5L)
        }

        // Custom quit message
        if (plugin.config.getBoolean("chat.custom-quit-message", false)) {
            event.quitMessage = null
        }

        // Save player data
        plugin.statsManager.saveAll()
    }
}

// Extension function for sendActionBar
fun Player.sendActionBar(message: String) {
    this.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(message))
}