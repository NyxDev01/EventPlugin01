package com.eventplugin.gui

import com.eventplugin.EventPlugin
import com.eventplugin.utils.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory

// ==================== STATS GUI ====================
class StatsGUI(private val plugin: EventPlugin) {

    fun open(player: Player) {
        val inventory = createInventory(player)
        player.openInventory(inventory)
    }

    private fun createInventory(player: Player): Inventory {
        val inventory = Bukkit.createInventory(null, 27, "§6§lYour Statistics")
        val data = plugin.statsManager.getPlayerData(player.uniqueId)

        // Total Wins
        inventory.setItem(10, ItemBuilder.from(Material.GOLD_INGOT)
            .name("§6§lTotal Wins")
            .lore(
                "§7Wins: §f${data.totalWins}",
                "§7Rank: §f#${plugin.statsManager.getPlayerRankByWins(player.uniqueId)}",
                "",
                "§7Win Rate: §f${String.format("%.1f", data.getWinRate())}%"
            )
            .glow(true)
            .build())

        // Total Kills
        inventory.setItem(11, ItemBuilder.from(Material.DIAMOND_SWORD)
            .name("§c§lTotal Kills")
            .lore(
                "§7Kills: §f${data.totalKills}",
                "§7Rank: §f#${plugin.statsManager.getPlayerRankByKills(player.uniqueId)}",
                "",
                "§7Current Event: §f${data.kills}"
            )
            .build())

        // Total Deaths
        inventory.setItem(12, ItemBuilder.from(Material.SKELETON_SKULL)
            .name("§8§lTotal Deaths")
            .lore(
                "§7Deaths: §f${data.totalDeaths}",
                "",
                "§7Games Played: §f${data.totalWins + data.totalDeaths}"
            )
            .build())

        // K/D Ratio
        inventory.setItem(13, ItemBuilder.from(Material.IRON_SWORD)
            .name("§e§lK/D Ratio")
            .lore(
                "§7K/D: §f${String.format("%.2f", data.getKDRatio())}",
                "",
                "§7Kills: §f${data.totalKills}",
                "§7Deaths: §f${data.totalDeaths}"
            )
            .build())

        // Best Killstreak
        inventory.setItem(14, ItemBuilder.from(Material.BLAZE_ROD)
            .name("§6§lBest Killstreak")
            .lore(
                "§7Best: §f${data.bestKillStreak}",
                "",
                "§7Current: §f${data.killStreak}"
            )
            .build())

        // Total Points
        inventory.setItem(15, ItemBuilder.from(Material.EMERALD)
            .name("§a§lTotal Points")
            .lore(
                "§7Total: §f${data.totalPoints}",
                "§7Rank: §f#${plugin.statsManager.getPlayerRankByPoints(player.uniqueId)}",
                "",
                "§7Current Event: §f${data.points}"
            )
            .build())

        // Current Event Stats
        inventory.setItem(16, ItemBuilder.from(Material.NETHER_STAR)
            .name("§d§lCurrent Event")
            .lore(
                "§7Kills: §f${data.kills}",
                "§7Points: §f${data.points}",
                "§7Killstreak: §f${data.killStreak}",
                "",
                "§7Status: ${if (data.isAlive) "§aAlive" else "§cDead"}",
                "§7Spectator: ${if (data.isSpectator) "§aYes" else "§cNo"}"
            )
            .build())

        // Leaderboard Position
        inventory.setItem(19, ItemBuilder.from(Material.GOLDEN_HELMET)
            .name("§6§lLeaderboard Rankings")
            .lore(
                "§7Wins: §f#${plugin.statsManager.getPlayerRankByWins(player.uniqueId)}",
                "§7Kills: §f#${plugin.statsManager.getPlayerRankByKills(player.uniqueId)}",
                "§7Points: §f#${plugin.statsManager.getPlayerRankByPoints(player.uniqueId)}"
            )
            .build())

        // Close
        inventory.setItem(22, ItemBuilder.from(Material.BARRIER)
            .name("§c§lClose")
            .build())

        // Fill empty slots
        for (i in 0 until inventory.size) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                    .name(" ")
                    .build())
            }
        }

        return inventory
    }

    fun handleClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val slot = event.slot

        when (slot) {
            22 -> player.closeInventory()
        }
    }
}

