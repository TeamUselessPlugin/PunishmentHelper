package io.github.teamuselessplugin.punishment.command

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import io.github.monun.invfx.openFrame
import io.github.teamuselessplugin.punishment.event.BlockEvents
import io.github.teamuselessplugin.punishment.`interface`.Command
import io.github.teamuselessplugin.punishment.invfx.PunishmentGUI
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