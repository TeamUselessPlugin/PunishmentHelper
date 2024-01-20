package io.github.teamuselessplugin.punishment.commands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.OfflinePlayerArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import io.github.monun.invfx.openFrame
import io.github.teamuselessplugin.punishment.interfaces.Command
import io.github.teamuselessplugin.punishment.invfx.PunishmentGUI
import org.bukkit.OfflinePlayer

class Punishment : Command {
    override fun push() {
        CommandAPICommand("punishment")
            .withAliases("처벌")
            .withPermission("punishment.gui")
            .withArguments(OfflinePlayerArgument("player"))
            .executesPlayer(PlayerCommandExecutor { sender, args ->
                val player = args[0] as OfflinePlayer
                if (sender.uniqueId != player.uniqueId) {
                    if (player.isOnline && !player.player?.hasPermission("punishment.bypass")!!) {
                        PunishmentGUI().main(sender, player.uniqueId)?.let { sender.openFrame(it) }
                    } else if (player.isOnline && player.player?.hasPermission("punishment.bypass")!!) {
                        sender.sendMessage("해당 플레이어에 대한 처벌 GUI를 활성화 할 수 없습니다.")
                    } else {
                        PunishmentGUI().main(sender, player.uniqueId)?.let { sender.openFrame(it) }
                    }
                } else {
                    sender.sendMessage("자기 자신에게는 처벌 GUI를 활성화 할 수 없습니다.")
                }
            }).register()
    }
}