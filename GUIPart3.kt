package com.eventplugin.gui

import com.eventplugin.EventPlugin
import com.eventplugin.utils.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory

// ==================== TITLES GUI ====================
class TitlesGUI(private val plugin: EventPlugin) {

    private val titles = mapOf(
        "crystalranked" to ChatColor.AQUA,
        "aurora" to ChatColor.LIGHT_PURPLE,
        "mythic" to ChatColor.GOLD,
        "legend" to ChatColor.RED,
        "champion" to ChatColor.YELLOW,
        "master" to ChatColor.DARK_PURPLE,
        "elite" to ChatColor.BLUE,
        "veteran" to ChatColor.GREEN
    )

    fun open(player: Player) {
        val inventory = createInventory(player)
        player.openInventory(inventory)
    }

    private fun createInventory(player: Player): Inventory {
        val inventory = Bukkit.createInventory(null, 27, "§6§lSelect Your Title")

        val data = plugin.statsManager.getPlayerData(player.uniqueId)
        val currentTitle = data.selectedTitle ?: ""

        var slot = 10
        titles.forEach { (name, color) ->
            val isSelected = currentTitle.equals(name, ignoreCase = true)

            val material = if (isSelected) Material.ENCHANTED_BOOK else Material.BOOK

            inventory.setItem(slot, ItemBuilder.from(material)
                .name("$color§l${name.uppercase()}")
                .lore(
                    if (isSelected) "§a§l✔ SELECTED" else "§7Click to select",
                    "",
                    "§7Preview: $color${player.name}",
                    "",
                    if (isSelected) "§eClick to unequip" else "§eClick to equip"
                )
                .glow(isSelected)
                .build())

            slot++
            if (slot == 17) slot = 19 // Skip to next row
        }

        // Remove title button
        inventory.setItem(22, ItemBuilder.from(Material.BARRIER)
            .name("§c§lRemove Title")
            .lore(
                "§7Remove your current title",
                "",
                "§eClick to remove"
            )
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
        val item = event.currentItem ?: return

        if (item.type == Material.BARRIER) {
            // Remove title
            val data = plugin.statsManager.getPlayerData(player.uniqueId)
            data.selectedTitle = null
            player.sendMessage("§a✔ Title removed!")
            player.closeInventory()
            return
        }

        if (item.type == Material.BOOK || item.type == Material.ENCHANTED_BOOK) {
            val titleName = ChatColor.stripColor(item.itemMeta?.displayName ?: "") ?: return

            val data = plugin.statsManager.getPlayerData(player.uniqueId)

            if (titleName.equals(data.selectedTitle, ignoreCase = true)) {
                // Unequip
                data.selectedTitle = null
                player.sendMessage("§a✔ Title unequipped!")
            } else {
                // Equip
                data.selectedTitle = titleName.lowercase()
                player.sendMessage("§a✔ Title equipped: ${titles[titleName.lowercase()]}§l$titleName")
            }

            player.closeInventory()
            open(player)
        }
    }
}

// ==================== UPDATED SETTINGS GUI WITH ALL TOGGLES ====================
class AdminSettingsGUI(private val plugin: EventPlugin) {

    fun open(player: Player) {
        val inventory = createInventory()
        player.openInventory(inventory)
    }

