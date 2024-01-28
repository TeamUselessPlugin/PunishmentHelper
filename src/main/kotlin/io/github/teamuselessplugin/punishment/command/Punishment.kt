package io.github.teamuselessplugin.punishment.command

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ListArgumentBuilder
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import io.github.monun.invfx.openFrame
import io.github.teamuselessplugin.punishment.`interface`.Command
import io.github.teamuselessplugin.punishment.invfx.PunishmentGUI
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.UUID
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
                }).withMapper { it.name }.buildGreedy())
            .executesPlayer(PlayerCommandExecutor { sender, args ->
                val player = args[0] as List<OfflinePlayer>

                // 명령어 실행 대상에 자기 자신이 포함되어 있는지 확인
                if (player.contains(sender)) {
                    return@PlayerCommandExecutor sender.sendMessage("자기 자신에게는 처벌 GUI를 활성화 할 수 없습니다.")
                }

                val hasPermission: List<Boolean> = player.map {
                    val isOnline = it.isOnline

                    if (isOnline) {
                        it.player?.hasPermission("punishment.bypass")!!
                    } else {
                        false
                    }
                }
                val uuid: List<UUID> = player.map { it.uniqueId }

                // 펄미션 확인
                if (!hasPermission.contains(true)){
                    PunishmentGUI().main(sender, uuid)
                } else {
                    sender.sendMessage("해당 플레이어에 대한 처벌 GUI를 활성화 할 수 없습니다.")
                }
            }).register()
    }
}