package io.github.teamuselessplugin.punishment.invfx

import io.github.monun.invfx.InvFX.frame
import io.github.monun.invfx.frame.InvFrame
import io.github.monun.invfx.openFrame
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import java.util.*

@Suppress("UNCHECKED_CAST")
internal class MultiplePlayers {
    fun vanilla(sender: Player): InvFrame {
        val targetPlayer: List<OfflinePlayer> = (PunishmentGUI.data[sender.uniqueId] as List<Any>)[0] as List<OfflinePlayer>
        val isOnline: MutableList<Boolean> = mutableListOf()
        val targetUUID: MutableList<UUID> = mutableListOf()
        val isBanned: MutableList<Boolean> = mutableListOf()

        targetPlayer.forEach {
            isOnline.add(it.isOnline)
            targetUUID.add(it.uniqueId)
            isBanned.add(it.isBanned)
        }

        return frame(4, Component.text("Punishment GUI")) {
            // Next
            slot(8, 0) {
                item = ItemStack(Material.ARROW).apply {
                    itemMeta = itemMeta.apply {
                        displayName(Component.text("§c다음"))
                        lore(listOf(Component.text("§7다음 페이지로 넘어갑니다.")))
                    }
                }

                onClick {
                    sender.openFrame(SinglePlayer().vanilla(sender))
                }
            }

            // Player Head
            slot(4, 0) {
                item = ItemStack(Material.PLAYER_HEAD).apply {

                    itemMeta = itemMeta.apply {
                        displayName(
                            Component.text("여러 플레이어 선택됨")
                                .color(TextColor.color(Color.WHITE.asRGB()))
                        )

                        val l: MutableList<Component> = mutableListOf()
                        l.add(Component.text(""))
                        targetPlayer.forEachIndexed { index, it ->
                            if (!isBanned[index] && isOnline[index]) {
                                l.add(
                                    Component.text("${it.name} [${it.uniqueId}]")
                                        .color(TextColor.color(Color.WHITE.asRGB()))
                                )
                            } else if (isBanned[index]) {
                                l.add(
                                    Component.text("${it.name} [${it.uniqueId}]")
                                        .decorate(TextDecoration.STRIKETHROUGH)
                                        .color(TextColor.color(Color.GRAY.asRGB()))
                                )
                                l.add(Component.text("§c이미 서버에서 차단된 플레이어입니다."))
                            } else {
                                l.add(
                                    Component.text("${it.name} [${it.uniqueId}]")
                                        .color(TextColor.color(Color.GRAY.asRGB()))
                                )
                                l.add(Component.text("§c오프라인 플레이어입니다."))
                            }
                            if (index != targetPlayer.size - 1) {
                                l.add(Component.text(""))
                            }
                        }

                        lore(l)
                    }
                }
            }

            /* Admin Buttons */
            // Ban Button
            slot(2, 2) {
                item = ItemStack(Material.BARRIER).apply {
                    itemMeta = itemMeta.apply {
                        displayName(Component.text("§c차단"))
                        lore(listOf(Component.text("§7선택한 플레이어 전체를 서버에서 차단합니다.")))
                    }
                }

                onClick {
                    sender.openFrame(
                        PunishmentGUI().template(
                            sender,
                            targetUUID,
                            PunishmentGUI.PunishmentType.BAN,
                            PunishmentGUI.PluginType.VANILLA
                        )
                    )
                }
            }

            // Kick Button
            slot(4, 2) {
                item = ItemStack(Material.IRON_DOOR).apply {
                    itemMeta = itemMeta.apply {
                        displayName(Component.text("§c추방"))
                        lore(listOf(Component.text("§7선택한 플레이어 전체를 서버에서 추방합니다.")))
                    }
                }

                onClick {
                    sender.openFrame(
                        PunishmentGUI().template(
                            sender,
                            targetUUID,
                            PunishmentGUI.PunishmentType.KICK,
                            PunishmentGUI.PluginType.VANILLA
                        )
                    )
                }
            }

            // Unban Button
            slot(6, 2) {
                item = ItemStack(Material.BARRIER).apply {
                    itemMeta = itemMeta.apply {
                        displayName(Component.text("§c차단 해제"))
                        lore(listOf(Component.text("§7선택한 플레이어 전체를 서버에서 차단 해제합니다.")))
                        addEnchant(Enchantment.DURABILITY, 1, true)
                        addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    }
                }

                onClick {
                    val worlds = Bukkit.getWorlds()
                    val currentFeedback = worlds.map { world -> world.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK) }
                    worlds.forEach { world -> world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false) }
                    targetPlayer.forEach {
                        if (it.isBanned) {
                            sender.performCommand("minecraft:pardon ${it.name}")
                        }
                    }
                    worlds.forEachIndexed { index, world -> world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, currentFeedback[index]!!) }

                    sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
                    sender.sendMessage(Component.text("§a선택한 플레이어들의 차단이 해제되었습니다."))
                    sender.closeInventory()
                }
            }
        }
    }

    fun liteBans(sender: Player): InvFrame {
        val targetPlayer: List<OfflinePlayer> = (PunishmentGUI.data[sender.uniqueId] as MutableList<Any>)[0] as List<OfflinePlayer>
        val collection: List<Any> = ((PunishmentGUI.data[sender.uniqueId] as MutableList<Any>)[1] as List<Any>?)!!
        val gui = PunishmentGUI()

        val isOnline: MutableList<Boolean> = mutableListOf()
        val targetUUID: MutableList<UUID> = mutableListOf()
        val isBanned: List<Boolean> = collection[0] as List<Boolean>
        val isMuted: List<Boolean> = collection[1] as List<Boolean>

        targetPlayer.forEach {
            isOnline.add(it.isOnline)
            targetUUID.add(it.uniqueId)
        }

        return frame(5, Component.text("Punishment GUI")) {
            // Next
            slot(8, 0) {
                item = ItemStack(Material.ARROW).apply {
                    itemMeta = itemMeta.apply {
                        displayName(Component.text("§c다음"))
                        lore(listOf(Component.text("§7다음 페이지로 넘어갑니다.")))
                    }
                }

                onClick {
                    sender.openFrame(SinglePlayer().liteBans(sender))
                }
            }

            // Player Head
            slot(4, 0) {
                item = ItemStack(Material.PLAYER_HEAD).apply {

                    itemMeta = itemMeta.apply {
                        displayName(
                            Component.text("여러 플레이어 선택됨")
                                .color(TextColor.color(Color.WHITE.asRGB()))
                        )

                        val l: MutableList<Component> = mutableListOf()
                        l.add(Component.text(""))
                        targetPlayer.forEachIndexed { index, it ->
                            if (!isBanned[index] && isOnline[index]) {
                                l.add(
                                    Component.text("${it.name} [${it.uniqueId}]")
                                        .color(TextColor.color(Color.WHITE.asRGB()))
                                )
                            } else if (isBanned[index]) {
                                l.add(
                                    Component.text("${it.name} [${it.uniqueId}]")
                                        .decorate(TextDecoration.STRIKETHROUGH)
                                        .color(TextColor.color(Color.GRAY.asRGB()))
                                )
                                l.add(Component.text("§c이미 서버에서 차단된 플레이어입니다."))
                            } else {
                                l.add(
                                    Component.text("${it.name} [${it.uniqueId}]")
                                        .color(TextColor.color(Color.GRAY.asRGB()))
                                )
                                l.add(Component.text("§c오프라인 플레이어입니다."))
                            }
                            if (index != targetPlayer.size - 1) {
                                l.add(Component.text(""))
                            }
                        }

                        lore(l)
                    }
                }
            }

            /* Admin Buttons */
            // Ban Button
            slot(1, 2) {
                item = ItemStack(Material.BARRIER).apply {
                    itemMeta = itemMeta.apply {
                        displayName(Component.text("§c차단"))
                        lore(listOf(Component.text("§7선택한 플레이어 전체를 서버에서 차단합니다.")))
                    }
                }

                onClick {
                    sender.openFrame(
                        gui.template(
                            sender,
                            targetUUID,
                            PunishmentGUI.PunishmentType.BAN,
                            PunishmentGUI.PluginType.LITEBANS
                        )
                    )
                }
            }

            // Unban Button
            slot(1, 3) {
                item = ItemStack(Material.BARRIER).apply {
                    itemMeta = itemMeta.apply {
                        displayName(Component.text("§c차단 해제"))
                        lore(listOf(Component.text("§7선택한 플레이어 전체를 서버에서 차단 해제합니다.")))
                        addEnchant(Enchantment.DURABILITY, 1, true)
                        addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    }
                }

                onClick {
                    targetPlayer.forEachIndexed { index, offlinePlayer ->
                        if (isBanned[index]) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "litebans:unban ${offlinePlayer.name} -S --sender-uuid=${sender.uniqueId} --sender=${sender.name}")
                        }
                    }
                    sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
                    sender.sendMessage(Component.text("§a선택한 플레이어들의 차단이 해제되었습니다."))
                    sender.closeInventory()
                }
            }

            // Kick Button
            slot(3, 2) {
                item = ItemStack(Material.IRON_DOOR).apply {
                    itemMeta = itemMeta.apply {
                        displayName(Component.text("§c추방"))
                        lore(listOf(Component.text("§7선택한 플레이어 전체를 서버에서 추방합니다.")))
                    }
                }

                onClick {
                    sender.openFrame(
                        gui.template(
                            sender,
                            targetUUID,
                            PunishmentGUI.PunishmentType.KICK,
                            PunishmentGUI.PluginType.LITEBANS
                        )
                    )
                }
            }

            // Mute Button
            slot(5, 2) {
                item = ItemStack(Material.BOOK).apply {
                    itemMeta = itemMeta.apply {
                        displayName(Component.text("§c채금"))
                        lore(listOf(Component.text("§7선택한 플레이어 전체를 서버에서 채금합니다.")))
                    }
                }

                onClick {
                    sender.openFrame(
                        gui.template(
                            sender,
                            targetUUID,
                            PunishmentGUI.PunishmentType.MUTE,
                            PunishmentGUI.PluginType.LITEBANS
                        )
                    )
                }
            }

            // Unmute Button
            slot(5, 3) {
                item = ItemStack(Material.BOOK).apply {
                    itemMeta = itemMeta.apply {
                        displayName(Component.text("§c채금 해제"))
                        lore(listOf(Component.text("§7선택한 플레이어 전체를 서버에서 채금 해제합니다.")))
                        addEnchant(Enchantment.DURABILITY, 1, true)
                        addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    }
                }

                onClick {
                    targetPlayer.forEachIndexed { index, offlinePlayer ->
                        if (isMuted[index]) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "litebans:unmute ${offlinePlayer.name} -S --sender-uuid=${sender.uniqueId} --sender=${sender.name}")
                        }
                    }
                    sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
                    sender.sendMessage(Component.text("§a선택한 플레이어들의 채금이 해제되었습니다."))
                    sender.closeInventory()
                }
            }

            // Warn Button
            slot(7, 2) {
                item = ItemStack(Material.PAPER).apply {
                    itemMeta = itemMeta.apply {
                        displayName(Component.text("§c경고"))
                        lore(listOf(Component.text("§7선택한 플레이어 전체에게 경고를 부여합니다.")))
                    }
                }

                onClick {
                    sender.openFrame(
                        gui.template(
                            sender,
                            targetUUID,
                            PunishmentGUI.PunishmentType.WARN,
                            PunishmentGUI.PluginType.LITEBANS
                        )
                    )
                }
            }
            /* Admin Buttons */
        }
    }
}