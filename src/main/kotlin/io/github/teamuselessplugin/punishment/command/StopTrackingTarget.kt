package io.github.teamuselessplugin.punishment.command

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import io.github.monun.invfx.openFrame
import io.github.teamuselessplugin.punishment.Main
import io.github.teamuselessplugin.punishment.`interface`.Command
import io.github.teamuselessplugin.punishment.invfx.PunishmentGUI
import io.github.teamuselessplugin.punishment.invfx.SinglePlayer
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Sound

internal class StopTrackingTarget : Command {
    override val commandName: String
        get() = "punishment-tracking-end"

    override fun push() {
        CommandAPICommand(commandName)
            .withAliases("추적종료")
            .withPermission("punishment.gui")
            .executesPlayer(PlayerCommandExecutor { sender, _ ->

                // 추적 중인 플레이어가 없는지 확인
                if (PunishmentGUI.tracking[sender.uniqueId] == false || PunishmentGUI.trackingPlayer[sender.uniqueId] == null) {
                    return@PlayerCommandExecutor sender.sendMessage("추적 중인 플레이어가 없습니다.")
                }

                val playerOffline = Bukkit.getOfflinePlayer(PunishmentGUI.trackingPlayer[sender.uniqueId]!!)

                PunishmentGUI.tracking[sender.uniqueId] = false
                sender.sendMessage(Component.text("§a${playerOffline.name}님에 대한 추적이 해제되었습니다."))

                sender.teleport(PunishmentGUI.oldLocation[sender.uniqueId]!!)
                sender.gameMode = PunishmentGUI.oldGameMode[sender.uniqueId]!!
                sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)

                PunishmentGUI.oldLocation.remove(sender.uniqueId)
                PunishmentGUI.oldGameMode.remove(sender.uniqueId)
                PunishmentGUI.trackingPlayer.remove(sender.uniqueId)

                if (Main.liteBans_enable) {
                    sender.openFrame(SinglePlayer().liteBans(sender))
                } else {
                    sender.openFrame(SinglePlayer().vanilla(sender))
                }
            }).register()
    }
}