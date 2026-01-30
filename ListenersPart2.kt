package com.eventplugin.listeners

import com.eventplugin.EventPlugin
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent

// ==================== COMBAT LISTENER ====================
class CombatListener(private val plugin: EventPlugin) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        val victim = event.entity

        if (victim !is Player) return

        val victimData = plugin.statsManager.getPlayerData(victim.uniqueId)

        // Prevent damage to spectators
        if (victimData.isSpectator) {
            event.isCancelled = true
            return
        }

        // Prevent damage to frozen players
        if (victimData.isFrozen) {
            event.isCancelled = true
            return
        }

        // Handle player vs player damage
        if (damager is Player) {
            val damagerData = plugin.statsManager.getPlayerData(damager.uniqueId)

            // Prevent spectators from damaging
            if (damagerData.isSpectator) {
                event.isCancelled = true
                return
            }

            // Prevent frozen players from damaging
            if (damagerData.isFrozen) {
                event.isCancelled = true
                return
            }

            // Check grace period
            if (plugin.eventManager.isGracePeriodActive()) {
                event.isCancelled = true
                damager.sendMessage("§c§lGrace period is active! PvP disabled.")
                return
            }

            // Check if event is active
            if (!plugin.eventManager.isEventActive()) {
                if (plugin.config.getBoolean("combat.disable-pvp-outside-event", true)) {
                    event.isCancelled = true
                    damager.sendMessage("§cPvP is disabled outside of events!")
                    return
                }
            }

            // Check friendly fire
            if (plugin.partyManager.isInSameParty(damager.uniqueId, victim.uniqueId)) {
                if (!plugin.config.getBoolean("party.friendly-fire", false)) {
                    event.isCancelled = true
                    damager.sendMessage("§cYou cannot damage party members!")
                    return
                }
            }

            // Apply combat tag
            plugin.combatManager.tagPlayer(victim.uniqueId, damager.uniqueId)
        }

        // Handle projectile damage
        if (damager is org.bukkit.entity.Projectile) {
            val shooter = damager.shooter
            if (shooter is Player) {
                val shooterData = plugin.statsManager.getPlayerData(shooter.uniqueId)

                if (shooterData.isSpectator) {
                    event.isCancelled = true
                    return
                }

                if (shooterData.isFrozen) {
                    event.isCancelled = true
                    return
                }

                if (plugin.eventManager.isGracePeriodActive()) {
                    event.isCancelled = true
                    return
                }

                if (plugin.partyManager.isInSameParty(shooter.uniqueId, victim.uniqueId)) {
                    if (!plugin.config.getBoolean("party.friendly-fire", false)) {
                        event.isCancelled = true
                        return
                    }
                }

                plugin.combatManager.tagPlayer(victim.uniqueId, shooter.uniqueId)
            }
        }
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity !is Player) return

        val data = plugin.statsManager.getPlayerData(entity.uniqueId)

        // Prevent damage to spectators
        if (data.isSpectator) {
            event.isCancelled = true
            return
        }

        // Prevent damage during grace period (except void)
        if (plugin.eventManager.isGracePeriodActive() && event.cause != EntityDamageEvent.DamageCause.VOID) {
            event.isCancelled = true
            return
        }

        // Track totem pops for surge
        if (event.cause == EntityDamageEvent.DamageCause.VOID) {
            return // Don't track void damage
        }

        // Check if player would die from this damage
        if (entity.health - event.finalDamage <= 0) {
            // Check for totem in hand
            val mainHand = entity.inventory.itemInMainHand
            val offHand = entity.inventory.itemInOffHand

            if (mainHand.type == Material.TOTEM_OF_UNDYING || offHand.type == Material.TOTEM_OF_UNDYING) {
                data.surgePops++
                entity.sendActionBar("§6§lTOTEM POP! §7Surge Pops: §f${data.surgePops}§7/§f${plugin.surgeManager.getRequiredPops()}")
            }
        }
    }
}

// ==================== BLOCK LISTENER ====================
class BlockListener(private val plugin: EventPlugin) : Listener {

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val data = plugin.statsManager.getPlayerData(player.uniqueId)

