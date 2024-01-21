package io.github.teamuselessplugin.punishment.invfx

import io.github.monun.heartbeat.coroutines.HeartbeatScope
import io.github.monun.invfx.InvFX.frame
import io.github.monun.invfx.frame.InvFrame
import io.github.monun.invfx.openFrame
import io.github.teamuselessplugin.punishment.Main
import io.github.teamuselessplugin.punishment.events.BlockEvents
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

class PunishmentGUI {
    private val errorByServer = Component.text("§c데이터를 가져올 수 없습니다. 관리자에게 문의해주세요.")
    private val errorByPlayer = Component.text("§c플레이어 데이터를 가져올 수 없습니다.")
    private val errorIsOfflinePlayer = Component.text("§c오프라인 플레이어입니다.")
    private val tooManyPlayers = Component.text("§c플레이어를 여러명 입력한 경우 해당 기능을 사용할 수 없습니다.")
    enum class PunishmentType {
        BAN, KICK, MUTE, WARN
    }
    enum class PluginType {
        LITEBANS, VANILLA
    }
    fun main(sender: Player, targetUUID: List<UUID>): InvFrame? {
        val targetPlayer: List<OfflinePlayer> = targetUUID.map { Bukkit.getOfflinePlayer(it) }
        val isValid: List<Boolean> = targetPlayer.map { it.hasPlayedBefore() || it.isOnline }
        val isOnline: List<Boolean> = targetPlayer.map { it.isOnline }
        val targetPlayerOnline: List<Player?> = targetPlayer.map { if (it.isOnline) it.player else null }

        if (!isValid.contains(false)) {
            try {
                var isBanned: List<Boolean>

                var a: InvFrame? = null
                // LiteBans가 활성화 되어있을 경우
                if (Main.liteBans_enable) {
                    // TODO 다시 만들기
                } else {
                    // LiteBans가 비활성화 되어있을 경우
                    a = frame(if (targetPlayer.size == 1) 6 else 4, Component.text("Punishment GUI")) {
                        isBanned = targetPlayer.map { it.isBanned }

                        // Player Head
                        slot(4, 0) {
                            item = ItemStack(Material.PLAYER_HEAD).apply {

                                itemMeta = itemMeta.apply {
                                    if (targetPlayer.size == 1) {
                                        (this as SkullMeta).owningPlayer = targetPlayer[0]

                                        displayName(
                                            Component.text("${targetPlayer[0].name} [${targetPlayer[0].uniqueId}]")
                                                .color(TextColor.color(Color.WHITE.asRGB()))
                                        )
                                    } else {
                                        displayName(
                                            Component.text("다중 플레이어 선택됨")
                                                .color(TextColor.color(Color.WHITE.asRGB()))
                                        )

                                        val l: MutableList<Component> = mutableListOf()
                                        l.add(Component.text("§c플레이어를 여러명 입력한 경우 어드민 유틸 기능을 사용할 수 없습니다."))
                                        l.add(Component.text(""))
                                        targetPlayer.forEachIndexed { index, it ->
                                            if (!isBanned.contains(true) && isOnline.contains(true)) {
                                                l.add(Component.text("${it.name} [${it.uniqueId}]")
                                                    .color(TextColor.color(Color.WHITE.asRGB())))
                                            } else if (isBanned.contains(true)) {
                                                l.add(Component.text("${it.name} [${it.uniqueId}]")
                                                    .decorate(TextDecoration.STRIKETHROUGH)
                                                    .color(TextColor.color(Color.GRAY.asRGB())))
                                                l.add(Component.text("§c이미 서버에서 차단된 플레이어입니다."))
                                            } else {
                                                l.add(Component.text("${it.name} [${it.uniqueId}]")
                                                    .color(TextColor.color(Color.GRAY.asRGB())))
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
                        }

                        /* Admin Buttons */
                        if (targetPlayer.size == 1) {
                            // Ban Button
                            slot(2, 2) {
                                if (!isBanned[0]) {
                                    item = ItemStack(Material.BARRIER).apply {
                                        itemMeta = itemMeta.apply {
                                            displayName(Component.text("§c차단"))
                                            lore(listOf(Component.text("§7플레이어를 서버에서 차단합니다.")))
                                        }
                                    }

                                    onClick {
                                        sender.openFrame(template(sender, targetUUID, PunishmentType.BAN, PluginType.VANILLA))
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
                                        sender.performCommand("minecraft:pardon ${targetPlayer[0].name}")
                                        sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
                                        sender.sendMessage(Component.text("§a${targetPlayer[0].name}님의 차단이 해제되었습니다."))
                                        sender.openFrame(main(sender, targetUUID)!!)
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
                                    sender.openFrame(template(sender, targetUUID, PunishmentType.KICK, PluginType.VANILLA))
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
                                        if (isOnline[0]) {
                                            BlockEvents.tracking[sender.uniqueId] = true
                                            sender.sendMessage(Component.text("§a${targetPlayer[0].name}님에 대한 추적이 설정되었습니다."))
                                            sender.sendMessage(Component.text("§a추적을 종료하려면 ")
                                                .append(Component.text("/추적종료")
                                                    .clickEvent(ClickEvent.runCommand("/추적종료"))
                                                    .hoverEvent(HoverEvent.showText(Component.text("§7클릭하여 추적을 종료합니다."))))
                                                .append(Component.text("§a를 입력하세요.")))

                                            BlockEvents.oldLoc[sender.uniqueId] = sender.location
                                            BlockEvents.oldGameMode[sender.uniqueId] = sender.gameMode
                                            BlockEvents.trackingPlayer[sender.uniqueId] = targetPlayer[0].uniqueId

                                            sender.gameMode = GameMode.SPECTATOR
                                            sender.teleport(targetPlayerOnline[0]?.location!!)
                                            sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
                                            sender.closeInventory()

                                            HeartbeatScope().launch {
                                                val glow = GlowPlayer(targetPlayerOnline[0]).apply { addWatcher(sender) }
                                                while (BlockEvents.tracking[sender.uniqueId] == true) {
                                                    if (sender.location.distance(targetPlayerOnline[0]?.location!!) > Main.conf?.getInt("trackingDistance")!!) {
                                                        sender.teleport(targetPlayerOnline[0]?.location!!)
                                                    }
                                                    glow.show()
                                                    sender.sendActionBar(Component.text("§f${targetPlayerOnline[0]?.name!!}님의 위치: X : §c${targetPlayerOnline[0]?.location?.blockX}§f, Y : §c${targetPlayerOnline[0]?.location?.blockY}§f, Z : §c${targetPlayerOnline[0]?.location?.blockZ} §f[거리 : §c${Math.round(sender.location.distance(targetPlayerOnline[0]?.location!!))}m§f]"))
                                                    delay(50L)
                                                }
                                                glow.hide()
                                            }
                                        } else {
                                            sender.sendMessage(errorIsOfflinePlayer)
                                        }
                                    }
                                }
                            }

                            // Block Movement Button
                            slot(3, 4) {
                                if (BlockEvents.blocker[targetPlayer[0].uniqueId] == true) {
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
                                    if (isOnline[0]) {
                                        sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
                                        if (BlockEvents.blocker[targetPlayer[0].uniqueId] == true) {
                                            BlockEvents.blocker[targetPlayer[0].uniqueId] = false
                                            sender.sendMessage(Component.text("§a${targetPlayer[0].name}님의 이동 제한 상태가 해제되었습니다."))
                                            sender.openFrame(main(sender, targetUUID)!!)
                                        } else {
                                            BlockEvents.blocker[targetPlayer[0].uniqueId] = true
                                            sender.sendMessage(Component.text("§a${targetPlayer[0].name}님의 이동 제한 상태가 설정되었습니다."))
                                            sender.openFrame(main(sender, targetUUID)!!)
                                        }
                                    } else {
                                        sender.sendMessage(errorIsOfflinePlayer)
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
                        } else {
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
                                        template(
                                            sender,
                                            targetUUID,
                                            PunishmentType.BAN,
                                            PluginType.VANILLA
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
                                        template(
                                            sender,
                                            targetUUID,
                                            PunishmentType.KICK,
                                            PluginType.VANILLA
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
                                    targetPlayer.forEach {
                                        if (it.isBanned) {
                                            sender.performCommand("minecraft:pardon ${it.name}")
                                        }
                                    }
                                    sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
                                    sender.sendMessage(Component.text("§a선택한 플레이어들의 차단이 해제되었습니다."))
                                    sender.closeInventory()
                                }
                            }
                        }
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

    private fun template(sender: Player, targetUUID: List<UUID>, punishmentType: PunishmentType, pluginType: PluginType): InvFrame {
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