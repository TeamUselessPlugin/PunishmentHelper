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

@Suppress("UNCHECKED_CAST")
internal class SinglePlayer {
    fun vanilla(sender: Player): InvFrame {
        val targetPlayer: List<OfflinePlayer> = (PunishmentGUI.data[sender.uniqueId] as MutableList<Any>)[0] as List<OfflinePlayer>
        val select: Int = (PunishmentGUI.data[sender.uniqueId] as MutableList<Any>)[2] as Int

        val isOnline: Boolean = targetPlayer[select].isOnline
        val targetUUID: UUID = targetPlayer[select].uniqueId
        val targetPlayerOnline: Player? = if (targetPlayer[select].isOnline) targetPlayer[select].player else null
        val gui = PunishmentGUI()

        return frame(6, Component.text("Punishment GUI")) {
            val isBanned = targetPlayer[select].isBanned

            // Player Head
            slot(4, 0) {
                item = ItemStack(Material.PLAYER_HEAD).apply {

                    itemMeta = itemMeta.apply {
                        (this as SkullMeta).owningPlayer = targetPlayer[select]

                        if (!isBanned && isOnline) {
                            displayName(
                                Component.text("${targetPlayer[select].name} [${targetPlayer[select].uniqueId}]")
                                    .color(TextColor.color(Color.WHITE.asRGB()))
                            )

                        } else if (isBanned) {
                            displayName(
                                Component.text("${targetPlayer[select].name} [${targetPlayer[select].uniqueId}]")
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
                                Component.text("${targetPlayer[select].name} [${targetPlayer[select].uniqueId}]")
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
                        val worlds = Bukkit.getWorlds()
                        val currentFeedback = worlds.map { world -> world.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK) }
                        worlds.forEach { world -> world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false) }
                        sender.performCommand("minecraft:pardon ${targetPlayer[select].name}")
                        worlds.forEachIndexed { index, world -> world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, currentFeedback[index]!!) }
                        sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
                        sender.sendMessage(Component.text("§a${targetPlayer[select].name}님의 차단이 해제되었습니다."))
                        sender.openFrame(vanilla(sender))
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
                if (PunishmentGUI.tracking[sender.uniqueId] == true) {
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
                    if (PunishmentGUI.tracking[sender.uniqueId] == true) {
                        sender.performCommand("punishment-tracking-end")
                    } else {
                        if (isOnline) {
                            PunishmentGUI.tracking[sender.uniqueId] = true
                            sender.sendMessage(Component.text("§a${targetPlayer[select].name}님에 대한 추적이 설정되었습니다."))
                            sender.sendMessage(Component.text("§a추적을 종료하려면 ")
                                .append(Component.text("/추적종료")
                                    .clickEvent(ClickEvent.runCommand("/추적종료"))
                                    .hoverEvent(HoverEvent.showText(Component.text("§7클릭하여 추적을 종료합니다."))))
                                .append(Component.text("§a를 입력하세요.")))

                            PunishmentGUI.oldLocation[sender.uniqueId] = sender.location
                            PunishmentGUI.oldGameMode[sender.uniqueId] = sender.gameMode
                            PunishmentGUI.trackingPlayer[sender.uniqueId] = targetPlayer[select].uniqueId

                            sender.gameMode = GameMode.SPECTATOR
                            sender.teleport(targetPlayerOnline?.location ?: sender.location)
                            sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
                            sender.closeInventory()

                            HeartbeatScope().launch {
                                val glow = GlowPlayer(targetPlayerOnline).apply { addWatcher(sender) }
                                while (PunishmentGUI.tracking[sender.uniqueId] == true) {
                                    if (sender.world != targetPlayerOnline?.world || sender.location.distance(targetPlayerOnline.location) > Main.conf?.getInt("trackingDistance")!!) {
                                        sender.teleport(targetPlayerOnline?.location!!)
                                    }
                                    glow.show()
                                    sender.sendActionBar(Component.text("§f${targetPlayer[select].name}님의 위치: X : §c${targetPlayerOnline.location.blockX}§f, Y : §c${targetPlayerOnline.location.blockY}§f, Z : §c${targetPlayerOnline.location.blockZ} §f[거리 : §c${Math.round(sender.location.distance(targetPlayerOnline.location))}m§f]"))
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
                if (BlockEvents.blocker[targetPlayer[select].uniqueId] == true) {
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
                        if (BlockEvents.blocker[targetPlayer[select].uniqueId] == true) {
                            BlockEvents.blocker[targetPlayer[select].uniqueId] = false
                            sender.sendMessage(Component.text("§a${targetPlayer[select].name}님의 이동 제한 상태가 해제되었습니다."))
                            sender.openFrame(gui.main(sender, listOf(targetUUID))!!)
                        } else {
                            BlockEvents.blocker[targetPlayer[select].uniqueId] = true
                            sender.sendMessage(Component.text("§a${targetPlayer[select].name}님의 이동 제한 상태가 설정되었습니다."))
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

            if (targetPlayer.size > 1) {
                // Back
                slot(0, 0) {
                    item = ItemStack(Material.ARROW).apply {
                        itemMeta = itemMeta.apply {
                            displayName(Component.text("§c뒤로"))
                            lore(listOf(Component.text("§7이전 페이지로 돌아갑니다.")))
                        }
                    }

                    onClick {
                        if (select == 0) {
                            sender.openFrame(MultiplePlayers().vanilla(sender))
                        } else {
                            (PunishmentGUI.data[sender.uniqueId] as MutableList<Any>)[2] = select - 1
                            sender.openFrame(vanilla(sender))
                        }
                    }
                }

                if (select != targetPlayer.size - 1) {
                    // Next
                    slot(8, 0) {
                        item = ItemStack(Material.ARROW).apply {
                            itemMeta = itemMeta.apply {
                                displayName(Component.text("§c다음"))
                                lore(listOf(Component.text("§7다음 페이지로 넘어갑니다.")))
                            }
                        }

                        onClick {
                            (PunishmentGUI.data[sender.uniqueId] as MutableList<Any>)[2] = select + 1
                            sender.openFrame(vanilla(sender))
                        }
                    }
                }
            }
        }
    }

    fun liteBans(sender: Player, select: Int = 0): InvFrame {
        val targetPlayer: List<OfflinePlayer> = (PunishmentGUI.data[sender.uniqueId] as List<Any>)[0] as List<OfflinePlayer>
        val collection: List<Any> = (PunishmentGUI.data[sender.uniqueId] as List<Any>)[1] as List<Any>

        return frame(1, Component.text("Hello")) {
            println(collection)
        }
    }
}