    private fun createInventory(): Inventory {
        val inventory = Bukkit.createInventory(null, 54, "§c§lAdmin Settings")

        // Row 1: Event Settings
        inventory.setItem(10, createToggleItem(
            Material.SHIELD,
            "§a§lGrace Period",
            "spectator.join-as-spectator",
            "Players join as spectators during event"
        ))

        inventory.setItem(11, createToggleItem(
            Material.BEDROCK,
            "§6§lSpawn Protection",
            "world.spawn-protection-enabled",
            "Protect spawn area from damage"
        ))

        inventory.setItem(12, createToggleItem(
            Material.GOLDEN_APPLE,
            "§e§lAuto Rekit",
            "kits.auto-rekit-enabled",
            "Automatically give kit after deaths"
        ))

        inventory.setItem(13, ItemBuilder.from(Material.NETHER_STAR)
            .name("§d§lAuto Rekit Deaths")
            .lore(
                "§7Current: §f${plugin.config.getInt("kits.auto-rekit-deaths", 3)} deaths",
                "",
                "§eLeft-Click: §7+1",
                "§eRight-Click: §7-1",
                "§eShift-Click: §7Set custom"
            )
            .build())

        inventory.setItem(14, createToggleItem(
            Material.IRON_SWORD,
            "§c§lFriendly Fire",
            "party.friendly-fire",
            "Allow party members to damage each other"
        ))

        inventory.setItem(15, createToggleItem(
            Material.PAPER,
            "§f§lWhitelist Mode",
            "event.whitelist-enabled",
            "Only whitelisted players can join"
        ))

        inventory.setItem(16, createToggleItem(
            Material.REDSTONE,
            "§c§lSurge System",
            "surge.enabled",
            "Enable surge damage system"
        ))

        // Row 2: Kit Settings
        inventory.setItem(19, ItemBuilder.from(Material.CHEST)
            .name("§b§lKit Cooldown (Alive)")
            .lore(
                "§7Current: §f${plugin.config.getInt("kits.cooldown-alive", 45)}s",
                "",
                "§eClick to modify"
            )
            .build())

        inventory.setItem(20, ItemBuilder.from(Material.ENDER_CHEST)
            .name("§7§lKit Cooldown (Spectator)")
            .lore(
                "§7Current: §f${plugin.config.getInt("kits.cooldown-spectator", 0)}s",
                "",
                "§eClick to modify"
            )
            .build())

        inventory.setItem(21, createToggleItem(
            Material.DIAMOND_SWORD,
            "§b§lKit Room Only",
            "kits.only-in-kitroom",
            "Kits can only be used in kit room"
        ))

        inventory.setItem(22, createToggleItem(
            Material.BARRIER,
            "§c§lClose Kit Room",
            "kits.close-kitroom-during-event",
            "Close kit room during active events"
        ))

        // Row 3: Combat Settings
        inventory.setItem(28, ItemBuilder.from(Material.CLOCK)
            .name("§c§lCombat Tag Duration")
            .lore(
                "§7Current: §f${plugin.config.getInt("combat.tag-duration", 15)}s",
                "",
                "§eClick to modify"
            )
            .build())

        inventory.setItem(29, createToggleItem(
            Material.OAK_DOOR,
            "§c§lPrevent Combat Logout",
            "combat.prevent-logout",
            "Prevent players from logging out in combat"
        ))

        inventory.setItem(30, createToggleItem(
            Material.SKELETON_SKULL,
            "§4§lCombat Logout Kill",
            "combat.logout-kill",
            "Kill players who logout during combat"
        ))

        inventory.setItem(31, createToggleItem(
            Material.DIAMOND_CHESTPLATE,
            "§6§lDisable PvP Outside Event",
            "combat.disable-pvp-outside-event",
            "Disable PvP when event is not active"
        ))

        // Row 4: World Settings
        inventory.setItem(37, createToggleItem(
            Material.TNT,
            "§e§lClear Entities on Reset",
            "world.clear-entities-on-reset",
            "Remove all entities when world resets"
        ))

        inventory.setItem(38, createToggleItem(
            Material.GRASS_BLOCK,
            "§2§lClear Drops on Reset",
            "world.clear-drops-on-reset",
            "Remove all drops when world resets"
        ))

        inventory.setItem(39, createToggleItem(
            Material.IRON_PICKAXE,
            "§7§lBlock Break Outside Event",
            "world.disable-block-break-outside-event",
            "Prevent block breaking outside events"
        ))

        inventory.setItem(40, createToggleItem(
            Material.STONE,
            "§7§lBlock Place Outside Event",
            "world.disable-block-place-outside-event",
            "Prevent block placing outside events"
        ))

        // Admin Actions Row
        inventory.setItem(45, ItemBuilder.from(Material.COMMAND_BLOCK)
            .name("§d§lGive Kit to All")
            .lore(
                "§7Give a kit to all players",
                "",
                "§eClick to select"
            )
            .build())

        inventory.setItem(46, ItemBuilder.from(Material.BEDROCK)
            .name("§8§lDrop Bedrock")
            .lore(
                "§7Drop a bedrock block",
                "",
                "§eClick to drop"
            )
            .build())

        inventory.setItem(47, ItemBuilder.from(Material.DEEPSLATE)
            .name("§8§lDrop Deepslate")
            .lore(
                "§7Drop a deepslate block",
                "",
                "§eClick to drop"
            )
            .build())

        // Back button
        inventory.setItem(49, ItemBuilder.from(Material.ARROW)
            .name("§e§lBack to Main Menu")
            .build())

        // Close button
        inventory.setItem(53, ItemBuilder.from(Material.BARRIER)
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

    private fun createToggleItem(material: Material, name: String, configPath: String, description: String): org.bukkit.inventory.ItemStack {
        val enabled = plugin.config.getBoolean(configPath, false)

        return ItemBuilder.from(material)
            .name(name)
            .lore(
                "§7$description",
                "",
                "§7Status: ${if (enabled) "§a§l✔ ENABLED" else "§c§l✖ DISABLED"}",
                "",
                "§eClick to toggle"
            )
            .glow(enabled)
            .build()
    }

    fun handleClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val slot = event.slot

        when (slot) {
            // Toggle items
            10 -> toggleConfig(player, "spectator.join-as-spectator", "Join as Spectator")
            11 -> toggleConfig(player, "world.spawn-protection-enabled", "Spawn Protection")
            12 -> toggleConfig(player, "kits.auto-rekit-enabled", "Auto Rekit")
            13 -> {
                // Auto rekit deaths
                val current = plugin.config.getInt("kits.auto-rekit-deaths", 3)
                val new = if (event.isLeftClick) current + 1 else maxOf(1, current - 1)
                plugin.config.set("kits.auto-rekit-deaths", new)
                plugin.saveConfig()
                player.sendMessage("§a✔ Auto rekit deaths set to $new")
                player.closeInventory()
                open(player)
            }
            14 -> toggleConfig(player, "party.friendly-fire", "Friendly Fire")
            15 -> toggleConfig(player, "event.whitelist-enabled", "Whitelist Mode")
            16 -> {
                plugin.surgeManager.toggle()
                player.sendMessage("§a✔ Surge toggled!")
                player.closeInventory()
                open(player)
            }
            21 -> toggleConfig(player, "kits.only-in-kitroom", "Kit Room Only")
            22 -> toggleConfig(player, "kits.close-kitroom-during-event", "Close Kit Room")
            29 -> toggleConfig(player, "combat.prevent-logout", "Prevent Combat Logout")
            30 -> toggleConfig(player, "combat.logout-kill", "Combat Logout Kill")
            31 -> toggleConfig(player, "combat.disable-pvp-outside-event", "Disable PvP Outside Event")
            37 -> toggleConfig(player, "world.clear-entities-on-reset", "Clear Entities")
            38 -> toggleConfig(player, "world.clear-drops-on-reset", "Clear Drops")
            39 -> toggleConfig(player, "world.disable-block-break-outside-event", "Block Break Outside Event")
            40 -> toggleConfig(player, "world.disable-block-place-outside-event", "Block Place Outside Event")

            // Admin actions
            45 -> {
                player.closeInventory()
                KitAllSelectionGUI(plugin).open(player)
            }
            46 -> {
                player.closeInventory()
                player.performCommand("drop bedrock")
            }
            47 -> {
                player.closeInventory()
                player.performCommand("drop deepslate")
            }

            // Navigation
            49 -> EventGUI(plugin).open(player)
            53 -> player.closeInventory()
        }
    }

    private fun toggleConfig(player: Player, path: String, name: String) {
        val current = plugin.config.getBoolean(path, false)
        plugin.config.set(path, !current)
        plugin.saveConfig()

        val status = if (!current) "§a§lENABLED" else "§c§lDISABLED"
        player.sendMessage("§a✔ $name $status")
        player.closeInventory()
        open(player)
    }
}

// ==================== KIT ALL SELECTION GUI ====================
class KitAllSelectionGUI(private val plugin: EventPlugin) {

