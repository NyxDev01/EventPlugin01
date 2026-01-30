package com.eventplugin.commands

import com.eventplugin.EventPlugin
import com.eventplugin.gui.EventGUI
import com.eventplugin.managers.EventManager
import com.eventplugin.managers.PartyManager
import com.eventplugin.data.PartyData
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

// ==================== EVENT COMMAND ====================
class EventCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use this command!")
            return true
        }

        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /event <gui|start|stop|pause|kill|freeze|ban|whitelist>")
            return true
        }

        when (args[0].lowercase()) {
            "gui" -> {
                EventGUI(plugin).open(sender)
            }
            "start" -> {
                if (plugin.eventManager.startEvent()) {
                    sender.sendMessage("§aEvent starting!")
                } else {
                    sender.sendMessage("§cEvent is already active or not enough players!")
                }
            }
            "forcestart" -> {
                if (plugin.eventManager.startEvent(force = true)) {
                    sender.sendMessage("§aEvent force started!")
                } else {
                    sender.sendMessage("§cEvent is already active!")
                }
            }
            "stop" -> {
                plugin.eventManager.stopEvent()
                sender.sendMessage("§cEvent stopped!")
            }
            "pause" -> {
                plugin.eventManager.pauseEvent()
                sender.sendMessage("§eEvent paused!")
            }
            "resume" -> {
                plugin.eventManager.resumeEvent()
                sender.sendMessage("§aEvent resumed!")
            }
            "kill" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /event kill <player>")
                    return true
                }
                val target = Bukkit.getPlayer(args[1])
                if (target == null) {
                    sender.sendMessage("§cPlayer not found!")
                    return true
                }
                target.health = 0.0
                sender.sendMessage("§aKilled ${target.name}!")
            }
            "freeze" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /event freeze <player>")
                    return true
                }
                val target = Bukkit.getPlayer(args[1])
                if (target == null) {
                    sender.sendMessage("§cPlayer not found!")
                    return true
                }
                val isFrozen = !plugin.adminManager.isFrozen(target.uniqueId)
                plugin.adminManager.freezePlayer(target.uniqueId, isFrozen)
                sender.sendMessage("§a${target.name} ${if (isFrozen) "frozen" else "unfrozen"}!")
            }
            "ban" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /event ban <player>")
                    return true
                }
                val target = Bukkit.getOfflinePlayer(args[1])
                plugin.adminManager.banFromEvent(target.uniqueId, true)
                sender.sendMessage("§a${target.name} banned from events!")
            }
            "unban" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /event unban <player>")
                    return true
                }
                val target = Bukkit.getOfflinePlayer(args[1])
                plugin.adminManager.banFromEvent(target.uniqueId, false)
                sender.sendMessage("§a${target.name} unbanned from events!")
            }
            "whitelist" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /event whitelist <add|remove|toggle> [player]")
                    return true
                }
                when (args[1].lowercase()) {
                    "add" -> {
                        if (args.size < 3) {
                            sender.sendMessage("§cUsage: /event whitelist add <player>")
                            return true
                        }
                        val target = Bukkit.getOfflinePlayer(args[2])
                        plugin.adminManager.addToWhitelist(target.uniqueId)
                        sender.sendMessage("§a${target.name} added to whitelist!")
                    }
                    "remove" -> {
                        if (args.size < 3) {
                            sender.sendMessage("§cUsage: /event whitelist remove <player>")
                            return true
                        }
                        val target = Bukkit.getOfflinePlayer(args[2])
                        plugin.adminManager.removeFromWhitelist(target.uniqueId)
                        sender.sendMessage("§a${target.name} removed from whitelist!")
                    }
                    "toggle" -> {
                        val enabled = !plugin.adminManager.isWhitelistMode()
                        plugin.adminManager.setWhitelistMode(enabled)
                        sender.sendMessage("§aWhitelist mode ${if (enabled) "enabled" else "disabled"}!")
                    }
                }
            }
            "reload" -> {
                plugin.reload()
                sender.sendMessage("§aPlugin reloaded!")
            }
            else -> {
                sender.sendMessage("§cUnknown subcommand!")
            }
        }

        return true
    }
}

