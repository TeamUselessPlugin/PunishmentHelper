package io.github.teamuselessplugin.punishment.invfx

import io.github.monun.invfx.InvFX.frame
import io.github.monun.invfx.frame.InvFrame
import io.github.monun.invfx.openFrame
import io.github.teamuselessplugin.punishment.Main
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import java.util.*

internal class PunishmentGUI {
    val errorByServer = Component.text("§c데이터를 가져올 수 없습니다. 관리자에게 문의해주세요.")
    val errorByPlayer = Component.text("§c플레이어 데이터를 가져올 수 없습니다.")
    val errorIsOfflinePlayer = Component.text("§c오프라인 플레이어입니다.")
    enum class PunishmentType {
        BAN, KICK, MUTE, WARN
    }
    enum class PluginType {
        LITEBANS, VANILLA
    }
    fun main(sender: Player, targetUUID: List<UUID>): InvFrame? {
        val targetPlayers: List<OfflinePlayer> = targetUUID.map { Bukkit.getOfflinePlayer(it) }
        val isValid: List<Boolean> = targetPlayers.map { it.hasPlayedBefore() || it.isOnline }

        if (!isValid.contains(false)) {
            try {
                var a: InvFrame? = null
                if (targetPlayers.size > 1) {
                    // 플레이어가 여러명일 경우
                    if (Main.liteBans_enable) {
                        // LiteBans가 활성화 되어있을 경우
                        // TODO 다시 만들기
                    } else {
                        // LiteBans가 비활성화 되어있을 경우
                        a = MultiplePlayers().vanilla(sender, targetPlayers)
                    }
                } else {
                    // 플레이어가 한명일 경우
                    if (Main.liteBans_enable) {
                        // LiteBans가 활성화 되어있을 경우
                        // TODO 다시 만들기
                    } else {
                        // LiteBans가 비활성화 되어있을 경우
                        a = SinglePlayer().vanilla(sender, targetPlayers[0])
                    }
                }
                return a
            } catch (e: IllegalStateException) {
                sender.sendMessage(errorByServer)
                e.printStackTrace()
            }
            return null
        } else {
            sender.sendMessage(errorByPlayer)
            return null
        }
    }

    internal fun template(sender: Player, targetUUID: List<UUID>, punishmentType: PunishmentType, pluginType: PluginType): InvFrame {
        val templateKeys = Main.template?.getConfigurationSection("templates")?.getKeys(false)?.toList()

        return frame((templateKeys?.size?.div(9) ?: 0) + 1, Component.text("Select Punishment Template")) {
            templateKeys?.forEachIndexed { index, it ->
                val material = Main.template?.getString("templates.$it.ItemStack.Material")?.uppercase()?.replace(" ", "_") ?: "PAPER"

                slot(index % 9, index / 9) {
                    item = ItemStack(Material.valueOf(material)).apply {
                        itemMeta = itemMeta.apply {
                            displayName(Component.text(Main.template?.getString("templates.$it.ItemStack.displayName").toString()
                                .replace("&", "§")))
                            lore(Main.template?.getStringList("templates.$it.ItemStack.lore")?.map { lore ->
                                Component.text(lore.replace("&", "§"))
                            })

                            if (Main.template?.getBoolean("templates.$it.ItemStack.glowing") == true) {
                                addEnchant(Enchantment.DURABILITY, 1, true)
                                addItemFlags(ItemFlag.HIDE_ENCHANTS)
                            }
                        }
                    }

                    onClick { _ ->
                        sender.openFrame(duration(sender, targetUUID, punishmentType, pluginType, it.toString())!!)
                    }
                }
            }
        }
    }

    private fun duration(sender: Player, targetUUID: List<UUID>, punishmentType: PunishmentType, pluginType: PluginType, template: String): InvFrame? {
        when (pluginType) {
            PluginType.VANILLA -> {
                punishment(sender, targetUUID, punishmentType, pluginType, template, null)
            }

            PluginType.LITEBANS -> {
                if (punishmentType == PunishmentType.BAN || punishmentType == PunishmentType.MUTE) {
                    val dateKeys = Main.template?.getConfigurationSection("durations")?.getKeys(false)?.toList()

                    return frame((dateKeys?.size?.div(9) ?: 0) + 1, Component.text("Select Punishment Duration")) {
                        dateKeys?.forEachIndexed { index, it ->
                            slot(index % 9, index / 9) {
                                item = ItemStack(Material.PAPER).apply {
                                    itemMeta = itemMeta.apply {
                                        displayName(Component.text(Main.template?.getString("durations.$it.displayName").toString()
                                            .replace("&", "§")))
                                    }
                                }

                                onClick { _ ->
                                    punishment(sender, targetUUID, punishmentType, pluginType, template, it.toString())
                                }
                            }
                        }
                    }
                } else {
                    punishment(sender, targetUUID, punishmentType, pluginType, template, null)
                }
            }
        }
        return null
    }

    private fun punishment(sender: Player, targetUUID: List<UUID>, punishmentType: PunishmentType, pluginType: PluginType, template: String, punishmentDuration: String?) {
        val playerOfflineList: List<OfflinePlayer> = targetUUID.map { sender.server.getOfflinePlayer(it) }

        val reason = Main.template?.getString("templates.$template.Reason")?.replace("&", "§")
        val duration = if (punishmentDuration != null) Main.template?.getInt("durations.$punishmentDuration") else -1

        sender.closeInventory()

        playerOfflineList.forEach {
            val isOnline = it.isOnline

            when (pluginType) {
                PluginType.LITEBANS -> {
                    when(punishmentType) {
                        PunishmentType.BAN -> TODO()
                        PunishmentType.KICK -> TODO()
                        PunishmentType.MUTE -> TODO()
                        PunishmentType.WARN -> TODO()
                    }
                }
                PluginType.VANILLA -> {
                    when(punishmentType) {
                        PunishmentType.BAN -> {
                            sender.performCommand("minecraft:ban ${it.name} $reason")
                            sender.sendMessage(Component.text("§a${it.name}님을 서버에서 차단하였습니다."))
                        }
                        PunishmentType.KICK -> {
                            if (isOnline) {
                                sender.performCommand("minecraft:kick ${it.name} $reason")
                                sender.sendMessage(Component.text("§a${it.name}님을 서버에서 추방하였습니다."))
                            } else {
                                sender.sendMessage(errorIsOfflinePlayer)
                            }
                        }
                        else -> {
                            sender.sendMessage(Component.text("§c해당 기능은 'LiteBans' 플러그인이 있을 때만 사용할 수 있습니다."))
                        }
                    }
                }
            }
        }
        sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
    }
}