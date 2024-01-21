package io.github.teamuselessplugin.punishment

import io.github.classgraph.ClassGraph
import io.github.teamuselessplugin.punishment.interfaces.Command

class CommandHandler {
    @Suppress("UnstableApiUsage")
    fun register() {
        val scanResult = ClassGraph()
            .acceptPackages(Main.instance!!.pluginMeta.mainClass.substringBeforeLast('.'))
            .enableClassInfo()
            .scan()

        scanResult
            .getClassesImplementing(Command::class.qualifiedName)
            .forEach {
                val command = it.loadClass(Command::class.java).getDeclaredConstructor().newInstance()
                command.push()
            }
    }
}