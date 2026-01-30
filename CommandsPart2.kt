package com.eventplugin.commands

import com.eventplugin.EventPlugin
import com.eventplugin.gui.KitGUI
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

// ==================== KIT COMMAND ====================
class KitCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use this command!")
            return true
        }

        if (!sender.hasPermission("event.kit.use")) {
            sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
            return true
        }

        if (args.isEmpty()) {
            // Open kit GUI
            KitGUI(plugin).open(sender)
            return true
        }

        when (args[0].lowercase()) {
            "create" -> {
                if (!sender.hasPermission("event.kit.create")) {
                    sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
                    return true
                }

                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /kit create <name>")
                    return true
                }

                val kitName = args[1]
                plugin.kitManager.createKit(kitName, sender.inventory.contents.filterNotNull().toTypedArray())
                sender.sendMessage("§aKit '$kitName' created!")
            }

            "delete" -> {
                if (!sender.hasPermission("event.kit.create")) {
                    sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
                    return true
                }

                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /kit delete <name>")
                    return true
                }

                val kitName = args[1]
                if (plugin.kitManager.deleteKit(kitName)) {
                    sender.sendMessage("§aKit '$kitName' deleted!")
                } else {
                    sender.sendMessage("§cKit not found!")
                }
            }

            "list" -> {
                val kits = plugin.kitManager.getAllKits()
                if (kits.isEmpty()) {
                    sender.sendMessage("§cNo kits available!")
                    return true
                }

                sender.sendMessage("§6§lAvailable Kits:")
                kits.forEach { kit ->
                    sender.sendMessage("§7- §f$kit")
                }
            }

            "set1", "set2", "set3" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /kit ${args[0]} <kitname>")
                    return true
                }

                val slot = args[0].last().toString().toInt()
                val kitName = args[1]

                val data = plugin.statsManager.getPlayerData(sender.uniqueId)

                // Check if kit exists
                if (plugin.kitManager.getKit(kitName) == null) {
                    sender.sendMessage("§cKit not found!")
                    return true
                }

                // Remove from list if already selected
                data.selectedKits.remove(kitName)

                // Ensure list is correct size
                while (data.selectedKits.size < 3) {
                    data.selectedKits.add("")
                }

                // Set kit
                data.selectedKits[slot - 1] = kitName

                val msg = (plugin.config.getString("messages.kit-selected") ?: "")
                    .replace("{kit}", kitName)
                    .replace("{slot}", slot.toString())
                sender.sendMessage((plugin.config.getString("messages.prefix") ?: "") + msg)
            }

            "view" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /kit view <name>")
                    return true
                }

                val kitName = args[1]
                // TODO: Show kit contents
                sender.sendMessage("§aViewing kit: $kitName")
            }

            "setcooldown" -> {
                if (!sender.hasPermission("event.admin")) {
                    sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
                    return true
                }

                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /kit setcooldown <seconds>")
                    return true
                }

                val seconds = args[1].toIntOrNull()
                if (seconds == null || seconds < 0) {
                    sender.sendMessage("§cInvalid seconds!")
                    return true
                }

                plugin.config.set("kits.cooldown-alive", seconds)
                plugin.saveConfig()
                sender.sendMessage("§aKit cooldown set to $seconds seconds!")
            }

            "resetcooldown" -> {
                if (!sender.hasPermission("event.admin")) {
                    sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
                    return true
                }

                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /kit resetcooldown <player>")
                    return true
                }

                val target = Bukkit.getPlayer(args[1])
                if (target == null) {
                    sender.sendMessage("§cPlayer not found!")
                    return true
                }

                val data = plugin.statsManager.getPlayerData(target.uniqueId)
                data.kitCooldown = 0
                sender.sendMessage("§aReset kit cooldown for ${target.name}!")
            }

            else -> {
                sender.sendMessage("§cUnknown subcommand!")
            }
        }

        return true
    }
}

