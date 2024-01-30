package io.github.teamuselessplugin.punishment.`interface`

internal interface Command {
    val commandName: String
    fun push()
}