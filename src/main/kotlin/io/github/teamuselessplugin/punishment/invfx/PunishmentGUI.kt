package io.github.teamuselessplugin.punishment.invfx

import io.github.monun.heartbeat.coroutines.HeartbeatScope
import io.github.monun.invfx.InvFX.frame
import io.github.monun.invfx.frame.InvFrame
import io.github.monun.invfx.openFrame
import io.github.teamuselessplugin.punishment.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import litebans.api.Database
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.collections.HashMap

internal class PunishmentGUI {
    // 처벌 종류
    internal enum class PunishmentType {
        BAN, KICK, MUTE, WARN
    }

    // 플러그인 종류
    internal enum class PluginType {
        LITEBANS, VANILLA
    }

    companion object {
        /**
         * 플러그인에서 사용하는 데이터를 저장하는 변수입니다.
         *
         * @suppress `HashMap<UUID, Any>` 타입으로 선언되어 있지만, 실제로는 `HashMap<UUID, MutableList<Any>>` 타입으로 사용됩니다.
         * @return [[targetPlayers, liteBansDataCollection, currentPage]]
         * @see liteBansDataCollection
         */
        var data = HashMap<UUID, Any>()

        val tracking: HashMap<UUID, Boolean> = HashMap()
        val trackingPlayer: HashMap<UUID, UUID> = HashMap()
        val oldLocation: HashMap<UUID, Location> = HashMap()
        val oldGameMode: HashMap<UUID, GameMode> = HashMap()

        val errorByServer = Component.text("§c데이터를 가져올 수 없습니다. 관리자에게 문의해주세요.")
        val errorByPlayer = Component.text("§c플레이어 데이터를 가져올 수 없습니다.")
        val errorIsOfflinePlayer = Component.text("§c오프라인 플레이어입니다.")
    }

    /**
     * LiteBans 데이터를 저장하는 변수입니다.
     *
     * @suppress `List<Any>` 타입으로 선언되어 있지만, 실제로는 일부 제외 `List<List<Any>>` 타입으로 사용됩니다.
     * @return [[isBanned(Boolean), isMuted(List:Boolean), bannedCount(List:Long), kickedCount(List:Long), mutedCount(List:Long), warnedCount(List:Long), latestPunishmentData(HashMap:UUID, List)]]
     * @see latestPunishmentData
     */
    private var liteBansDataCollection: List<Any>? = null

    /**
     * LiteBans에서 가장 최근에 적용된 처벌 데이터를 저장하는 변수입니다.
     *
     * @return [[time(List:Long), reason(List:String), type(List:String), until(List:Long), active(List:Boolean), banned_by_uuid(List:String)]]
     */
    private val latestPunishmentData: HashMap<UUID, List<Any>> = HashMap()

    /**
     * 메인 GUI를 열어주는 함수입니다.
     *
     * @param sender 명령어를 실행한 플레이어
     * @param targetPlayers 명령어를 실행한 플레이어가 선택한 플레이어
     */
    fun main(sender: Player, targetPlayers: List<OfflinePlayer>) {
        val isValid: List<Boolean> = targetPlayers.map { it.hasPlayedBefore() || it.isOnline }

        // 선택된 플레이어 중 플레이어가 유효한 데이터를 가지고 있지 않을 경우
        if (isValid.contains(false)) {
            sender.sendMessage(errorByPlayer)
            return
        }

        try {
            var a: InvFrame? = null

            if (Main.liteBans_enable) {
                // LiteBans가 활성화 되어있을 경우
                liteBansDBUpdate(sender, targetPlayers)?.thenRun {
                    a = if (targetPlayers.size > 1) {
                        // 플레이어가 여러명일 경우
                        MultiplePlayers().liteBans(sender)
                    } else {
                        // 플레이어가 한명일 경우
                        SinglePlayer().liteBans(sender)
                    }
                }
            } else {
                data[sender.uniqueId] = mutableListOf(targetPlayers, null, 0)

                a = if (targetPlayers.size > 1) {
                    // 플레이어가 여러명일 경우
                    MultiplePlayers().vanilla(sender)
                } else {
                    // 플레이어가 한명일 경우
                    SinglePlayer().vanilla(sender)
                }
            }

            HeartbeatScope().launch {
                while (a == null) {
                    sender.sendActionBar(Component.text("데이터를 가져오는 중..."))
                    delay(50)
                }

                if (Main.liteBans_enable) sender.sendActionBar(Component.text("데이터를 가져왔습니다."))
                sender.openFrame(a!!)
            }
        } catch (e: IllegalStateException) {
            sender.sendMessage(errorByServer)
            e.printStackTrace()
        }
    }

