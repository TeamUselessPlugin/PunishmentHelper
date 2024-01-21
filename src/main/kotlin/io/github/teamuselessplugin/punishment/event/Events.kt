package io.github.teamuselessplugin.punishment.event

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

internal class Events : Listener {
    @EventHandler
    fun logout(e: PlayerQuitEvent) {
        if (StickFinderEvent.seeker[e.player.uniqueId] == true) {
            StickFinderEvent.seeker.remove(e.player.uniqueId)
            StickFinderEvent.coroutineEnabled.remove(e.player.uniqueId)
            StickFinderEvent.target.remove(e.player.uniqueId)
        }

        if (BlockEvents.tracking[e.player.uniqueId] == true) {
            e.player.performCommand("punishment-tracking-end")
        }
    }
}