// ==================== PLAYER LIST GUI ====================
class PlayerListGUI(private val plugin: EventPlugin) {

    fun open(player: Player) {
        val inventory = createInventory(player)
        player.openInventory(inventory)
    }

    private fun createInventory(viewer: Player): Inventory {
        val inventory = Bukkit.createInventory(null, 54, "§e§lPlayer List")

        val alivePlayers = mutableListOf<Player>()
        val spectators = mutableListOf<Player>()

        Bukkit.getOnlinePlayers().forEach { p ->
            val data = plugin.statsManager.getPlayerData(p.uniqueId)
            if (data.isAlive && !data.isSpectator) {
                alivePlayers.add(p)
            } else if (data.isSpectator) {
                spectators.add(p)
            }
        }

        var slot = 0

        // Alive players
        inventory.setItem(slot++, ItemBuilder.from(Material.LIME_STAINED_GLASS_PANE)
            .name("§a§lALIVE PLAYERS (${alivePlayers.size})")
            .build())
        slot++ // Skip a slot

        alivePlayers.forEach { p ->
            if (slot >= 27) return@forEach

            val data = plugin.statsManager.getPlayerData(p.uniqueId)
            val party = plugin.partyManager.getParty(p.uniqueId)

            inventory.setItem(slot++, ItemBuilder.from(Material.PLAYER_HEAD)
                .name("§f${p.name}")
                .lore(
                    "§7Health: §c${String.format("%.1f", p.health)}§7/§c${p.maxHealth}",
                    "§7Kills: §f${data.kills}",
                    "§7Points: §f${data.points}",
                    "§7Killstreak: §f${data.killStreak}",
                    if (party != null) "${party.color}Party: ${party.members.size} members" else "§7No Party",
                    "",
                    if (viewer.hasPermission("event.admin")) {
                        "§eLeft-Click: §7Teleport to player"
                    } else {
                        "§eClick: §7Spectate player"
                    },
                    if (viewer.hasPermission("event.admin")) "§eRight-Click: §7Kill player" else ""
                )
                .build())
        }

        // Spectators section
        slot = 27
        inventory.setItem(slot++, ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
            .name("§7§lSPECTATORS (${spectators.size})")
            .build())
        slot++

        spectators.forEach { p ->
            if (slot >= 54) return@forEach

            val data = plugin.statsManager.getPlayerData(p.uniqueId)

            inventory.setItem(slot++, ItemBuilder.from(Material.PLAYER_HEAD)
                .name("§7${p.name}")
                .lore(
                    "§7Kills: §f${data.kills}",
                    "§7Points: §f${data.points}",
                    "",
                    if (viewer.hasPermission("event.admin")) {
                        "§eClick: §7Revive player"
                    } else {
                        "§7Spectating"
                    }
                )
                .build())
        }

        // Close button
        inventory.setItem(49, ItemBuilder.from(Material.BARRIER)
            .name("§c§lClose")
            .build())

        return inventory
    }

    fun handleClick(event: InventoryClickEvent) {
        val viewer = event.whoClicked as? Player ?: return
        val item = event.currentItem ?: return

        if (item.type == Material.BARRIER) {
            viewer.closeInventory()
            return
        }

        if (item.type != Material.PLAYER_HEAD) return

        val playerName = org.bukkit.ChatColor.stripColor(item.itemMeta?.displayName ?: "") ?: return
        val target = Bukkit.getPlayer(playerName) ?: return

        if (event.isLeftClick) {
            if (viewer.hasPermission("event.admin")) {
                viewer.teleport(target)
                viewer.sendMessage("§aTeleported to ${target.name}")
            } else {
                viewer.teleport(target)
                viewer.sendMessage("§7Now spectating ${target.name}")
            }
            viewer.closeInventory()
        } else if (event.isRightClick && viewer.hasPermission("event.admin")) {
            val data = plugin.statsManager.getPlayerData(target.uniqueId)
            if (data.isAlive) {
                target.health = 0.0
                viewer.sendMessage("§cKilled ${target.name}")
            } else {
                viewer.performCommand("rev ${target.name}")
            }
            viewer.closeInventory()
        }
    }
}