    fun open(player: Player) {
        val inventory = createInventory()
        player.openInventory(inventory)
    }

    private fun createInventory(): Inventory {
        val inventory = Bukkit.createInventory(null, 27, "§d§lGive Kit to All Players")

        inventory.setItem(11, ItemBuilder.from(Material.DIAMOND_SWORD)
            .name("§b§lKit Slot 1")
            .lore(
                "§7Give everyone their kit 1",
                "",
                "§eClick to execute"
            )
            .build())

        inventory.setItem(13, ItemBuilder.from(Material.DIAMOND_CHESTPLATE)
            .name("§b§lKit Slot 2")
            .lore(
                "§7Give everyone their kit 2",
                "",
                "§eClick to execute"
            )
            .build())

        inventory.setItem(15, ItemBuilder.from(Material.DIAMOND_HELMET)
            .name("§b§lKit Slot 3")
            .lore(
                "§7Give everyone their kit 3",
                "",
                "§eClick to execute"
            )
            .build())

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

        when (event.slot) {
            11 -> {
                player.closeInventory()
                player.performCommand("kitall k1")
            }
            13 -> {
                player.closeInventory()
                player.performCommand("kitall k2")
            }
            15 -> {
                player.closeInventory()
                player.performCommand("kitall k3")
            }
            22 -> AdminSettingsGUI(plugin).open(player)
        }
    }
}