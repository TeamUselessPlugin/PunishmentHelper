package io.github.teamuselessplugin.punishment.command

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ListArgumentBuilder
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import io.github.monun.invfx.openFrame
import io.github.teamuselessplugin.punishment.`interface`.Command
import io.github.teamuselessplugin.punishment.invfx.PunishmentGUI
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.function.Supplier


internal class Punishment : Command {
    override val commandName: String
        get() = "punishment"

    @Suppress("UNCHECKED_CAST")
    override fun push() {

        CommandAPICommand(commandName)
            .withAliases("처벌")
            .withPermission("punishment.gui")
            .withArguments(ListArgumentBuilder<String>("offline_players")
                .withList(Supplier {
                    mutableListOf<String>().apply {
                        Bukkit.getOnlinePlayers().forEach { add(it.name) }
                        Bukkit.getOfflinePlayers().forEach { if (!contains(it.name)) add(it.name.toString()) }
                    }
                }).withMapper { it }.buildGreedy())
            .executesPlayer(PlayerCommandExecutor { sender, args ->
                val player = mutableListOf<OfflinePlayer>().apply {
                    (args[0] as List<String>).forEach { name ->
                        add(Bukkit.getOfflinePlayer(name))
                    }
                }

                // 명령어 실행 대상에 자기 자신이 포함되어 있는지 확인
                if (player.contains(sender)) {
                    return@PlayerCommandExecutor sender.sendMessage("자기 자신에게는 처벌 GUI를 활성화 할 수 없습니다.")
                }

                // 펄미션 확인
                if (!player.map { it.isOnline }.contains(false) && !player.map { it.player?.hasPermission("punishment.bypass")!! }.contains(true)) {
                    PunishmentGUI().main(sender, player.map { it.uniqueId })?.let {
                        sender.openFrame(it)
                    }
                } else if (!player.map { it.isOnline }.contains(false) && player.map { it.player?.hasPermission("punishment.bypass")!! }.contains(true)) {
                    sender.sendMessage("해당 플레이어에 대한 처벌 GUI를 활성화 할 수 없습니다.")
                } else {
                    PunishmentGUI().main(sender, player.map { it.uniqueId })?.let {
                        sender.openFrame(it)
                    }
                }
            }).register()
    }
}