    /**
     * LiteBans 데이터를 업데이트하는 함수입니다.
     *
     * Async로 실행되며, 실행이 완료되면 `data` 변수에 데이터를 저장합니다.
     *
     * @param sender 명령어를 실행한 플레이어
     * @param targetPlayers 명령어를 실행한 플레이어가 선택한 플레이어
     * @return CompletableFuture<Void>?
     */
    fun liteBansDBUpdate(sender: Player, targetPlayers: List<OfflinePlayer>): CompletableFuture<Void>? {
        if (latestPunishmentData.isNotEmpty() || liteBansDataCollection != null) {
            latestPunishmentData.clear()
            liteBansDataCollection = null
        }

        return CompletableFuture.runAsync {
            val db = Database.get()
            val isBanned = targetPlayers.map { db.isPlayerBanned(it.uniqueId, null) }
            val isMuted = targetPlayers.map { db.isPlayerMuted(it.uniqueId, null) }
            val bannedCount = targetPlayers.map {
                var count: Long
                db.prepareStatement("SELECT COUNT(*) FROM {bans} WHERE uuid=?").apply {
                    setString(1, it.uniqueId.toString())
                }.use { statement ->
                    statement.executeQuery().use { resultSet ->
                        count = if (resultSet.next()) {
                            resultSet.getLong(1)
                        } else {
                            0
                        }
                    }
                    statement.close()
                }

                if (count > 0) {
                    db.prepareStatement("SELECT * FROM {bans} WHERE uuid=? ORDER BY id DESC LIMIT 1").apply {
                        setString(1, it.uniqueId.toString())
                    }.use { statement ->
                        statement.executeQuery().use { resultSet ->
                            if (resultSet.next()) {
                                if (latestPunishmentData[it.uniqueId] == null || (latestPunishmentData[it.uniqueId]!![0] as Long) < resultSet.getLong("time")) {
                                    latestPunishmentData[it.uniqueId] = listOf(
                                        resultSet.getLong("time"),
                                        resultSet.getString("reason"),
                                        "차단",
                                        resultSet.getLong("until"),
                                        resultSet.getBoolean("active"),
                                        resultSet.getString("banned_by_uuid")
                                    )
                                }
                            }
                        }
                        statement.close()
                    }
                }
                count
            }
            val kickedCount = targetPlayers.map {
                var count: Long
                db.prepareStatement("SELECT COUNT(*) FROM {kicks} WHERE uuid=?").apply {
                    setString(1, it.uniqueId.toString())
                }.use { statement ->
                    statement.executeQuery().use { resultSet ->
                        count = if (resultSet.next()) {
                            resultSet.getLong(1)
                        } else {
                            0
                        }
                    }
                    statement.close()
                }

                if (count > 0) {
                    db.prepareStatement("SELECT * FROM {kicks} WHERE uuid=? ORDER BY id DESC LIMIT 1").apply {
                        setString(1, it.uniqueId.toString())
                    }.use { statement ->
                        statement.executeQuery().use { resultSet ->
                            if (resultSet.next()) {
                                if (latestPunishmentData[it.uniqueId] == null || (latestPunishmentData[it.uniqueId]!![0] as Long) < resultSet.getLong("time")) {
                                    latestPunishmentData[it.uniqueId] = listOf(
                                        resultSet.getLong("time"),
                                        resultSet.getString("reason"),
                                        "추방",
                                        0L,
                                        false,
                                        resultSet.getString("banned_by_uuid")
                                    )
                                }
                            }
                        }
                        statement.close()
                    }
                }
                count
            }
            val mutedCount = targetPlayers.map {
                var count: Long
                db.prepareStatement("SELECT COUNT(*) FROM {mutes} WHERE uuid=?").apply {
                    setString(1, it.uniqueId.toString())
                }.use { statement ->
                    statement.executeQuery().use { resultSet ->
                        count = if (resultSet.next()) {
                            resultSet.getLong(1)
                        } else {
                            0
                        }
                    }
                    statement.close()
                }

                if (count > 0) {
                    db.prepareStatement("SELECT * FROM {mutes} WHERE uuid=? ORDER BY id DESC LIMIT 1").apply {
                        setString(1, it.uniqueId.toString())
                    }.use { statement ->
                        statement.executeQuery().use { resultSet ->
                            if (resultSet.next()) {
                                if (latestPunishmentData[it.uniqueId] == null || (latestPunishmentData[it.uniqueId]!![0] as Long) < resultSet.getLong("time")) {
                                    latestPunishmentData[it.uniqueId] = listOf(
                                        resultSet.getLong("time"),
                                        resultSet.getString("reason"),
                                        "채금",
                                        resultSet.getLong("until"),
                                        resultSet.getBoolean("active"),
                                        resultSet.getString("banned_by_uuid")
                                    )
                                }
                            }
                        }
                        statement.close()
                    }
                }
                count
            }
            val warnedCount = targetPlayers.map {
                var count: Long
                db.prepareStatement("SELECT COUNT(*) FROM {warnings} WHERE uuid=?").apply {
                    setString(1, it.uniqueId.toString())
                }.use { statement ->
                    statement.executeQuery().use { resultSet ->
                        count = if (resultSet.next()) {
                            resultSet.getLong(1)
                        } else {
                            0
                        }
                    }
                    statement.close()
                }

                if (count > 0) {
                    db.prepareStatement("SELECT * FROM {warnings} WHERE uuid=? ORDER BY id DESC LIMIT 1").apply {
                        setString(1, it.uniqueId.toString())
                    }.use { statement ->
                        statement.executeQuery().use { resultSet ->
                            if (resultSet.next()) {
                                if (latestPunishmentData[it.uniqueId] == null || (latestPunishmentData[it.uniqueId]!![0] as Long) < resultSet.getLong("time")) {
                                    latestPunishmentData[it.uniqueId] = listOf(
                                        resultSet.getLong("time"),
                                        resultSet.getString("reason"),
                                        "경고",
                                        resultSet.getLong("until"),
                                        resultSet.getBoolean("active"),
                                        resultSet.getString("banned_by_uuid")
                                    )
                                }
                            }
                        }
                        statement.close()
                    }
                }
                count
            }

            liteBansDataCollection = listOf(isBanned, isMuted, bannedCount, kickedCount, mutedCount, warnedCount, latestPunishmentData)
            data[sender.uniqueId] = mutableListOf(targetPlayers, liteBansDataCollection, 0)
        }
    }

