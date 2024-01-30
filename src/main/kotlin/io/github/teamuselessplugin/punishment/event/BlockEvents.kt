package io.github.teamuselessplugin.punishment.event

import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import java.util.*
import kotlin.collections.HashMap

internal class BlockEvents : Listener {
    companion object {
        val blocker: HashMap<UUID, Boolean> = HashMap()
    }
    @EventHandler
    fun interaction(e : PlayerInteractEvent) {
        if (blocker[e.player.uniqueId] == true) {
            e.isCancelled = true
            e.player.sendActionBar(Component.text("§c관리자에 의해 상호작용이 제한되었습니다."))
        }
    }

    @EventHandler
    fun movement(e : PlayerMoveEvent) {
        if (blocker[e.player.uniqueId] == true) {
            val loc = Location(e.player.world, e.player.location.x, e.player.location.y - 0.1, e.player.location.z)
            if (loc.block.type != Material.AIR) {
                e.isCancelled = true
                e.player.sendActionBar(Component.text("§c관리자에 의해 이동이 제한되었습니다."))
            }
        }
    }

    @EventHandler
    fun command(e : PlayerCommandPreprocessEvent) {
        if (blocker[e.player.uniqueId] == true) {
            e.isCancelled = true
            e.player.sendMessage(Component.text("§c관리자에 의해 명령어 사용이 제한되었습니다."))
        }
    }

    @EventHandler
    fun damage(e : EntityDamageByEntityEvent) {
        if (e.entity is Player) {
            if (blocker[e.entity.uniqueId] == true || blocker[e.damager.uniqueId] == true) {
                e.isCancelled = true
                e.damager.sendMessage(Component.text("§c관리자에 의해 피해를 입힐 수 없습니다."))
            }
        }
    }

    @EventHandler
    fun death(e : PlayerDeathEvent) {
        if (blocker[e.entity.uniqueId] == true) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun dropItem(e : PlayerDropItemEvent) {
        if (blocker[e.player.uniqueId] == true) {
            e.isCancelled = true
            e.player.sendActionBar(Component.text("§c관리자에 의해 아이템 드롭이 제한되었습니다."))
        }
    }

    @EventHandler
    fun inventoryOpen(e : InventoryOpenEvent) {
        if (blocker[e.player.uniqueId] == true) {
            e.player.closeInventory()
            e.player.sendActionBar(Component.text("§c관리자에 의해 인벤토리 열람이 제한되었습니다."))
        }
    }

    @EventHandler
    fun inventory(e : InventoryClickEvent) {
        if (blocker[e.whoClicked.uniqueId] == true) {
            e.isCancelled = true
            e.whoClicked.sendActionBar(Component.text("§c관리자에 의해 인벤토리 사용이 제한되었습니다."))
        }
    }
}