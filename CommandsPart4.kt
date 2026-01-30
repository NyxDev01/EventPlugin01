package com.eventplugin.commands

import com.eventplugin.EventPlugin
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

// ==================== JOIN SPECTATOR COMMAND ====================
class JoinSpectatorCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage("§c§l✖ §cNo permission!")
            return true
        }

        if (args.isEmpty()) {
            val current = plugin.config.getBoolean("spectator.join-as-spectator", false)
            sender.sendMessage("§7Join as spectator: ${if (current) "§a✔ Enabled" else "§c✖ Disabled"}")
            sender.sendMessage("§eUsage: /joinspectator <true|false>")
            return true
        }

        val enabled = when (args[0].lowercase()) {
            "true", "on", "enable", "yes" -> true
            "false", "off", "disable", "no" -> false
            else -> {
                sender.sendMessage("§c✖ Invalid argument! Use true or false")
                return true
            }
        }

        plugin.config.set("spectator.join-as-spectator", enabled)
        plugin.saveConfig()

        val status = if (enabled) "§a§lENABLED" else "§c§lDISABLED"
        Bukkit.broadcastMessage("§6§l[Event] §r§7Join as spectator has been $status")

        return true
    }
}

// ==================== PROTECTION COMMAND ====================
class ProtectionCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage("§c§l✖ §cNo permission!")
            return true
        }

        if (args.isEmpty()) {
            val current = plugin.config.getBoolean("world.spawn-protection-enabled", true)
            sender.sendMessage("§7Spawn protection: ${if (current) "§a✔ Enabled" else "§c✖ Disabled"}")
            sender.sendMessage("§eUsage: /protection <true|false>")
            return true
        }

        val enabled = when (args[0].lowercase()) {
            "true", "on", "enable", "yes" -> true
            "false", "off", "disable", "no" -> false
            else -> {
                sender.sendMessage("§c✖ Invalid argument! Use true or false")
                return true
            }
        }

        plugin.config.set("world.spawn-protection-enabled", enabled)
        plugin.saveConfig()

        val status = if (enabled) "§a§lENABLED" else "§c§lDISABLED"
        Bukkit.broadcastMessage("§6§l[Event] §r§7Spawn protection has been $status")

        return true
    }
}

// ==================== DROP COMMAND ====================
class DropCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use this command!")
            return true
        }

        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage("§c§l✖ §cNo permission!")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /drop <bedrock|deepslate>")
            return true
        }

        val material = when (args[0].lowercase()) {
            "bedrock" -> Material.BEDROCK
            "deepslate" -> Material.DEEPSLATE
            else -> {
                sender.sendMessage("§c✖ Invalid material! Use 'bedrock' or 'deepslate'")
                return true
            }
        }

        val itemStack = org.bukkit.inventory.ItemStack(material, 1)
        val item = sender.world.dropItemNaturally(sender.location, itemStack)

        sender.sendMessage("§a✔ Dropped ${args[0]}!")

        return true
    }
}

// ==================== REKIT COMMAND ====================
class RekitCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage("§c§l✖ §cNo permission!")
            return true
        }

        if (args.isEmpty()) {
            val current = plugin.config.getBoolean("kits.auto-rekit-enabled", false)
            sender.sendMessage("§7Auto-rekit: ${if (current) "§a✔ Enabled" else "§c✖ Disabled"}")
            sender.sendMessage("§eUsage: /rekit <true|false>")
            return true
        }

        val enabled = when (args[0].lowercase()) {
            "true", "on", "enable", "yes" -> true
            "false", "off", "disable", "no" -> false
            else -> {
                sender.sendMessage("§c✖ Invalid argument! Use true or false")
                return true
            }
        }

        plugin.config.set("kits.auto-rekit-enabled", enabled)
        plugin.saveConfig()

        val status = if (enabled) "§a§lENABLED" else "§c§lDISABLED"
        Bukkit.broadcastMessage("§6§l[Event] §r§7Auto-rekit has been $status")

        return true
    }
}

// ==================== REKIT SET COMMAND ====================
class RekitSetCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage("§c§l✖ §cNo permission!")
            return true
        }

        if (args.isEmpty()) {
            val current = plugin.config.getInt("kits.auto-rekit-deaths", 3)
            sender.sendMessage("§7Auto-rekit after: §f$current deaths")
            sender.sendMessage("§eUsage: /rekitset <deaths>")
            return true
        }

        val deaths = args[0].toIntOrNull()
        if (deaths == null || deaths < 1) {
            sender.sendMessage("§c✖ Invalid number! Must be at least 1")
            return true
        }

        plugin.config.set("kits.auto-rekit-deaths", deaths)
        plugin.saveConfig()

        Bukkit.broadcastMessage("§6§l[Event] §r§7Auto-rekit set to §f$deaths §7deaths")

        return true
    }
}

// ==================== KITALL COMMAND ====================
class KitAllCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage("§c§l✖ §cNo permission!")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /kitall <k1|k2|k3>")
            return true
        }

        val slot = when (args[0].lowercase()) {
            "k1" -> 0
            "k2" -> 1
            "k3" -> 2
            else -> {
                sender.sendMessage("§c✖ Invalid slot! Use k1, k2, or k3")
                return true
            }
        }

        var successCount = 0
        var failCount = 0

        Bukkit.getOnlinePlayers().forEach { player ->
            val data = plugin.statsManager.getPlayerData(player.uniqueId)

            if (data.selectedKits.size <= slot || data.selectedKits[slot].isEmpty()) {
                failCount++
                return@forEach
            }

            val kitName = data.selectedKits[slot]

            if (plugin.kitManager.giveKit(player, kitName)) {
                successCount++
                player.sendMessage("§a✔ Kit '§f$kitName§a' given!")
            } else {
                failCount++
            }
        }

        sender.sendMessage("§6§l[Event] §r§7Gave kit ${args[0]} to §a$successCount §7players (§c$failCount §7failed)")

        return true
    }
}

// ==================== DOKIT COMMAND ====================
class DoKitCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage("§c§l✖ §cNo permission!")
            return true
        }

        if (args.size < 2) {
            sender.sendMessage("§cUsage: /dokit <k1|k2|k3> <player>")
            return true
        }

        val slot = when (args[0].lowercase()) {
            "k1" -> 0
            "k2" -> 1
            "k3" -> 2
            else -> {
                sender.sendMessage("§c✖ Invalid slot! Use k1, k2, or k3")
                return true
            }
        }

        val target = Bukkit.getPlayer(args[1])
        if (target == null) {
            sender.sendMessage("§c✖ Player '${args[1]}' not found!")
            return true
        }

        val data = plugin.statsManager.getPlayerData(target.uniqueId)

        if (data.selectedKits.size <= slot || data.selectedKits[slot].isEmpty()) {
            sender.sendMessage("§c✖ ${target.name} has no kit in slot ${args[0]}!")
            return true
        }

        val kitName = data.selectedKits[slot]

        if (plugin.kitManager.giveKit(target, kitName)) {
            sender.sendMessage("§a✔ Gave kit '§f$kitName§a' to §f${target.name}")
            target.sendMessage("§a✔ Kit '§f$kitName§a' given by admin!")
        } else {
            sender.sendMessage("§c✖ Failed to give kit!")
        }

        return true
    }
}

// ==================== TITLES COMMAND ====================
class TitlesCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use this command!")
            return true
        }

        if (!sender.hasPermission("event.titles")) {
            sender.sendMessage("§c§l✖ §cNo permission!")
            return true
        }

        // Open titles GUI
        com.eventplugin.gui.TitlesGUI(plugin).open(sender)

        return true
    }
}