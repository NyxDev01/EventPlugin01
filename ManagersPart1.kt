package com.eventplugin.managers

import com.eventplugin.EventPlugin
import com.eventplugin.data.PartyData
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.util.*

// ==================== PARTY MANAGER ====================
class PartyManager(private val plugin: EventPlugin) {

    private val parties = mutableMapOf<UUID, PartyData>()
    private val playerParties = mutableMapOf<UUID, UUID>()
    private var maxPartySize = 4

    private val partyColors = listOf(
        ChatColor.AQUA, ChatColor.GREEN, ChatColor.YELLOW,
        ChatColor.LIGHT_PURPLE, ChatColor.RED, ChatColor.BLUE,
        ChatColor.GOLD, ChatColor.DARK_GREEN, ChatColor.DARK_PURPLE
    )
    private var colorIndex = 0

    fun createParty(leader: UUID): PartyData? {
        if (playerParties.containsKey(leader)) return null

        val party = PartyData(
            leader = leader,
            color = getNextColor()
        )

        parties[party.partyId] = party
        playerParties[leader] = party.partyId

        updatePartyGlow(party)
        return party
    }

    fun disbandParty(partyId: UUID) {
        val party = parties[partyId] ?: return

        party.members.forEach { uuid ->
            playerParties.remove(uuid)
            Bukkit.getPlayer(uuid)?.let { removeGlow(it) }
        }

        parties.remove(partyId)
    }

    fun disbandAllParties() {
        parties.keys.toList().forEach { disbandParty(it) }
    }

    fun invitePlayer(partyId: UUID, target: UUID): Boolean {
        val party = parties[partyId] ?: return false

        if (party.isFull(maxPartySize)) return false
        if (playerParties.containsKey(target)) return false

        party.addInvite(target)
        return true
    }

    fun acceptInvite(player: UUID, partyId: UUID): Boolean {
        val party = parties[partyId] ?: return false

        if (!party.hasInvite(player)) return false
        if (party.isFull(maxPartySize)) return false
        if (playerParties.containsKey(player)) return false

        party.addMember(player)
        playerParties[player] = partyId

        updatePartyGlow(party)
        return true
    }

    fun leaveParty(player: UUID): Boolean {
        val partyId = playerParties[player] ?: return false
        val party = parties[partyId] ?: return false

        party.removeMember(player)
        playerParties.remove(player)

        Bukkit.getPlayer(player)?.let { removeGlow(it) }

        if (party.isLeader(player)) {
            disbandParty(partyId)
            return true
        }

        if (party.members.isEmpty()) {
            disbandParty(partyId)
        } else {
            updatePartyGlow(party)
        }

        return true
    }

    fun kickMember(leader: UUID, target: UUID): Boolean {
        val partyId = playerParties[leader] ?: return false
        val party = parties[partyId] ?: return false

        if (!party.isLeader(leader)) return false
        if (!party.isMember(target)) return false

        return leaveParty(target)
    }

    fun getParty(player: UUID): PartyData? {
        val partyId = playerParties[player] ?: return null
        return parties[partyId]
    }

    fun getAllParties(): List<PartyData> {
        return parties.values.toList()
    }

    fun isInSameParty(player1: UUID, player2: UUID): Boolean {
        val party1 = playerParties[player1]
        val party2 = playerParties[player2]
        return party1 != null && party1 == party2
    }

    fun updatePartyGlow(party: PartyData) {
        if (!plugin.config.getBoolean("party.highlight-enabled", true)) return

        party.members.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                player.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, Int.MAX_VALUE, 0, false, false))
            }
        }
    }

    fun removeGlow(player: Player) {
        player.removePotionEffect(PotionEffectType.GLOWING)
    }

    private fun getNextColor(): ChatColor {
        val color = partyColors[colorIndex % partyColors.size]
        colorIndex++
        return color
    }

    fun setMaxPartySize(size: Int) {
        maxPartySize = size
    }

    fun getMaxPartySize(): Int = maxPartySize

    fun saveAll() {
        // Save party data to config/database
    }

    fun reload() {
        maxPartySize = plugin.config.getInt("party.max-size", 4)
    }
}

