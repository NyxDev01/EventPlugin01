package com.eventplugin.utils

import com.eventplugin.EventPlugin
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.io.File
import java.util.concurrent.TimeUnit

// ==================== EXTENSION FUNCTIONS ====================
fun FileConfiguration.getStringSafe(path: String, default: String = ""): String {
    return getString(path) ?: default
}

fun CommandSender.sendMessageSafe(message: String?) {
    message?.let { sendMessage(it) }
}

// ==================== CONFIG UTIL ====================
object ConfigUtil {

    fun saveDefaultConfig(plugin: EventPlugin) {
        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdirs()
        }

        val configFile = File(plugin.dataFolder, "config.yml")
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false)
        }
    }

    fun loadConfig(plugin: EventPlugin, fileName: String): FileConfiguration {
        val file = File(plugin.dataFolder, fileName)
        if (!file.exists()) {
            plugin.saveResource(fileName, false)
        }
        return YamlConfiguration.loadConfiguration(file)
    }

    fun saveConfig(plugin: EventPlugin, config: FileConfiguration, fileName: String) {
        val file = File(plugin.dataFolder, fileName)
        try {
            config.save(file)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to save $fileName: ${e.message}")
        }
    }

    fun reloadConfig(plugin: EventPlugin): FileConfiguration {
        val file = File(plugin.dataFolder, "config.yml")
        return YamlConfiguration.loadConfiguration(file)
    }

    fun getString(config: FileConfiguration, path: String, default: String = ""): String {
        return config.getString(path) ?: default
    }

    fun getInt(config: FileConfiguration, path: String, default: Int = 0): Int {
        return config.getInt(path, default)
    }

    fun getBoolean(config: FileConfiguration, path: String, default: Boolean = false): Boolean {
        return config.getBoolean(path, default)
    }

    fun getDouble(config: FileConfiguration, path: String, default: Double = 0.0): Double {
        return config.getDouble(path, default)
    }

    fun getStringList(config: FileConfiguration, path: String): List<String> {
        return config.getStringList(path)
    }
}

// ==================== MESSAGE UTIL ====================
object MessageUtil {

    private var prefix = "§6§l[Event] §r"

    fun setPrefix(newPrefix: String) {
        prefix = colorize(newPrefix)
    }

    fun colorize(message: String): String {
        return ChatColor.translateAlternateColorCodes('&', message)
    }

    fun send(player: org.bukkit.entity.Player, message: String) {
        player.sendMessage(prefix + colorize(message))
    }

    fun sendRaw(player: org.bukkit.entity.Player, message: String) {
        player.sendMessage(colorize(message))
    }

    fun broadcast(message: String) {
        Bukkit.broadcastMessage(prefix + colorize(message))
    }

    fun broadcastRaw(message: String) {
        Bukkit.broadcastMessage(colorize(message))
    }

    fun sendTitle(player: org.bukkit.entity.Player, title: String, subtitle: String, fadeIn: Int = 10, stay: Int = 70, fadeOut: Int = 20) {
        player.sendTitle(colorize(title), colorize(subtitle), fadeIn, stay, fadeOut)
    }

    fun sendActionBar(player: org.bukkit.entity.Player, message: String) {
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent(colorize(message)))
    }

    fun broadcastTitle(title: String, subtitle: String, fadeIn: Int = 10, stay: Int = 70, fadeOut: Int = 20) {
        Bukkit.getOnlinePlayers().forEach { player ->
            player.sendTitle(colorize(title), colorize(subtitle), fadeIn, stay, fadeOut)
        }
    }

    fun sendPermissionError(player: org.bukkit.entity.Player) {
        send(player, "§cYou don't have permission to do that!")
    }

    fun sendPlayerNotFound(player: org.bukkit.entity.Player, targetName: String) {
        send(player, "§cPlayer '$targetName' not found!")
    }

    fun sendUsage(player: org.bukkit.entity.Player, usage: String) {
        send(player, "§cUsage: $usage")
    }

    fun replaceVariables(message: String, variables: Map<String, String>): String {
        var result = message
        variables.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }
        return result
    }

    fun formatList(items: List<String>, prefix: String = "§7- §f"): String {
        return items.joinToString("\n") { "$prefix$it" }
    }
}

// ==================== ITEM BUILDER ====================
class ItemBuilder(private var material: Material) {

    private var amount: Int = 1
    private var displayName: String? = null
    private var lore: MutableList<String> = mutableListOf()
    private var enchantments: MutableMap<org.bukkit.enchantments.Enchantment, Int> = mutableMapOf()
    private var itemFlags: MutableList<org.bukkit.inventory.ItemFlag> = mutableListOf()
    private var unbreakable: Boolean = false
    private var customModelData: Int? = null
    private var glowing: Boolean = false

    fun amount(amount: Int): ItemBuilder {
        this.amount = amount
        return this
    }

    fun name(name: String): ItemBuilder {
        this.displayName = MessageUtil.colorize(name)
        return this
    }

    fun lore(vararg lines: String): ItemBuilder {
        lines.forEach { lore.add(MessageUtil.colorize(it)) }
        return this
    }

    fun lore(lines: List<String>): ItemBuilder {
        lines.forEach { lore.add(MessageUtil.colorize(it)) }
        return this
    }

