package io.github.teamuselessplugin.punishment.event

import io.github.teamuselessplugin.punishment.invfx.PunishmentGUI
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

internal class Events : Listener {
    @EventHandler
    fun logout(e: PlayerQuitEvent) {
        if (StickFinderEvent.seeker[e.player.uniqueId] == true) {
            StickFinderEvent.seeker.remove(e.player.uniqueId)
        }

        if (PunishmentGUI.tracking[e.player.uniqueId] == true) {
            e.player.performCommand("punishment-tracking-end")
        }
    }
}