    internal fun template(sender: Player, targetUUID: List<UUID>, punishmentType: PunishmentType, pluginType: PluginType): InvFrame {
        val templateKeys = Main.template?.getConfigurationSection("templates")?.getKeys(false)?.toList()

        return frame((templateKeys?.size?.div(9) ?: 0) + 1, Component.text("Select Punishment Template")) {
            templateKeys?.forEachIndexed { index, it ->
                val material = Main.template?.getString("templates.$it.ItemStack.Material")?.uppercase()?.replace(" ", "_") ?: "PAPER"

                slot(index % 9, index / 9) {
                    item = ItemStack(Material.valueOf(material)).apply {
                        itemMeta = itemMeta.apply {
                            displayName(Component.text(Main.template?.getString("templates.$it.ItemStack.displayName").toString()
                                .replace("&", "§")))
                            lore(Main.template?.getStringList("templates.$it.ItemStack.lore")?.map { lore ->
                                Component.text(lore.replace("&", "§"))
                            })

                            if (Main.template?.getBoolean("templates.$it.ItemStack.glowing") == true) {
                                addEnchant(Enchantment.DURABILITY, 1, true)
                                addItemFlags(ItemFlag.HIDE_ENCHANTS)
                            }
                        }
                    }

                    onClick { _ ->
                        sender.openFrame(duration(sender, targetUUID, punishmentType, pluginType, it.toString())!!)
                    }
                }
            }
        }
    }

