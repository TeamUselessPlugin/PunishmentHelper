package io.github.teamuselessplugin.punishment.command

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ListArgumentBuilder
import dev.jorel.commandapi.executors.PlayerCommandExecutor
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
            .withArguments(ListArgumentBuilder<OfflinePlayer>("offline_players")
                .withList(Supplier {
                    mutableListOf<OfflinePlayer>().apply {
                        Bukkit.getOnlinePlayers().forEach { add(it) }
                        Bukkit.getOfflinePlayers().forEach { if (!contains(it)) add(it) }
                    }
                }).withMapper { it.name /* 최적화 이슈 */ }.buildGreedy())
            .executesPlayer(PlayerCommandExecutor { sender, args ->
                val player = (args[0] as List<OfflinePlayer>).sortedBy { it.name }

                // 명령어 실행 대상에 자기 자신이 포함되어 있는지 확인
                if (player.contains(sender)) {
                    return@PlayerCommandExecutor sender.sendMessage("자기 자신에게는 처벌 GUI를 활성화 할 수 없습니다.")
                }

                val hasPermission: MutableList<OfflinePlayer> = mutableListOf<OfflinePlayer>().apply {
                    player.forEach {
                        val isOnline = it.isOnline

                        if (isOnline && it.player?.hasPermission("punishment.bypass")!!) {
                            add(it)
                        }
                    }
                }

                // 펄미션 확인
                if (hasPermission.isEmpty()){
                    PunishmentGUI().main(sender, player)
                } else {
                    sender.sendMessage("선택한 플레이어중 처벌 GUI 우회 권한을 가진 플레이어가 있습니다. : ${hasPermission.map { it.name }}")
                }
            }).register()
    }
}