// ==================== KIT MANAGER ====================
class KitManager(private val plugin: EventPlugin) {

    private val kits = mutableMapOf<String, ItemStack>()
    private val kitRoomPos1 = mutableMapOf<String, Location>()
    private val kitRoomPos2 = mutableMapOf<String, Location>()
    private val kitRoomWarp = mutableMapOf<String, Location>()

    fun createKit(name: String, items: Array<ItemStack>) {
        kits[name.lowercase()] = ItemStack(Material.CHEST)
    }

    fun deleteKit(name: String): Boolean {
        return kits.remove(name.lowercase()) != null
    }

    fun getKit(name: String): ItemStack? {
        return kits[name.lowercase()]
    }

    fun getAllKits(): Set<String> {
        return kits.keys
    }

    fun giveKit(player: Player, kitName: String): Boolean {
        val kit = kits[kitName.lowercase()] ?: return false

        val data = plugin.statsManager.getPlayerData(player.uniqueId)
        if (!data.canUseKit()) {
            return false
        }

        if (plugin.config.getBoolean("kits.only-in-kitroom", true)) {
            if (!isInKitRoom(player.location)) {
                return false
            }
        }

        player.inventory.clear()

        val cooldown = if (data.isSpectator) {
            plugin.config.getInt("kits.cooldown-spectator", 0)
        } else {
            plugin.config.getInt("kits.cooldown-alive", 45)
        }

        data.setKitCooldown(cooldown)
        return true
    }

    // Force give kit - bypasses cooldown and location checks (for auto-rekit)
    fun giveKitForce(player: Player, kitName: String): Boolean {
        val kit = kits[kitName.lowercase()] ?: return false

        player.inventory.clear()
        // TODO: Actually give kit items when kit system is fully implemented

        return true
    }
    fun setKitRoomPos1(world: String, location: Location) {
        kitRoomPos1[world] = location
    }

    fun setKitRoomPos2(world: String, location: Location) {
        kitRoomPos2[world] = location
    }

    fun setKitRoomWarp(world: String, location: Location) {
        kitRoomWarp[world] = location
    }

    fun getKitRoomWarp(world: String): Location? {
        return kitRoomWarp[world]
    }

    fun isInKitRoom(location: Location): Boolean {
        val world = location.world?.name ?: return false
        val pos1 = kitRoomPos1[world] ?: return false
        val pos2 = kitRoomPos2[world] ?: return false

        val minX = minOf(pos1.x, pos2.x)
        val maxX = maxOf(pos1.x, pos2.x)
        val minY = minOf(pos1.y, pos2.y)
        val maxY = maxOf(pos1.y, pos2.y)
        val minZ = minOf(pos1.z, pos2.z)
        val maxZ = maxOf(pos1.z, pos2.z)

        return location.x in minX..maxX &&
                location.y in minY..maxY &&
                location.z in minZ..maxZ
    }

    fun isKitRoomAccessible(): Boolean {
        if (!plugin.eventManager.isEventActive()) return true

        val gracePeriod = plugin.eventManager.getGracePeriodRemaining()
        return gracePeriod > 0
    }

    fun reload() {
        // Reload kit settings
    }
}

// ==================== SURGE MANAGER ====================
class SurgeManager(private val plugin: EventPlugin) {

    private var enabled = true
    private var requiredPops = 3
    private var damage = 1.0
    private var interval = 3
    private var surgeTask: BukkitTask? = null

