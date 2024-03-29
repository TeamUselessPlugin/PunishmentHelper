package io.github.teamuselessplugin.punishment

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import io.github.kill00.configapi.cfg
import io.github.teamuselessplugin.punishment.event.BlockEvents
import io.github.teamuselessplugin.punishment.event.Events
import io.github.teamuselessplugin.punishment.event.StickFinderEvent
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

internal class Main : JavaPlugin() {
    companion object {
        var conf: FileConfiguration? = null
        var template: FileConfiguration? = null
        var liteBans_enable: Boolean = false
        var instance: Main? = null
    }

    override fun onLoad() {
        CommandAPI.onLoad(CommandAPIBukkitConfig(this).silentLogs(true).verboseOutput(false))
    }
    override fun onEnable() {
        // 콘피그 세팅
        cfg.register(this)

        cfg.makeData("config.yml")
        conf = cfg.get("config.yml")!!

        cfg.makeData("template.yml")
        template = cfg.get("template.yml")!!

        // LiteBans 연동
        liteBans_enable = server.pluginManager.getPlugin("LiteBans") != null

        // 싱글톤
        instance = this

        // 커맨드 등록
        CommandHandler().register()

        // 이벤트 등록
        server.pluginManager.registerEvents(Events(), this)
        server.pluginManager.registerEvents(BlockEvents(), this)
        server.pluginManager.registerEvents(StickFinderEvent(), this)
    }
}