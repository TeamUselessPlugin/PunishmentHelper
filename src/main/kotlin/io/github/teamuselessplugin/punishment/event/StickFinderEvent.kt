package io.github.teamuselessplugin.punishment.event

import io.github.monun.heartbeat.coroutines.HeartbeatScope
import io.github.teamuselessplugin.punishment.packet.GlowPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import java.util.*

internal class StickFinderEvent : Listener {
    companion object {
        val seeker: HashMap<UUID, Boolean> = HashMap()
        val target: HashMap<UUID, Player> = HashMap()
        val coroutineEnabled: HashMap<UUID, Boolean> = HashMap()
    }

    @EventHandler
    fun interaction(e: PlayerInteractEvent) {
        if (e.player.hasPermission("punishment.stick")) {
            if (e.action.isRightClick && e.item?.type == Material.STICK && !e.player.isSneaking && target[e.player.uniqueId] != null) {
                e.player.performCommand("punishment ${target[e.player.uniqueId]?.name}")
            }

            if (e.player.isSneaking && e.action.isRightClick && e.item?.type == Material.STICK) {
                if (seeker[e.player.uniqueId] == true) {
                    // 플레이어 추적 비활성화
                    seeker[e.player.uniqueId] = false
                    e.player.playSound(e.player.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)

                    e.player.sendMessage("§c플레이어 추적기가 비활성화 되었습니다.")
                } else {
                    // 플레이어 추적 활성화
                    seeker[e.player.uniqueId] = true
                    e.player.playSound(e.player.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)

                    if (coroutineEnabled[e.player.uniqueId] == null || !coroutineEnabled[e.player.uniqueId]!!) {
                        HeartbeatScope().launch {
                            coroutineEnabled[e.player.uniqueId] = true
                            val glow = GlowPlayer().apply { addWatcher(e.player) }
                            while (seeker[e.player.uniqueId] == true) {
                                if (e.player.inventory.itemInMainHand.type == Material.STICK) {
                                    if (BlockEvents.tracking[e.player.uniqueId] == null || BlockEvents.tracking[e.player.uniqueId] == false) {
                                        var text = Component.text("§f감지된 유저 : ${target[e.player.uniqueId]?.name ?: "없음"}")
                                        if (target[e.player.uniqueId]?.name != null) {
                                            text = text.append(Component.text(" (§c오른쪽 버튼을 클릭하여 처벌 GUI 열기§f)"))
                                        }

                                        e.player.sendActionBar(text)

                                        val maxDistance = 100
                                        val players: MutableList<Player> = mutableListOf()
                                        e.player.getTargetEntity(maxDistance)?.let {
                                            if (it is Player && it != e.player) {
                                                players.add(it)
                                            }
                                        }

                                        if (target[e.player.uniqueId] != null) {
                                            glow.hide()
                                            target.remove(e.player.uniqueId)
                                        }
                                        if (players.size > 0) {
                                            target[e.player.uniqueId] = players[0]
                                            glow.setTarget(players[0]).also { glow.show() }
                                        }
                                    } else {
                                        glow.hide()
                                    }
                                } else {
                                    glow.hide()
                                    target.remove(e.player.uniqueId)
                                }
                                delay(50L)
                            }
                            coroutineEnabled[e.player.uniqueId] = false
                            if (target[e.player.uniqueId] != null) {
                                glow.hide()
                                target.remove(e.player.uniqueId)
                            }
                        }
                        e.player.sendMessage("§a플레이어 추적기가 활성화 되었습니다.")
                    }
                }
            }
        }
    }
}