    fun start() {
        if (!enabled || surgeTask != null) return

        surgeTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            if (!plugin.eventManager.isEventActive()) return@Runnable

            Bukkit.getOnlinePlayers().forEach { player ->
                val data = plugin.statsManager.getPlayerData(player.uniqueId)

                if (!data.isAlive || data.isSpectator) return@forEach

                if (data.surgePops < requiredPops) {
                    player.damage(damage)
                    player.sendActionBar("§c§lSURGE DAMAGE! Get ${requiredPops - data.surgePops} more pops!")
                }
            }
        }, 0L, (interval * 20L))
    }

    fun stop() {
        surgeTask?.cancel()
        surgeTask = null
    }

    fun toggle(): Boolean {
        enabled = !enabled
        if (enabled) start() else stop()
        return enabled
    }

    fun setRequiredPops(amount: Int) {
        requiredPops = amount
    }

    fun getRequiredPops(): Int = requiredPops

    fun isEnabled(): Boolean = enabled

    fun reload() {
        enabled = plugin.config.getBoolean("surge.enabled", true)
        requiredPops = plugin.config.getInt("surge.required-pops", 3)
        damage = plugin.config.getDouble("surge.damage", 1.0)
        interval = plugin.config.getInt("surge.interval", 3)
    }
}

// ==================== BORDER MANAGER (FIXED!) ====================
class BorderManager(private val plugin: EventPlugin) {

    private var currentPhase = 0
    private var shrinkTask: BukkitTask? = null
    private var isPaused = false
    private val phaseSizes = listOf(150, 125, 100, 75, 60, 40, 20, 10, 7, 5, 3)

    fun shrinkToPhase(phase: Int, seconds: Int) {
        if (phase < 1 || phase > 11) return

        val world = Bukkit.getWorld(plugin.config.getString("world.event-world-name", "world") ?: "world") ?: return
        val border = world.worldBorder

        val size = phaseSizes[phase - 1].toDouble() * 2 // World border uses diameter

        val msg = (plugin.config.getString("messages.border-shrinking") ?: "")
            .replace("{size}", phaseSizes[phase - 1].toString()) // Show radius in message
            .replace("{time}", seconds.toString())
        Bukkit.broadcastMessage(plugin.config.getString("messages.prefix") + msg)

        border.setSize(size, seconds.toLong())
        currentPhase = phase

        Bukkit.getOnlinePlayers().forEach {
            it.playSound(it.location, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.5f)
        }
    }

    fun startAutomaticShrinking() {
        if (shrinkTask != null) {
            shrinkTask?.cancel()
        }

        var phase = 1

        // FIXED: Use runTaskLater chaining instead of repeating task
        fun scheduleNextPhase() {
            if (phase > 11 || isPaused || !plugin.eventManager.isEventActive()) {
                shrinkTask?.cancel()
                shrinkTask = null
                return
            }

            val time = plugin.config.getInt("border.phases.$phase.default-time", 60)
            shrinkToPhase(phase, time)

            phase++

            // Schedule next phase after this one completes + 2 second buffer
            shrinkTask = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                scheduleNextPhase()
            }, (time * 20L) + 40L)
        }

        scheduleNextPhase()
    }

    fun pauseShrinking() {
        isPaused = true
        shrinkTask?.cancel()
    }

    fun resumeShrinking() {
        isPaused = false
        if (plugin.eventManager.isEventActive() && currentPhase < 11) {
            startAutomaticShrinking()
        }
    }

    fun resetBorder() {
        shrinkTask?.cancel()
        shrinkTask = null
        currentPhase = 0
        isPaused = false

        val world = Bukkit.getWorld(plugin.config.getString("world.event-world-name", "world") ?: "world") ?: return
        val border = world.worldBorder

        border.reset()
        border.size = 300.0 // Reset to initial size (diameter)
    }

    fun setBorderCenter(location: Location) {
        location.world?.worldBorder?.center = location
    }

    fun getCurrentPhase(): Int = currentPhase

    fun reload() {
        // Reload border settings
    }
}

// Extension function for sendActionBar
fun Player.sendActionBar(message: String) {
    this.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent(message))
}