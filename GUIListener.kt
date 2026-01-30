package com.eventplugin.listeners

import com.eventplugin.EventPlugin
import com.eventplugin.gui.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class GUIListener(private val plugin: EventPlugin) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val title = event.view.title

        when {
            title.contains("Event Control Panel") -> {
                event.isCancelled = true
                EventGUI(plugin).handleClick(event)
            }
            title.contains("Event Settings") -> {
                event.isCancelled = true
                SettingsGUI(plugin).handleClick(event)
            }
            title.contains("Kit Selection") -> {
                event.isCancelled = true
                KitGUI(plugin).handleClick(event, player)
            }
            title.contains("Your Statistics") -> {
                event.isCancelled = true
                StatsGUI(plugin).handleClick(event)
            }
            title.contains("Player List") -> {
                event.isCancelled = true
                PlayerListGUI(plugin).handleClick(event)
            }
            title.contains("Spectator Menu") -> {
                event.isCancelled = true
                SpectatorCompassGUI(plugin).handleClick(event)
            }
        }
    }
}