// ==================== SPECTATOR COMPASS GUI ====================
class SpectatorCompassGUI(private val plugin: EventPlugin) {

    fun open(player: Player) {
        val inventory = createInventory(player)
        player.openInventory(inventory)
    }

    private fun createInventory(spectator: Player): Inventory {
        val inventory = Bukkit.createInventory(null, 54, "§b§lSpectator Menu")

        val alivePlayers = Bukkit.getOnlinePlayers().filter { p ->
            val data = plugin.statsManager.getPlayerData(p.uniqueId)
            data.isAlive && !data.isSpectator
        }.sortedByDescending { p ->
            plugin.statsManager.getPlayerData(p.uniqueId).kills
        }

        var slot = 0
        alivePlayers.forEach { p ->
            if (slot >= 54) return@forEach

            val data = plugin.statsManager.getPlayerData(p.uniqueId)
            val party = plugin.partyManager.getParty(p.uniqueId)

            val healthBar = getHealthBar(p.health, p.maxHealth)

            inventory.setItem(slot++, ItemBuilder.from(Material.PLAYER_HEAD)
                .name("${if (party != null) party.color else "§f"}${p.name}")
                .lore(
                    healthBar,
                    "§7Kills: §c${data.kills} §7Points: §6${data.points}",
                    "§7Killstreak: §e${data.killStreak}",
                    if (party != null) "${party.color}Party (${party.members.size})" else "§7Solo",
                    "",
                    "§eClick to teleport"
                )
                .build())
        }

        if (alivePlayers.isEmpty()) {
            inventory.setItem(22, ItemBuilder.from(Material.BARRIER)
                .name("§c§lNo Alive Players")
                .lore("§7There are no players to spectate")
                .build())
        }

        // Random teleport
        inventory.setItem(49, ItemBuilder.from(Material.ENDER_PEARL)
            .name("§d§lRandom Player")
            .lore("§7Teleport to a random alive player")
            .build())

        // Close
        inventory.setItem(53, ItemBuilder.from(Material.BARRIER)
            .name("§c§lClose")
            .build())

        return inventory
    }

    private fun getHealthBar(health: Double, maxHealth: Double): String {
        val percentage = (health / maxHealth * 100).toInt()
        val bars = (health / maxHealth * 10).toInt()

        val color = when {
            percentage >= 75 -> "§a"
            percentage >= 50 -> "§e"
            percentage >= 25 -> "§6"
            else -> "§c"
        }

        val healthBar = buildString {
            append(color)
            repeat(bars) { append("█") }
            append("§7")
            repeat(10 - bars) { append("█") }
        }

        return "$healthBar §r$color${String.format("%.1f", health)}§7/§c${maxHealth.toInt()}"
    }

    fun handleClick(event: InventoryClickEvent) {
        val spectator = event.whoClicked as? Player ?: return
        val item = event.currentItem ?: return

        when (item.type) {
            Material.BARRIER -> {
                spectator.closeInventory()
            }
            Material.ENDER_PEARL -> {
                spectator.closeInventory()
                plugin.spectatorManager.teleportToRandomPlayer(spectator)
            }
            Material.PLAYER_HEAD -> {
                val playerName = org.bukkit.ChatColor.stripColor(item.itemMeta?.displayName ?: "") ?: return
                val target = Bukkit.getPlayer(playerName) ?: return

                spectator.teleport(target)
                spectator.sendMessage("§7Now spectating §e${target.name}")
                spectator.closeInventory()
            }
            else -> {}
        }
    }
}