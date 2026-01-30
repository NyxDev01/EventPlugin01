package com.eventplugin.listeners

import com.eventplugin.EventPlugin
import com.eventplugin.chat.ChatFormatter
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class ChatListener(private val plugin: EventPlugin) : Listener {

    private val formatter = ChatFormatter(plugin)

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val message = event.message
        val data = plugin.statsManager.getPlayerData(player.uniqueId)

        // Always cancel to use custom formatting
        event.isCancelled = true

        // Check for party chat prefix
        if (message.startsWith("@") || message.startsWith("!")) {
            val party = plugin.partyManager.getParty(player.uniqueId)
            if (party == null) {
                player.sendMessage("§c§l✖ §cYou are not in a party!")
                return
            }

            val actualMessage = message.substring(1).trim()
            if (actualMessage.isEmpty()) {
                player.sendMessage("§c§l✖ §cPlease provide a message!")
                return
            }

            // Format and send to party members
            val formattedMessage = formatter.formatPartyChat(player, actualMessage, party)
            party.members.forEach { uuid ->
                val member = org.bukkit.Bukkit.getPlayer(uuid)
                member?.sendMessage(formattedMessage)
            }
            return
        }

        // Check for staff chat prefix
        if (message.startsWith("#") && player.hasPermission("event.staff")) {
            val actualMessage = message.substring(1).trim()
            if (actualMessage.isEmpty()) {
                player.sendMessage("§c§l✖ §cPlease provide a message!")
                return
            }

            val formattedMessage = formatter.formatStaffChat(player, actualMessage)

            // Send to all staff members
            org.bukkit.Bukkit.getOnlinePlayers()
                .filter { it.hasPermission("event.staff") }
                .forEach { it.sendMessage(formattedMessage) }

            // Also send to console
            org.bukkit.Bukkit.getConsoleSender().sendMessage(formattedMessage)
            return
        }

        // Spectator chat - only other spectators and staff can see
        if (data.isSpectator && plugin.config.getBoolean("spectator.separate-chat", true)) {
            val spectatorMessage = formatter.formatSpectatorChat(player, message)

            org.bukkit.Bukkit.getOnlinePlayers().forEach { p ->
                val pData = plugin.statsManager.getPlayerData(p.uniqueId)
                if (pData.isSpectator || p.hasPermission("event.staff")) {
                    p.sendMessage(spectatorMessage)
                }
            }
            return
        }

        // Regular chat with fancy formatting
        val formattedMessage = formatter.formatChat(player, message)

        // Send to all non-spectator players (unless they're staff)
        org.bukkit.Bukkit.getOnlinePlayers()
            .filter { p ->
                val pData = plugin.statsManager.getPlayerData(p.uniqueId)
                !pData.isSpectator || p.hasPermission("event.staff")
            }
            .forEach { it.sendMessage(formattedMessage) }
    }
}