// ==================== PARTY COMMAND ====================
class PartyCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use this command!")
            return true
        }

        if (!sender.hasPermission("event.party.use")) {
            sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /party <create|invite|accept|deny|leave|kick|disband|list>")
            return true
        }

        when (args[0].lowercase()) {
            "create" -> {
                val party = plugin.partyManager.createParty(sender.uniqueId)
                if (party != null) {
                    sender.sendMessage("§aParty created!")
                } else {
                    sender.sendMessage("§cYou are already in a party!")
                }
            }
            "invite" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /party invite <player>")
                    return true
                }
                val target = Bukkit.getPlayer(args[1])
                if (target == null) {
                    sender.sendMessage("§cPlayer not found!")
                    return true
                }

                val party = plugin.partyManager.getParty(sender.uniqueId)
                if (party == null) {
                    sender.sendMessage("§cYou are not in a party!")
                    return true
                }

                if (!party.isLeader(sender.uniqueId)) {
                    sender.sendMessage("§cOnly the party leader can invite players!")
                    return true
                }

                if (plugin.partyManager.invitePlayer(party.partyId, target.uniqueId)) {
                    sender.sendMessage("§aInvited ${target.name} to the party!")
                    target.sendMessage("§a${sender.name} invited you to their party! /party accept")
                } else {
                    sender.sendMessage("§cCouldn't invite player! (Full or already in party)")
                }
            }
            "accept" -> {
                // Find a party that has invited this player
                var foundParty: PartyData? = null
                plugin.partyManager.getAllParties().forEach { party ->
                    if (party.hasInvite(sender.uniqueId)) {
                        foundParty = party
                    }
                }

                if (foundParty == null) {
                    sender.sendMessage("§cYou don't have any party invites!")
                    return true
                }

                if (plugin.partyManager.acceptInvite(sender.uniqueId, foundParty!!.partyId)) {
                    sender.sendMessage("§aYou joined the party!")

                    // Notify party members
                    foundParty!!.members.forEach { uuid ->
                        Bukkit.getPlayer(uuid)?.sendMessage("§a${sender.name} joined the party!")
                    }
                } else {
                    sender.sendMessage("§cCouldn't join party!")
                }
            }
            "deny" -> {
                // Clear all invites for this player
                plugin.partyManager.getAllParties().forEach { party ->
                    party.invites.remove(sender.uniqueId)
                }
                sender.sendMessage("§cDenied all party invites!")
            }
            "leave" -> {
                if (plugin.partyManager.leaveParty(sender.uniqueId)) {
                    sender.sendMessage("§cYou left the party!")
                } else {
                    sender.sendMessage("§cYou are not in a party!")
                }
            }
            "kick" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /party kick <player>")
                    return true
                }
                val target = Bukkit.getPlayer(args[1])
                if (target == null) {
                    sender.sendMessage("§cPlayer not found!")
                    return true
                }

                if (plugin.partyManager.kickMember(sender.uniqueId, target.uniqueId)) {
                    sender.sendMessage("§aKicked ${target.name} from the party!")
                    target.sendMessage("§cYou were kicked from the party!")
                } else {
                    sender.sendMessage("§cCouldn't kick player!")
                }
            }
            "disband" -> {
                val party = plugin.partyManager.getParty(sender.uniqueId)
                if (party == null) {
                    sender.sendMessage("§cYou are not in a party!")
                    return true
                }

                if (!party.isLeader(sender.uniqueId)) {
                    sender.sendMessage("§cOnly the party leader can disband the party!")
                    return true
                }

                // Notify all members
                party.members.forEach { uuid ->
                    Bukkit.getPlayer(uuid)?.sendMessage("§cThe party has been disbanded!")
                }

                plugin.partyManager.disbandParty(party.partyId)
                sender.sendMessage("§cParty disbanded!")
            }
            "list" -> {
                val party = plugin.partyManager.getParty(sender.uniqueId)
                if (party == null) {
                    sender.sendMessage("§cYou are not in a party!")
                    return true
                }

                sender.sendMessage("§6§lParty Members:")
                party.members.forEach { uuid ->
                    val member = Bukkit.getOfflinePlayer(uuid)
                    val status = if (party.isLeader(uuid)) "§e[Leader]" else ""
                    sender.sendMessage("§7- §f${member.name} $status")
                }
            }
            "setsize" -> {
                if (!sender.hasPermission("event.admin")) {
                    sender.sendMessage("§cYou don't have permission!")
                    return true
                }
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /party setsize <size>")
                    return true
                }
                val size = args[1].toIntOrNull()
                if (size == null || size < 1) {
                    sender.sendMessage("§cInvalid size!")
                    return true
                }
                plugin.partyManager.setMaxPartySize(size)
                sender.sendMessage("§aMax party size set to $size!")
            }
        }

        return true
    }
}

