package com.eventplugin.gui

import com.eventplugin.EventPlugin
import com.eventplugin.utils.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory

// ==================== EVENT GUI (FIXED) ====================
class EventGUI(private val plugin: EventPlugin) {

    fun open(player: Player) {
        val inventory = createInventory()
        player.openInventory(inventory)
    }

    private fun createInventory(): Inventory {
        val inventory = Bukkit.createInventory(null, 54, "§6§lEvent Control Panel")

        // Start Event
        inventory.setItem(10, ItemBuilder.from(Material.LIME_CONCRETE)
            .name("§a§lStart Event")
            .lore(
                "§7Click to start the event",
                "§7Requires minimum players: §f${plugin.config.getInt("event.min-players")}",
                "",
                "§7Current players: §f${Bukkit.getOnlinePlayers().size}"
            )
            .build())

        // Force Start Event
        inventory.setItem(11, ItemBuilder.from(Material.ORANGE_CONCRETE)
            .name("§6§lForce Start Event")
            .lore(
                "§7Click to force start",
                "§7Bypasses minimum player requirement"
            )
            .build())

        // Stop Event
        inventory.setItem(12, ItemBuilder.from(Material.RED_CONCRETE)
            .name("§c§lStop Event")
            .lore(
                "§7Click to stop the current event",
                "§7Event Active: §f${plugin.eventManager.isEventActive()}"
            )
            .build())

        // Pause/Resume
        inventory.setItem(14, ItemBuilder.from(Material.YELLOW_CONCRETE)
            .name("§e§lPause/Resume Border")
            .lore(
                "§7Click to pause or resume",
                "§7border shrinking"
            )
            .build())

        // Border Controls (Phase 1-11)
        val borderSlots = listOf(19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31)
        for (i in 1..11) {
            val slot = borderSlots[i - 1]
            val size = plugin.config.getInt("border.phases.$i.size", 100)
            val time = plugin.config.getInt("border.phases.$i.default-time", 60)

            inventory.setItem(slot, ItemBuilder.from(Material.BARRIER)
                .name("§c§lBorder Phase $i")
                .lore(
                    "§7Size: §f${size} blocks",
                    "§7Time: §f${time}s",
                    "",
                    "§eClick: §7Shrink with default time"
                )
                .build())
        }

        // Revive All
        inventory.setItem(15, ItemBuilder.from(Material.TOTEM_OF_UNDYING)
            .name("§a§lRevive All Players")
            .lore(
                "§7Click to revive all dead players"
            )
            .build())

        // Settings
        inventory.setItem(16, ItemBuilder.from(Material.COMPARATOR)
            .name("§b§lSettings")
            .lore(
                "§7Click to open settings menu"
            )
            .build())

        // World Management
        inventory.setItem(37, ItemBuilder.from(Material.GRASS_BLOCK)
            .name("§2§lRegenerate World")
            .lore(
                "§7Click to regenerate the event world",
                "§c§lWarning: This cannot be undone!"
            )
            .build())

        // Clear Blocks
        inventory.setItem(38, ItemBuilder.from(Material.TNT)
            .name("§c§lClear Blocks")
            .lore(
                "§7Click to clear obsidian, glowstone,",
                "§7crystals, and anchors in border"
            )
            .build())

        // Player List
        inventory.setItem(42, ItemBuilder.from(Material.PLAYER_HEAD)
            .name("§e§lPlayer List")
            .lore(
                "§7Click to view all players"
            )
            .build())

        // Leaderboard
        inventory.setItem(43, ItemBuilder.from(Material.GOLD_INGOT)
            .name("§6§lLeaderboard")
            .lore(
                "§7Top players by wins, kills, K/D"
            )
            .build())

        // Close
        inventory.setItem(49, ItemBuilder.from(Material.BARRIER)
            .name("§c§lClose")
            .build())

        // Fill empty slots with glass panes
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
            10 -> { // Start Event
                player.closeInventory()
                player.performCommand("event start")
            }
            11 -> { // Force Start
                player.closeInventory()
                player.performCommand("event forcestart")
            }
            12 -> { // Stop Event
                player.closeInventory()
                player.performCommand("event stop")
            }
            14 -> { // Pause/Resume
                player.closeInventory()
                if (plugin.borderManager.getCurrentPhase() > 0) {
                    plugin.borderManager.pauseShrinking()
                    player.sendMessage("§eBorder paused!")
                } else {
                    plugin.borderManager.resumeShrinking()
                    player.sendMessage("§aBorder resumed!")
                }
            }
            15 -> { // Revive All
                player.closeInventory()
                player.performCommand("revall")
            }
            16 -> { // Settings
                SettingsGUI(plugin).open(player)
            }
            19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31 -> { // Border phases
                val borderSlots = listOf(19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31)
                val phase = borderSlots.indexOf(slot) + 1
                player.closeInventory()
                player.performCommand("border shrink$phase 60")
            }
            37 -> { // Regenerate World
                player.closeInventory()
                plugin.worldManager.regenerateWorld()
            }
            38 -> { // Clear Blocks
                player.closeInventory()
                player.performCommand("clear")
            }
            42 -> { // Player List
                PlayerListGUI(plugin).open(player)
            }
            49 -> { // Close
                player.closeInventory()
            }
        }
    }
}