// ==================== KIT SLOT COMMANDS ====================
class KitSlotCommand(private val plugin: EventPlugin, private val slot: Int) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use this command!")
            return true
        }

        if (!sender.hasPermission("event.kit.use")) {
            sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
            return true
        }

        val data = plugin.statsManager.getPlayerData(sender.uniqueId)

        // Check if in kit room
        if (!plugin.kitManager.isInKitRoom(sender.location)) {
            sender.sendMessage("§cYou must be in the kit room to use kits!")
            return true
        }

        // Check if kit room is accessible
        if (!plugin.kitManager.isKitRoomAccessible()) {
            sender.sendMessage("§cKit room is not accessible during the event!")
            return true
        }

        // Check cooldown
        if (!data.canUseKit()) {
            val remaining = data.getRemainingKitCooldown()
            val msg = (plugin.config.getString("messages.kit-cooldown") ?: "")
                .replace("{time}", remaining.toString())
            sender.sendMessage((plugin.config.getString("messages.prefix") ?: "") + msg)
            return true
        }

        // Get kit from slot
        if (data.selectedKits.size < slot || data.selectedKits[slot - 1].isEmpty()) {
            sender.sendMessage("§cNo kit selected in slot $slot!")
            return true
        }

        val kitName = data.selectedKits[slot - 1]

        // Give kit
        if (plugin.kitManager.giveKit(sender, kitName)) {
            sender.sendMessage("§aKit '$kitName' given!")
        } else {
            sender.sendMessage("§cFailed to give kit!")
        }

        return true
    }
}

// ==================== KIT ROOM COMMAND ====================
class KitRoomCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use this command!")
            return true
        }

        if (!sender.hasPermission("event.kit.room")) {
            sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
            return true
        }

        if (args.isEmpty()) {
            // Teleport to kit room
            val warp = plugin.kitManager.getKitRoomWarp(sender.world.name)
            if (warp == null) {
                sender.sendMessage("§cKit room warp not set!")
                return true
            }

            sender.teleport(warp)
            sender.sendMessage("§aTeleported to kit room!")
            return true
        }

        when (args[0].lowercase()) {
            "set" -> {
                if (!sender.hasPermission("event.admin")) {
                    sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
                    return true
                }

                // TODO: Implement WorldEdit-style selection
                sender.sendMessage("§aUse /kitroom set after selecting two positions!")
            }

            "setwarp" -> {
                if (!sender.hasPermission("event.admin")) {
                    sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
                    return true
                }

                plugin.kitManager.setKitRoomWarp(sender.world.name, sender.location)
                sender.sendMessage("§aKit room warp set!")
            }
        }

        return true
    }
}

// ==================== SPAWN COMMANDS ====================
class SetSpawnCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use this command!")
            return true
        }

        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
            return true
        }

        plugin.spawnManager.setSpawn(sender.world.name, sender.location)
        sender.sendMessage("§aSpawn set!")

        return true
    }
}

class SpawnCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use this command!")
            return true
        }

        if (!sender.hasPermission("event.use")) {
            sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
            return true
        }

        if (plugin.spawnManager.teleportToSpawn(sender)) {
            sender.sendMessage("§aTeleported to spawn!")
        } else {
            sender.sendMessage("§cSpawn not set!")
        }

        return true
    }
}

// ==================== INVISIBILITY COMMAND ====================
class InvisCommand(private val plugin: EventPlugin) : CommandExecutor {

    private var invisEnabled = false

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /invis <toggle|on|off>")
            return true
        }

        when (args[0].lowercase()) {
            "toggle" -> {
                invisEnabled = !invisEnabled
            }
            "on" -> {
                invisEnabled = true
            }
            "off" -> {
                invisEnabled = false
            }
            else -> {
                sender.sendMessage("§cUnknown subcommand!")
                return true
            }
        }

        // Apply invisibility
        Bukkit.getOnlinePlayers().forEach { player ->
            val data = plugin.statsManager.getPlayerData(player.uniqueId)

            if (data.isAlive && !data.isSpectator) {
                if (invisEnabled) {
                    player.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Int.MAX_VALUE, 0, false, false))
                } else {
                    player.removePotionEffect(PotionEffectType.INVISIBILITY)
                }
            }
        }

        sender.sendMessage("§aInvisibility ${if (invisEnabled) "enabled" else "disabled"}!")

        return true
    }
}

// ==================== CLEAR COMMAND ====================
class ClearCommand(private val plugin: EventPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage(plugin.config.getString("messages.no-permission") ?: "§cNo permission!")
            return true
        }

        sender.sendMessage("§eClearing blocks... This may take a moment!")

        val materials = listOf(
            Material.OBSIDIAN,
            Material.GLOWSTONE,
            Material.END_CRYSTAL,
            Material.RESPAWN_ANCHOR
        )

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            plugin.worldManager.clearBlocks(materials)
        })

        return true
    }
}