package com.eventplugin.commands

import com.eventplugin.EventPlugin
import com.eventplugin.gui.StatsGUI
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

// ==================== BROADCAST COMMAND ====================
class BroadcastCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
            return true
        }

        if (args.size < 2) {
            sender.sendMessage("§cUsage: /broadcast <chat|screen> <message>")
            return true
        }

        val type = args[0].lowercase()
        val message = args.drop(1).joinToString(" ").replace("&", "§")

        when (type) {
            "chat" -> {
                Bukkit.broadcastMessage(message)
                sender.sendMessage("§aBroadcast sent!")
            }

            "screen" -> {
                Bukkit.getOnlinePlayers().forEach { player ->
                    player.sendTitle(message, "", 10, 70, 20)
                }
                sender.sendMessage("§aTitle broadcast sent!")
            }

            else -> {
                sender.sendMessage("§cInvalid type! Use 'chat' or 'screen'")
            }
        }

        return true
    }
}

// ==================== STAFF CHAT COMMAND ====================
class StaffChatCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("event.staff")) {
            sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /sc <message>")
            return true
        }

        val message = args.joinToString(" ")
        val senderName = if (sender is Player) sender.name else "Console"

        val formattedMessage = "§c§l[STAFF] §r§7$senderName: §f$message"

        // Send to all staff members
        Bukkit.getOnlinePlayers()
            .filter { it.hasPermission("event.staff") }
            .forEach { it.sendMessage(formattedMessage) }

        // Also send to console
        Bukkit.getConsoleSender().sendMessage(formattedMessage)

        return true
    }
}

// ==================== STATS COMMAND ====================
class StatsCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            // Console can view other players' stats
            if (args.isEmpty()) {
                sender.sendMessage("§cUsage: /stats <player>")
                return true
            }

            val target = Bukkit.getOfflinePlayer(args[0])
            val data = plugin.statsManager.getPlayerData(target.uniqueId)

            sender.sendMessage("§6§l=== Stats for ${target.name} ===")
            sender.sendMessage("§7Total Wins: §f${data.totalWins}")
            sender.sendMessage("§7Total Kills: §f${data.totalKills}")
            sender.sendMessage("§7Total Deaths: §f${data.totalDeaths}")
            sender.sendMessage("§7K/D Ratio: §f${String.format("%.2f", data.getKDRatio())}")
            sender.sendMessage("§7Win Rate: §f${String.format("%.1f", data.getWinRate())}%")
            sender.sendMessage("§7Best Killstreak: §f${data.bestKillStreak}")
            sender.sendMessage("§7Total Points: §f${data.totalPoints}")

            return true
        }

        if (!sender.hasPermission("event.use")) {
            sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
            return true
        }

        if (args.isNotEmpty()) {
            if (!sender.hasPermission("event.stats.others")) {
                sender.sendMessage("§cYou can only view your own stats!")
                return true
            }

            val target = Bukkit.getOfflinePlayer(args[0])
            val data = plugin.statsManager.getPlayerData(target.uniqueId)

            sender.sendMessage("§6§l=== Stats for ${target.name} ===")
            sender.sendMessage("§7Total Wins: §f${data.totalWins}")
            sender.sendMessage("§7Total Kills: §f${data.totalKills}")
            sender.sendMessage("§7Total Deaths: §f${data.totalDeaths}")
            sender.sendMessage("§7K/D Ratio: §f${String.format("%.2f", data.getKDRatio())}")
            sender.sendMessage("§7Win Rate: §f${String.format("%.1f", data.getWinRate())}%")
            sender.sendMessage("§7Best Killstreak: §f${data.bestKillStreak}")
            sender.sendMessage("§7Total Points: §f${data.totalPoints}")

            return true
        }


        StatsGUI(plugin).open(sender)

        return true
    }
}