// ==================== PARTY CHAT COMMAND ====================
class PartyChatCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use this command!")
            return true
        }

        val party = plugin.partyManager.getParty(sender.uniqueId)
        if (party == null) {
            sender.sendMessage("§cYou are not in a party!")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /pc <message>")
            return true
        }

        val message = args.joinToString(" ")
        val prefix = plugin.config.getString("chat.party-prefix") ?: "§9[PARTY] "

        party.members.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.sendMessage("$prefix§f${sender.name}: $message")
        }

        return true
    }
}

// ==================== BORDER COMMAND ====================
class BorderCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /border <shrink1-11> <seconds>")
            return true
        }

        val phase = when {
            args[0].startsWith("shrink") -> args[0].substring(6).toIntOrNull()
            else -> null
        }

        if (phase == null || phase !in 1..11) {
            sender.sendMessage("§cInvalid phase! Use shrink1 through shrink11")
            return true
        }

        val seconds = args.getOrNull(1)?.toIntOrNull() ?: plugin.config.getInt("border.phases.$phase.default-time", 60)

        plugin.borderManager.shrinkToPhase(phase, seconds)
        sender.sendMessage("§aBorder shrinking to phase $phase!")

        return true
    }
}

// ==================== SURGE COMMAND ====================
class SurgeCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /surge <set|toggle|info>")
            return true
        }

        when (args[0].lowercase()) {
            "set" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /surge set <amount>")
                    return true
                }
                val amount = args[1].toIntOrNull()
                if (amount == null || amount < 0) {
                    sender.sendMessage("§cInvalid amount!")
                    return true
                }
                plugin.surgeManager.setRequiredPops(amount)
                sender.sendMessage("§aRequired pops set to $amount!")
            }
            "toggle" -> {
                val enabled = plugin.surgeManager.toggle()
                sender.sendMessage("§aSurge ${if (enabled) "enabled" else "disabled"}!")
            }
            "info" -> {
                sender.sendMessage("§6§lSurge Info:")
                sender.sendMessage("§7Enabled: §f${plugin.surgeManager.isEnabled()}")
                sender.sendMessage("§7Required Pops: §f${plugin.surgeManager.getRequiredPops()}")
            }
        }

        return true
    }
}

// ==================== REVIVE COMMANDS ====================
class ReviveAllCommand(private val plugin: EventPlugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
            return true
        }

        var revived = 0
        Bukkit.getOnlinePlayers().forEach { player ->
            val data = plugin.statsManager.getPlayerData(player.uniqueId)
            if (!data.isAlive) {
                data.isAlive = true
                data.isSpectator = false
                plugin.spectatorManager.setSpectator(player, false)
                player.health = 20.0
                revived++
            }
        }

        sender.sendMessage("§aRevived $revived players!")
        return true
    }
}

class ReviveCommand(private val plugin: EventPlugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /rev <player>")
            return true
        }

        val target = Bukkit.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage("§cPlayer not found!")
            return true
        }

        val data = plugin.statsManager.getPlayerData(target.uniqueId)
        data.isAlive = true
        data.isSpectator = false
        plugin.spectatorManager.setSpectator(target, false)
        target.health = 20.0

        sender.sendMessage("§aRevived ${target.name}!")
        return true
    }
}

// ==================== PREREVIVE COMMAND (FIXED WITH POINTS COST!) ====================
class PreReviveCommand(private val plugin: EventPlugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use this command!")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /prerevive <player>")
            return true
        }

        val target = Bukkit.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage("§cPlayer not found!")
            return true
        }

        val targetData = plugin.statsManager.getPlayerData(target.uniqueId)

        // Check if target is actually a spectator
        if (!targetData.isSpectator) {
            sender.sendMessage("§c${target.name} is not a spectator!")
            return true
        }

        // Get cost from config (default 5 points)
        val cost = plugin.config.getInt("prerevive.cost", 5)
        val senderData = plugin.statsManager.getPlayerData(sender.uniqueId)

        // Check if sender has enough points
        if (senderData.points < cost) {
            sender.sendMessage("§cYou need §6$cost points §cto revive a player! You have §6${senderData.points} points§c.")
            return true
        }

        // Deduct points from sender
        senderData.points -= cost

        // Revive target at sender's location
        targetData.isAlive = true
        targetData.isSpectator = false
        plugin.spectatorManager.setSpectator(target, false)
        target.teleport(sender.location)
        target.health = 20.0

        sender.sendMessage("§aRevived ${target.name} at your location! §7(-$cost points)")
        target.sendMessage("§aYou were revived by ${sender.name}!")

        // Broadcast to all players
        Bukkit.broadcastMessage(plugin.config.getString("messages.prefix") + "§e${sender.name} §7revived §e${target.name} §7for §6$cost points§7!")

        return true
    }
}