// ==================== SETTINGS GUI ====================
class SettingsGUI(private val plugin: EventPlugin) {

    fun open(player: Player) {
        val inventory = createInventory()
        player.openInventory(inventory)
    }

    private fun createInventory(): Inventory {
        val inventory = Bukkit.createInventory(null, 27, "§b§lEvent Settings")

        // Grace Period
        inventory.setItem(10, ItemBuilder.from(Material.SHIELD)
            .name("§a§lGrace Period")
            .lore(
                "§7Current: §f${plugin.config.getInt("event.grace-period")}s",
                "",
                "§eClick to modify"
            )
            .build())

        // Event Mode
        inventory.setItem(11, ItemBuilder.from(Material.DIAMOND_SWORD)
            .name("§6§lEvent Mode")
            .lore(
                "§7Current: §f${plugin.eventManager.getEventMode()}",
                "",
                "§7Available modes:",
                "§f- SOLOS",
                "§f- DUOS",
                "§f- TRIOS",
                "§f- SQUADS",
                "§f- BLITZ",
                "§f- UHC"
            )
            .build())

        // Max Party Size
        inventory.setItem(12, ItemBuilder.from(Material.PLAYER_HEAD)
            .name("§e§lMax Party Size")
            .lore(
                "§7Current: §f${plugin.partyManager.getMaxPartySize()}",
                "",
                "§eLeft-Click: §7Increase",
                "§eRight-Click: §7Decrease"
            )
            .build())

        // Surge Settings
        inventory.setItem(13, ItemBuilder.from(Material.GOLDEN_APPLE)
            .name("§6§lSurge Settings")
            .lore(
                "§7Enabled: §f${plugin.surgeManager.isEnabled()}",
                "§7Required Pops: §f${plugin.surgeManager.getRequiredPops()}",
                "",
                "§eClick to toggle"
            )
            .build())

        // Friendly Fire
        inventory.setItem(14, ItemBuilder.from(Material.IRON_SWORD)
            .name("§c§lFriendly Fire")
            .lore(
                "§7Current: §f${if (plugin.config.getBoolean("party.friendly-fire")) "Enabled" else "Disabled"}",
                "",
                "§eClick to toggle"
            )
            .build())

        // Whitelist Mode
        inventory.setItem(15, ItemBuilder.from(Material.PAPER)
            .name("§f§lWhitelist Mode")
            .lore(
                "§7Current: §f${if (plugin.adminManager.isWhitelistMode()) "Enabled" else "Disabled"}",
                "",
                "§eClick to toggle"
            )
            .build())

        // Back
        inventory.setItem(22, ItemBuilder.from(Material.ARROW)
            .name("§e§lBack")
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
            13 -> { // Surge toggle
                plugin.surgeManager.toggle()
                player.closeInventory()
                SettingsGUI(plugin).open(player)
            }
            14 -> { // Friendly fire
                val current = plugin.config.getBoolean("party.friendly-fire")
                plugin.config.set("party.friendly-fire", !current)
                plugin.saveConfig()
                player.closeInventory()
                SettingsGUI(plugin).open(player)
            }
            15 -> { // Whitelist
                val enabled = !plugin.adminManager.isWhitelistMode()
                plugin.adminManager.setWhitelistMode(enabled)
                player.closeInventory()
                SettingsGUI(plugin).open(player)
            }
            22 -> { // Back
                EventGUI(plugin).open(player)
            }
        }
    }
}