        // Spectators can't break blocks
        if (data.isSpectator) {
            event.isCancelled = true
            return
        }

        // Frozen players can't break blocks
        if (data.isFrozen) {
            event.isCancelled = true
            return
        }

        // Check if block breaking is allowed
        if (!plugin.eventManager.isEventActive()) {
            if (!player.hasPermission("event.admin")) {
                if (plugin.config.getBoolean("world.disable-block-break-outside-event", false)) {
                    event.isCancelled = true
                    return
                }
            }
        }

        // Prevent breaking in kit room
        if (plugin.kitManager.isInKitRoom(event.block.location)) {
            if (!player.hasPermission("event.admin")) {
                event.isCancelled = true
                player.sendMessage("§cYou cannot break blocks in the kit room!")
                return
            }
        }

        // Track golden apple consumption for surge
        val block = event.block
        if (block.type == Material.GOLD_BLOCK) {
            // Optional: Add surge pops for mining gold
        }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val data = plugin.statsManager.getPlayerData(player.uniqueId)

        // Spectators can't place blocks
        if (data.isSpectator) {
            event.isCancelled = true
            return
        }

        // Frozen players can't place blocks
        if (data.isFrozen) {
            event.isCancelled = true
            return
        }

        // Check if block placing is allowed
        if (!plugin.eventManager.isEventActive()) {
            if (!player.hasPermission("event.admin")) {
                if (plugin.config.getBoolean("world.disable-block-place-outside-event", false)) {
                    event.isCancelled = true
                    return
                }
            }
        }

        // Prevent placing in kit room
        if (plugin.kitManager.isInKitRoom(event.block.location)) {
            if (!player.hasPermission("event.admin")) {
                event.isCancelled = true
                player.sendMessage("§cYou cannot place blocks in the kit room!")
                return
            }
        }

        // Restrict certain blocks
        val restrictedBlocks = listOf(
            Material.BEDROCK,
            Material.BARRIER,
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK
        )

        if (restrictedBlocks.contains(event.block.type) && !player.hasPermission("event.admin")) {
            event.isCancelled = true
            player.sendMessage("§cYou cannot place this block!")
        }
    }
}

// ==================== PLAYER MOVE LISTENER ====================
class PlayerMoveListener(private val plugin: EventPlugin) : Listener {

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val data = plugin.statsManager.getPlayerData(player.uniqueId)

        // Freeze check
        if (data.isFrozen) {
            val from = event.from
            val to = event.to ?: return

            if (from.x != to.x || from.z != to.z) {
                event.isCancelled = true
                return
            }
        }

        // Height limit check
        if (plugin.eventManager.isEventActive() && data.isAlive && !data.isSpectator) {
            val maxHeight = plugin.config.getInt("event.max-height", 320)
            val warningHeight = plugin.config.getInt("event.warning-height", 300)

            if (player.location.y > maxHeight) {
                // Apply massive damage
                player.damage(plugin.config.getDouble("event.height-damage", 10.0))
                player.sendTitle("§c§lTOO HIGH!", "§eGet below Y=${maxHeight}!", 0, 20, 10)
            } else if (player.location.y > warningHeight) {
                // Warning
                player.sendActionBar("§e§lWARNING: §cYou are too high! Get below Y=${maxHeight}")
            }
        }

        // Border damage check
        if (plugin.eventManager.isEventActive() && data.isAlive && !data.isSpectator) {
            val world = player.world
            val border = world.worldBorder
            val center = border.center
            val radius = border.size / 2

            val distance = player.location.distance(center)
            if (distance > radius) {
                val damage = plugin.config.getDouble("border.damage-per-tick", 1.0)
                player.damage(damage)
                player.sendActionBar("§c§lOUTSIDE BORDER! §eTake damage!")
            }
        }

        // Kit room access check during event
        if (plugin.eventManager.isEventActive()) {
            val to = event.to ?: return

            if (plugin.kitManager.isInKitRoom(to) && !plugin.kitManager.isKitRoomAccessible()) {
                if (!player.hasPermission("event.admin")) {
                    event.isCancelled = true
                    player.sendMessage("§cKit room is closed during the event!")
                }
            }
        }
    }
}