    private fun duration(sender: Player, targetUUID: List<UUID>, punishmentType: PunishmentType, pluginType: PluginType, template: String): InvFrame? {
        when (pluginType) {
            PluginType.VANILLA -> {
                punishment(sender, targetUUID, punishmentType, pluginType, template, null)
            }

            PluginType.LITEBANS -> {
                if (punishmentType == PunishmentType.BAN || punishmentType == PunishmentType.MUTE) {
                    val dateKeys = Main.template?.getConfigurationSection("durations")?.getKeys(false)?.toList()

                    return frame((dateKeys?.size?.div(9) ?: 0) + 1, Component.text("Select Punishment Duration")) {
                        dateKeys?.forEachIndexed { index, it ->
                            slot(index % 9, index / 9) {
                                item = ItemStack(Material.PAPER).apply {
                                    itemMeta = itemMeta.apply {
                                        displayName(Component.text(Main.template?.getString("durations.$it.displayName").toString()
                                            .replace("&", "§")))
                                    }
                                }

                                onClick { _ ->
                                    punishment(sender, targetUUID, punishmentType, pluginType, template, it.toString())
                                }
                            }
                        }
                    }
                } else {
                    punishment(sender, targetUUID, punishmentType, pluginType, template, null)
                }
            }
        }
        return null
    }

    private fun punishment(sender: Player, targetUUID: List<UUID>, punishmentType: PunishmentType, pluginType: PluginType, template: String, punishmentDuration: String?) {
        val playerOfflineList: List<OfflinePlayer> = targetUUID.map { sender.server.getOfflinePlayer(it) }

        val reason = Main.template?.getString("templates.$template.Reason")?.replace("&", "§")
        val duration = if (punishmentDuration != null) Main.template?.getInt("durations.$punishmentDuration.time") else -1

        sender.closeInventory()

        playerOfflineList.forEach {
            val isOnline = it.isOnline

            when (pluginType) {
                PluginType.LITEBANS -> {
                    val flags = "-S --sender-uuid=${sender.uniqueId} --sender=${sender.name}"

                    when(punishmentType) {
                        PunishmentType.BAN -> {
                            if (duration == -1) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "litebans:ban ${it.name} $reason $flags")
                                sender.sendMessage(Component.text("§a${it.name}님을 서버에서 차단하였습니다."))
                            } else {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "litebans:tempban ${it.name} $reason ${duration}m $flags")
                                sender.sendMessage(Component.text("§a${it.name}님을 서버에서 ${duration}분 동안 차단하였습니다."))
                            }
                        }
                        PunishmentType.KICK -> {
                            if (isOnline) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "litebans:kick ${it.name} $reason $flags")
                                sender.sendMessage(Component.text("§a${it.name}님을 서버에서 추방하였습니다."))
                            } else {
                                sender.sendMessage(errorIsOfflinePlayer)
                            }
                        }
                        PunishmentType.MUTE -> {
                            if (duration == -1) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "litebans:mute ${it.name} $reason $flags")
                                sender.sendMessage(Component.text("§a${it.name}님을 서버에서 채팅 금지하였습니다."))
                            } else {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "litebans:tempmute ${it.name} $reason ${duration}m $flags")
                                sender.sendMessage(Component.text("§a${it.name}님을 서버에서 ${duration}분 동안 채팅 금지하였습니다."))
                            }
                        }
                        PunishmentType.WARN -> {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "litebans:warn ${it.name} $reason $flags")
                            sender.sendMessage(Component.text("§a${it.name}님에게 경고를 하였습니다."))
                        }
                    }
                }
                PluginType.VANILLA -> {
                    val worlds = Bukkit.getWorlds()
                    val currentFeedback = worlds.map { world -> world.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK) }
                    worlds.forEach { world -> world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false) }

                    when(punishmentType) {
                        PunishmentType.BAN -> {
                            sender.performCommand("minecraft:ban ${it.name} $reason")
                            sender.sendMessage(Component.text("§a${it.name}님을 서버에서 차단하였습니다."))
                        }
                        PunishmentType.KICK -> {
                            if (isOnline) {
                                sender.performCommand("minecraft:kick ${it.name} $reason")
                                sender.sendMessage(Component.text("§a${it.name}님을 서버에서 추방하였습니다."))
                            } else {
                                sender.sendMessage(errorIsOfflinePlayer)
                            }
                        }
                        else -> {
                            sender.sendMessage(Component.text("§c해당 기능은 'LiteBans' 플러그인이 있을 때만 사용할 수 있습니다."))
                        }
                    }

                    worlds.forEachIndexed { index, world -> world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, currentFeedback[index]!!) }
                }
            }
        }
        sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
    }
}