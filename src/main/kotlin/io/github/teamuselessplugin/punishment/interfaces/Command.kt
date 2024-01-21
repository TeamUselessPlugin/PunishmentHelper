package io.github.teamuselessplugin.punishment.interfaces

interface Command {
    val commandName: String
    fun push()
}