// ==================== KIT GUI ====================
class KitGUI(private val plugin: EventPlugin) {

    fun open(player: Player) {
        val inventory = createInventory(player)
        player.openInventory(inventory)
    }

    private fun createInventory(player: Player): Inventory {
        val inventory = Bukkit.createInventory(null, 54, "§6§lKit Selection")
        val data = plugin.statsManager.getPlayerData(player.uniqueId)
        val kits = plugin.kitManager.getAllKits().toList()

        // Display available kits
        var slot = 10
        kits.forEach { kitName ->
            if (slot >= 44) return@forEach

            val isSelected = data.selectedKits.contains(kitName)
            val material = if (isSelected) Material.LIME_STAINED_GLASS_PANE else Material.CHEST

            inventory.setItem(slot, ItemBuilder.from(material)
                .name("§6§l$kitName")
                .lore(
                    if (isSelected) "§a§lSELECTED" else "§7Click to select",
                    "",
                    "§eLeft-Click: §7Select for slot 1",
                    "§eRight-Click: §7Select for slot 2",
                    "§eShift-Click: §7Select for slot 3"
                )
                .build())

            slot++
            if (slot % 9 == 8) slot += 2 // Skip to next row
        }

        // Show selected kits
        inventory.setItem(48, ItemBuilder.from(Material.DIAMOND_SWORD)
            .name("§b§lKit Slot 1")
            .lore(
                "§7Current: §f${data.selectedKits.getOrNull(0) ?: "None"}",
                "",
                "§eCommand: §7/k1"
            )
            .build())

        inventory.setItem(49, ItemBuilder.from(Material.DIAMOND_CHESTPLATE)
            .name("§b§lKit Slot 2")
            .lore(
                "§7Current: §f${data.selectedKits.getOrNull(1) ?: "None"}",
                "",
                "§eCommand: §7/k2"
            )
            .build())

        inventory.setItem(50, ItemBuilder.from(Material.DIAMOND_HELMET)
            .name("§b§lKit Slot 3")
            .lore(
                "§7Current: §f${data.selectedKits.getOrNull(2) ?: "None"}",
                "",
                "§eCommand: §7/k3"
            )
            .build())

        // Close button
        inventory.setItem(53, ItemBuilder.from(Material.BARRIER)
            .name("§c§lClose")
            .build())

        return inventory
    }

    fun handleClick(event: InventoryClickEvent, player: Player) {
        val slot = event.slot

        if (slot == 53) {
            player.closeInventory()
            return
        }

        val item = event.currentItem ?: return
        if (item.type == Material.GRAY_STAINED_GLASS_PANE) return

        val kitName = org.bukkit.ChatColor.stripColor(item.itemMeta?.displayName ?: "") ?: return
        if (kitName.isBlank()) return

        val slotNum = when {
            event.isShiftClick -> 3
            event.isRightClick -> 2
            event.isLeftClick -> 1
            else -> 1
        }

        player.performCommand("kit set$slotNum $kitName")
        player.closeInventory()
    }
}