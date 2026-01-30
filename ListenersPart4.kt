package com.eventplugin.listeners

import com.eventplugin.EventPlugin
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent

// ==================== SPECTATOR LISTENER ====================
class SpectatorListener(private val plugin: EventPlugin) : Listener {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val data = plugin.statsManager.getPlayerData(player.uniqueId)

        if (!data.isSpectator) return

        // Spectators can't interact with blocks (except compass)
        if (event.item?.type != Material.COMPASS) {
            event.isCancelled = true
        }

        // Handle spectator compass click
        if (event.item?.type == Material.COMPASS) {
            event.isCancelled = true
            // Open spectator GUI (will be in GUI files)
            com.eventplugin.gui.SpectatorCompassGUI(plugin).open(player)
        }
    }

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val data = plugin.statsManager.getPlayerData(player.uniqueId)

        if (data.isSpectator) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        val data = plugin.statsManager.getPlayerData(player.uniqueId)

        if (data.isSpectator) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onEntityPickupItem(event: EntityPickupItemEvent) {
        val entity = event.entity
        if (entity !is Player) return

        val data = plugin.statsManager.getPlayerData(entity.uniqueId)

        if (data.isSpectator) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        val entity = event.entity
        if (entity !is Player) return

        val data = plugin.statsManager.getPlayerData(entity.uniqueId)

        // Spectators don't lose hunger
        if (data.isSpectator) {
            event.isCancelled = true
            entity.foodLevel = 20
        }

        // Disable hunger loss during grace period
        if (plugin.eventManager.isGracePeriodActive()) {
            event.isCancelled = true
            entity.foodLevel = 20
        }
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity !is Player) return

        val data = plugin.statsManager.getPlayerData(entity.uniqueId)

        // Spectators take no damage
        if (data.isSpectator) {
            event.isCancelled = true

            // Teleport spectators back if they fall into void
            if (event.cause == EntityDamageEvent.DamageCause.VOID) {
                val spawn = plugin.spawnManager.getSpawn(entity.world.name)
                if (spawn != null) {
                    entity.teleport(spawn)
                }
            }
        }
    }
}

// ==================== INVENTORY LISTENER ====================
class InventoryListener(private val plugin: EventPlugin) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val data = plugin.statsManager.getPlayerData(player.uniqueId)

        // Spectators can't modify inventory (except in spectator mode GUI)
        if (data.isSpectator && event.inventory.type != InventoryType.CHEST) {
            if (player.gameMode != GameMode.SPECTATOR) {
                event.isCancelled = true
                return
            }
        }

        // Frozen players can't modify inventory
        if (data.isFrozen) {
            event.isCancelled = true
            return
        }

        // Handle GUI clicks (will check for custom inventory titles)
        val title = event.view.title

        // Check if this is a custom GUI
        if (title.contains("Event Control") ||
            title.contains("Kit Selection") ||
            title.contains("Stats") ||
            title.contains("Player List") ||
            title.contains("Spectator")) {
            event.isCancelled = true

            val clicked = event.currentItem ?: return

            // Handle click based on GUI type (delegated to GUI classes)
            // The actual click handling is in the respective GUI files
        }
    }
}

// ==================== DAMAGE LISTENER ====================
class DamageListener(private val plugin: EventPlugin) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity !is Player) return

        val data = plugin.statsManager.getPlayerData(entity.uniqueId)

        // Track golden apple consumption
        if (event.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            // This is handled in CombatListener
            return
        }

        // Fall damage during grace period
        if (plugin.eventManager.isGracePeriodActive()) {
            if (event.cause == EntityDamageEvent.DamageCause.FALL) {
                if (plugin.config.getBoolean("event.disable-fall-damage-grace", true)) {
                    event.isCancelled = true
                    return
                }
            }
        }

        // UHC mode - no natural regeneration
        if (plugin.eventManager.getEventMode() == "UHC") {
            entity.setHealth(minOf(entity.health, entity.maxHealth))
        }
    }

    @EventHandler
    fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        val player = event.player
        val item = event.item
        val data = plugin.statsManager.getPlayerData(player.uniqueId)

        // Track golden apple consumption for surge
        if (item.type == Material.GOLDEN_APPLE || item.type == Material.ENCHANTED_GOLDEN_APPLE) {
            data.surgePops++
            player.sendActionBar("§6§l✦ GAPPLE CONSUMED! §7Surge Pops: §f${data.surgePops}§7/§f${plugin.surgeManager.getRequiredPops()}")
        }

        // UHC mode - disable natural regeneration from food
        if (plugin.eventManager.getEventMode() == "UHC") {
            // Prevent regeneration from certain foods if configured
            if (plugin.config.getBoolean("uhc.disable-natural-regen", true)) {
                player.removePotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION)
            }
        }
    }

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        val entity = event.entity
        if (entity !is Player) return

        // Handle hunger in different event modes
        when (plugin.eventManager.getEventMode()) {
            "BLITZ" -> {
                // Faster hunger depletion in blitz mode
                if (plugin.config.getBoolean("blitz.faster-hunger", false)) {
                    entity.foodLevel = maxOf(0, entity.foodLevel - 1)
                }
            }
            "UHC" -> {
                // UHC hunger mechanics (if any special rules)
            }
        }
    }
}