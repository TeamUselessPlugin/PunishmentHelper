package io.github.teamuselessplugin.punishment.invfx

import io.github.monun.heartbeat.coroutines.HeartbeatScope
import io.github.monun.invfx.InvFX.frame
import io.github.monun.invfx.frame.InvFrame
import io.github.monun.invfx.openFrame
import io.github.teamuselessplugin.punishment.Main
import io.github.teamuselessplugin.punishment.event.BlockEvents
import io.github.teamuselessplugin.punishment.packet.GlowPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.*

internal class SinglePlayer {
    fun vanilla(sender: Player, targetPlayer: OfflinePlayer): InvFrame {
        val isOnline: Boolean = targetPlayer.isOnline
        val targetUUID: UUID = targetPlayer.uniqueId
        val targetPlayerOnline: Player? = if (targetPlayer.isOnline) targetPlayer.player else null
        val gui = PunishmentGUI()

        return frame(6, Component.text("Punishment GUI")) {
            val isBanned = targetPlayer.isBanned

            // Player Head
            slot(4, 0) {
                item = ItemStack(Material.PLAYER_HEAD).apply {

                    itemMeta = itemMeta.apply {
                        (this as SkullMeta).owningPlayer = targetPlayer

                        if (!isBanned && isOnline) {
                            displayName(
                                Component.text("${targetPlayer.name} [${targetPlayer.uniqueId}]")
                                    .color(TextColor.color(Color.WHITE.asRGB()))
                            )

                        } else if (isBanned) {
                            displayName(
                                Component.text("${targetPlayer.name} [${targetPlayer.uniqueId}]")
                                    .decorate(TextDecoration.STRIKETHROUGH)
                                    .color(TextColor.color(Color.GRAY.asRGB()))
                            )

                            lore(
                                listOf(
                                    Component.text("§c이미 서버에서 차단된 플레이어입니다.")
                                )
                            )
                        } else {
                            displayName(
                                Component.text("${targetPlayer.name} [${targetPlayer.uniqueId}]")
                                    .color(TextColor.color(Color.GRAY.asRGB()))
                            )

                            lore(
                                listOf(
                                    Component.text("§c오프라인 플레이어입니다.")
                                )
                            )
                        }
                    }
                }
            }

            /* Admin Buttons */
            // Ban / Unban Button
            slot(2, 2) {
                if (!isBanned) {
                    item = ItemStack(Material.BARRIER).apply {
                        itemMeta = itemMeta.apply {
                            displayName(Component.text("§c차단"))
                            lore(listOf(Component.text("§7플레이어를 서버에서 차단합니다.")))
                        }
                    }

                    onClick {
                        sender.openFrame(gui.template(sender, listOf(targetUUID), PunishmentGUI.PunishmentType.BAN, PunishmentGUI.PluginType.VANILLA))
                    }
                } else {
                    item = ItemStack(Material.BARRIER).apply {
                        itemMeta = itemMeta.apply {
                            displayName(Component.text("§c차단 해제"))
                            lore(listOf(Component.text("§7플레이어의 차단을 해제합니다.")))
                            addEnchant(Enchantment.DURABILITY, 1, true)
                            addItemFlags(ItemFlag.HIDE_ENCHANTS)
                        }
                    }

                    onClick {
                        sender.performCommand("minecraft:pardon ${targetPlayer.name}")
                        sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
                        sender.sendMessage(Component.text("§a${targetPlayer.name}님의 차단이 해제되었습니다."))
                        sender.openFrame(gui.main(sender, listOf(targetUUID))!!)
                    }
                }
            }

            // Kick Button
            slot(6, 2) {
                item = ItemStack(Material.IRON_DOOR).apply {
                    itemMeta = itemMeta.apply {
                        displayName(Component.text("§c추방"))
                        lore(listOf(Component.text("§7플레이어를 서버에서 추방합니다.")))
                    }
                }

                onClick {
                    sender.openFrame(gui.template(sender, listOf(targetUUID), PunishmentGUI.PunishmentType.KICK, PunishmentGUI.PluginType.VANILLA))
                }
            }

            // Tracking Player Button
            slot(1, 4) {
                if (BlockEvents.tracking[sender.uniqueId] == true) {
                    item = ItemStack(Material.COMPASS).apply {
                        itemMeta = itemMeta.apply {
                            displayName(Component.text("§c플레이어 추적 §a(설정됨)"))
                            lore(listOf(Component.text("§7플레이어를 추적합니다.")))
                            addEnchant(Enchantment.DURABILITY, 1, true)
                            addItemFlags(ItemFlag.HIDE_ENCHANTS)
                        }
                    }
                } else {
                    item = ItemStack(Material.COMPASS).apply {
                        itemMeta = itemMeta.apply {
                            displayName(Component.text("§c플레이어 추적"))
                            lore(listOf(Component.text("§7플레이어를 추적합니다.")))
                        }
                    }
                }

                onClick {
                    if (BlockEvents.tracking[sender.uniqueId] == true) {
                        sender.performCommand("punishment-tracking-end")
                    } else {
                        if (isOnline) {
                            BlockEvents.tracking[sender.uniqueId] = true
                            sender.sendMessage(Component.text("§a${targetPlayer.name}님에 대한 추적이 설정되었습니다."))
                            sender.sendMessage(Component.text("§a추적을 종료하려면 ")
                                .append(Component.text("/추적종료")
                                    .clickEvent(ClickEvent.runCommand("/추적종료"))
                                    .hoverEvent(HoverEvent.showText(Component.text("§7클릭하여 추적을 종료합니다."))))
                                .append(Component.text("§a를 입력하세요.")))

                            BlockEvents.oldLoc[sender.uniqueId] = sender.location
                            BlockEvents.oldGameMode[sender.uniqueId] = sender.gameMode
                            BlockEvents.trackingPlayer[sender.uniqueId] = targetPlayer.uniqueId

                            sender.gameMode = GameMode.SPECTATOR
                            sender.spectatorTarget = targetPlayerOnline
                            sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
                            sender.closeInventory()

                            HeartbeatScope().launch {
                                val glow = GlowPlayer(targetPlayerOnline).apply { addWatcher(sender) }
                                while (BlockEvents.tracking[sender.uniqueId] == true) {
                                    if (sender.location.distance(targetPlayerOnline?.location!!) > Main.conf?.getInt("trackingDistance")!!) {
                                        sender.teleport(targetPlayerOnline.location)
                                    }
                                    glow.show()
                                    sender.sendActionBar(Component.text("§f${targetPlayer.name}님의 위치: X : §c${targetPlayerOnline.location.blockX}§f, Y : §c${targetPlayerOnline.location.blockY}§f, Z : §c${targetPlayerOnline.location.blockZ} §f[거리 : §c${Math.round(sender.location.distance(targetPlayerOnline.location))}m§f]"))
                                    delay(50L)
                                }
                                glow.hide()
                            }
                        } else {
                            sender.sendMessage(gui.errorIsOfflinePlayer)
                        }
                    }
                }
            }

            // Block Movement Button
            slot(3, 4) {
                if (BlockEvents.blocker[targetPlayer.uniqueId] == true) {
                    item = ItemStack(Material.STRUCTURE_VOID).apply {
                        itemMeta = itemMeta.apply {
                            displayName(Component.text("§c이동 제한 & 상호작용 제한 §a(설정됨)"))
                            lore(listOf(Component.text("§7플레이어의 이동과 상호작용을 제한합니다.")))
                            addEnchant(Enchantment.DURABILITY, 1, true)
                            addItemFlags(ItemFlag.HIDE_ENCHANTS)
                        }
                    }
                } else {
                    item = ItemStack(Material.STRUCTURE_VOID).apply {
                        itemMeta = itemMeta.apply {
                            displayName(Component.text("§c이동 제한 & 상호작용 제한"))
                            lore(listOf(Component.text("§7플레이어의 이동과 상호작용을 제한합니다.")))
                        }
                    }
                }

                onClick {
                    if (isOnline) {
                        sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
                        if (BlockEvents.blocker[targetPlayer.uniqueId] == true) {
                            BlockEvents.blocker[targetPlayer.uniqueId] = false
                            sender.sendMessage(Component.text("§a${targetPlayer.name}님의 이동 제한 상태가 해제되었습니다."))
                            sender.openFrame(gui.main(sender, listOf(targetUUID))!!)
                        } else {
                            BlockEvents.blocker[targetPlayer.uniqueId] = true
                            sender.sendMessage(Component.text("§a${targetPlayer.name}님의 이동 제한 상태가 설정되었습니다."))
                            sender.openFrame(gui.main(sender, listOf(targetUUID))!!)
                        }
                    } else {
                        sender.sendMessage(gui.errorIsOfflinePlayer)
                    }
                }
            }

            // Inventory See Button
            slot(5, 4) {
                item = ItemStack(Material.CHEST).apply {
                    itemMeta = itemMeta.apply {
                        displayName(Component.text("§c인벤토리 열람 §7(미구현)"))
                        lore(listOf(Component.text("§7플레이어의 인벤토리를 열람합니다.")))
                    }
                }

                onClick {
                    // TODO: Inventory
                    sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.1f)
                    sender.sendMessage(Component.text("§c아직 구현되지 않았습니다."))
                }
            }

            // EnderChest See Button
            slot(7, 4) {
                item = ItemStack(Material.ENDER_CHEST).apply {
                    itemMeta = itemMeta.apply {
                        displayName(Component.text("§c엔더 상자 열람 §7(미구현)"))
                        lore(listOf(Component.text("§7플레이어의 엔더 상자를 열람합니다.")))
                    }
                }

                onClick {
                    // TODO: EnderChest
                    sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.1f)
                    sender.sendMessage(Component.text("§c아직 구현되지 않았습니다."))
                }
            }
            /* Admin Buttons */
        }
    }
}