package io.github.teamuselessplugin.punishment

import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import io.github.teamuselessplugin.punishment.`interface`.Command

internal class CommandHandler {
    @Suppress("UnstableApiUsage")
    companion object {
        val scanResult: ScanResult = ClassGraph()
            .acceptPackages(Main.instance!!.pluginMeta.mainClass.substringBeforeLast('.'))
            .enableClassInfo()
            .scan()
    }

    fun register() {
        scanResult
            .getClassesImplementing(Command::class.qualifiedName)
            .forEach {
                val command = it.loadClass(Command::class.java).getDeclaredConstructor().newInstance()
                command.push()
            }
    }

    fun commandsName(): List<String> {
        return scanResult
            .getClassesImplementing(Command::class.qualifiedName)
            .map {
                it.loadClass(Command::class.java).getDeclaredConstructor().newInstance().commandName
            }
    }
}