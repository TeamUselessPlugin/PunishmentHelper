package io.github.teamuselessplugin.punishment

class CommandHandler {
    fun register() {
        val commands = listOf(
            io.github.teamuselessplugin.punishment.commands.Punishment(),
            io.github.teamuselessplugin.punishment.commands.StopTrackingPlayer()
        )

        commands.forEach { it.push() }
    }
}