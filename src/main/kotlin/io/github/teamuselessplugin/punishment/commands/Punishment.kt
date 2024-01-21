package io.github.teamuselessplugin.punishment.commands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ListArgumentBuilder
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import io.github.monun.invfx.openFrame
import io.github.teamuselessplugin.punishment.interfaces.Command
import io.github.teamuselessplugin.punishment.invfx.PunishmentGUI
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer


class Punishment : Command {
    override fun push() {
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
    }
}