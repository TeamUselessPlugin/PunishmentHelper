package io.github.teamuselessplugin.punishment.commands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ListArgumentBuilder
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import io.github.monun.invfx.openFrame
import io.github.teamuselessplugin.punishment.events.BlockEvents
import io.github.teamuselessplugin.punishment.invfx.PunishmentGUI
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.Sound


class Punishment {
    fun register() {

        val offlinePlayers = ListArgumentBuilder<OfflinePlayer>("offline_players")
            .withList(Bukkit.getOfflinePlayers().map { it }.toList())
            .withMapper { it.name }.buildGreedy()

        CommandAPICommand("punishment")
            .withAliases("처벌")
            .withPermission("punishment.gui")
            // TODO 여러명 선택 가능하게 하기
            .withArguments(offlinePlayers)
            .executesPlayer(PlayerCommandExecutor { sender, args ->
                @Suppress("UNCHECKED_CAST") val player = args[0] as List<OfflinePlayer>
                if (!player.contains(sender)) {
                    if (!player.map { it.isOnline }.contains(false) && !player.map { it.player?.hasPermission("punishment.bypass")!! }.contains(true)) {
                        PunishmentGUI().main(sender, player.map { it.uniqueId })?.let { sender.openFrame(it) }
                    } else if (!player.map { it.isOnline }.contains(false) && player.map { it.player?.hasPermission("punishment.bypass")!! }.contains(true)) {
                        sender.sendMessage("해당 플레이어에 대한 처벌 GUI를 활성화 할 수 없습니다.")
                    } else {
                        PunishmentGUI().main(sender, player.map { it.uniqueId })?.let { sender.openFrame(it) }
                    }
                } else {
                    sender.sendMessage("자기 자신에게는 처벌 GUI를 활성화 할 수 없습니다.")
                }
            }).register()

        CommandAPICommand("punishment-tracking-end")
            .withAliases("추적종료")
            .withPermission("punishment.gui")
            .executesPlayer(PlayerCommandExecutor { sender, _ ->
                if (BlockEvents.tracking[sender.uniqueId] == true) {
                    val playerOffline = Bukkit.getOfflinePlayer(BlockEvents.trackingPlayer[sender.uniqueId]!!)

                    BlockEvents.tracking[sender.uniqueId] = false
                    sender.sendMessage(Component.text("§a${playerOffline.name}님에 대한 추적이 해제되었습니다."))

                    sender.teleport(BlockEvents.oldLoc[sender.uniqueId]!!)
                    sender.gameMode = BlockEvents.oldGameMode[sender.uniqueId]!!
                    sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)

                    BlockEvents.oldLoc.remove(sender.uniqueId)
                    BlockEvents.oldGameMode.remove(sender.uniqueId)
                    BlockEvents.trackingPlayer.remove(sender.uniqueId)
                    sender.openFrame(PunishmentGUI().main(sender, listOf(playerOffline.uniqueId))!!)
                } else {
                    sender.sendMessage("추적 중이 아닙니다.")
                }
            }).register()
    }
}