    fun enchant(enchantment: org.bukkit.enchantments.Enchantment, level: Int): ItemBuilder {
        enchantments[enchantment] = level
        return this
    }

    fun flag(vararg flags: org.bukkit.inventory.ItemFlag): ItemBuilder {
        itemFlags.addAll(flags)
        return this
    }

    fun unbreakable(unbreakable: Boolean): ItemBuilder {
        this.unbreakable = unbreakable
        return this
    }

    fun customModelData(data: Int): ItemBuilder {
        this.customModelData = data
        return this
    }

    fun glow(glow: Boolean): ItemBuilder {
        this.glowing = glow
        if (glow) {
            enchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1)
            flag(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
        }
        return this
    }

    fun build(): ItemStack {
        val item = ItemStack(material, amount)
        val meta = item.itemMeta ?: return item

        displayName?.let { meta.setDisplayName(it) }
        if (lore.isNotEmpty()) meta.lore = lore
        enchantments.forEach { (enchant, level) ->
            meta.addEnchant(enchant, level, true)
        }
        itemFlags.forEach { meta.addItemFlags(it) }
        meta.isUnbreakable = unbreakable
        customModelData?.let { meta.setCustomModelData(it) }

        item.itemMeta = meta
        return item
    }

    companion object {
        fun from(material: Material): ItemBuilder {
            return ItemBuilder(material)
        }

        fun skull(playerName: String): ItemBuilder {
            val item = ItemBuilder(Material.PLAYER_HEAD)
            // Set skull owner if needed
            return item
        }
    }
}

// ==================== LOCATION UTIL ====================
object LocationUtil {

    fun serialize(location: Location): String {
        return "${location.world?.name},${location.x},${location.y},${location.z},${location.yaw},${location.pitch}"
    }

    fun deserialize(string: String): Location? {
        val parts = string.split(",")
        if (parts.size < 4) return null

        val world = Bukkit.getWorld(parts[0]) ?: return null
        val x = parts[1].toDoubleOrNull() ?: return null
        val y = parts[2].toDoubleOrNull() ?: return null
        val z = parts[3].toDoubleOrNull() ?: return null
        val yaw = parts.getOrNull(4)?.toFloatOrNull() ?: 0f
        val pitch = parts.getOrNull(5)?.toFloatOrNull() ?: 0f

        return Location(world, x, y, z, yaw, pitch)
    }

    fun center(location: Location): Location {
        val centered = location.clone()
        centered.x = location.blockX + 0.5
        centered.y = location.blockY.toDouble()
        centered.z = location.blockZ + 0.5
        return centered
    }

    fun distance2D(loc1: Location, loc2: Location): Double {
        val dx = loc1.x - loc2.x
        val dz = loc1.z - loc2.z
        return kotlin.math.sqrt(dx * dx + dz * dz)
    }

    fun isInside(location: Location, pos1: Location, pos2: Location): Boolean {
        if (location.world != pos1.world) return false

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

    fun getRandomLocation(center: Location, radius: Double): Location {
        val random = java.util.Random()
        val angle = random.nextDouble() * 2 * Math.PI
        val distance = random.nextDouble() * radius

        val x = center.x + distance * kotlin.math.cos(angle)
        val z = center.z + distance * kotlin.math.sin(angle)

        val world = center.world ?: return center
        val y = world.getHighestBlockYAt(x.toInt(), z.toInt()).toDouble() + 1

        return Location(world, x, y, z)
    }

    fun format(location: Location): String {
        return "§7World: §f${location.world?.name} §7X: §f${location.blockX} §7Y: §f${location.blockY} §7Z: §f${location.blockZ}"
    }
}

// ==================== TIME UTIL ====================
object TimeUtil {

    fun formatTime(seconds: Long): String {
        if (seconds <= 0) return "0s"

        val hours = TimeUnit.SECONDS.toHours(seconds)
        val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
        val secs = seconds % 60

        return buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (secs > 0 || (hours == 0L && minutes == 0L)) append("${secs}s")
        }.trim()
    }

    fun formatTimeShort(seconds: Long): String {
        if (seconds <= 0) return "0s"

        val minutes = seconds / 60
        val secs = seconds % 60

        return if (minutes > 0) {
            "${minutes}m ${secs}s"
        } else {
            "${secs}s"
        }
    }

    fun formatCountdown(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    fun parseTime(input: String): Long? {
        val regex = "(\\d+)([smhd])".toRegex()
        val matches = regex.findAll(input.lowercase())

        var totalSeconds = 0L
        for (match in matches) {
            val value = match.groupValues[1].toLongOrNull() ?: continue
            val unit = match.groupValues[2]

            totalSeconds += when (unit) {
                "s" -> value
                "m" -> value * 60
                "h" -> value * 3600
                "d" -> value * 86400
                else -> 0
            }
        }

        return if (totalSeconds > 0) totalSeconds else null
    }

    fun getTimestamp(): Long {
        return System.currentTimeMillis()
    }

    fun getTimeSince(timestamp: Long): Long {
        return (System.currentTimeMillis() - timestamp) / 1000
    }

    fun formatDate(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return format.format(date)
    }

    fun getRemainingTime(endTime: Long): Long {
        return maxOf(0, (endTime - System.currentTimeMillis()) / 1000)
    }
}