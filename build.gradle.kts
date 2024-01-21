import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val name: String by project
val version: String by project
val targetPaperAPI: String by project

plugins {
    kotlin("jvm") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    /* Essentials Dependency */
    compileOnly(kotlin("stdlib")) // Kotlin Standard Library : Apache-2.0 License
    compileOnly(kotlin("reflect")) // Kotlin Reflection : Apache-2.0 License
    compileOnly("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.7.3") // Kotlin Coroutines : Apache-2.0 License
    /* Essentials Dependency */

    compileOnly(fileTree(mapOf("dir" to "libs/compileOnly", "include" to listOf("*.jar")))) // Load all jars in libs folder (Local Libraries)
    compileOnly("io.papermc.paper", "paper-api", "${targetPaperAPI}-R0.1-SNAPSHOT") // PaperMC API : MIT License
    compileOnly("dev.jorel", "commandapi-bukkit-core", "9.3.0") // CommandAPI Dev Only : MIT License
    compileOnly("com.gitlab.ruany", "LiteBansAPI", "0.4.1") // LiteBans API : MIT License
    compileOnly("io.github.monun", "heartbeat-coroutines", "0.0.5") // Heartbeat Coroutine : GPL-3.0 License
    compileOnly("io.github.monun", "invfx-api", "3.3.2") // InvFX API : GPL-3.0 License
    compileOnly("com.comphenix.protocol", "ProtocolLib", "5.2.0-20231209.220838-1") // ProtocolLib : GPL-2.0 License
    compileOnly("io.github.classgraph", "classgraph", "4.8.165")

    implementation(fileTree(mapOf("dir" to "libs/implementation", "include" to listOf("*.jar")))) // Load all jars in libs folder (Local Libraries)
}

tasks {
    processResources {
        filteringCharset = "UTF-8"

        filesMatching("**/*.yml") {
            expand(project.properties)
        }
    }

    project.delete(
        file("build/resources")
    )

    register<ShadowJar>("paperJar") {
        archiveBaseName.set(project.name)
        archiveVersion.set(version)
        archiveClassifier.set("")
        from(sourceSets["main"].output, "LICENSE", "README.MD")

        configurations = listOf(project.configurations.runtimeClasspath.get()) // Add all dependencies to the jar

        doLast {
            copy {
                val archiveFileName = "${project.name}-dev.jar"

                from(archiveFile)
                rename { archiveFileName }

                val newPluginFileLocation = File("\\\\192.168.123.107\\Users\\User\\Desktop\\DEV\\plugins") // DevServer
//                val newPluginFileLocation = File(rootDir, ".dev/plugins") // Local

                if (File(newPluginFileLocation, archiveFileName).exists()) {
                    into(File(newPluginFileLocation, "update"))
                    File(newPluginFileLocation, "update/RELOAD").delete()
                } else {
                    into(newPluginFileLocation)
                }
            }
        }
    }
}