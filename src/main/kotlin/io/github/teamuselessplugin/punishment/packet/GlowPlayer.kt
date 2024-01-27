package io.github.teamuselessplugin.punishment.packet

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.WrappedDataValue
import com.comphenix.protocol.wrappers.WrappedDataWatcher
import io.github.teamuselessplugin.punishment.Main
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType
import kotlin.experimental.and

internal class GlowPlayer(private var target: Player? = null) {
    private val watchers = mutableListOf<Player>()
    private var protocolListener: PacketAdapter? = null

    /**
     * 지정된 플레이어의 강조 효과를 활성화 합니다.
     *
     * @return 성공 여부
     */
    fun show(): Boolean {
        // 이미 강조 효과가 활성화된 상태거나 플레이어가 지정되지 않은 경우 false를 반환
        if (protocolListener != null || target == null || watchers.isEmpty()) {
            return false
        }

        return init()
    }


    /**
     * 강조된 플레이어의 강조 효과를 해제합니다.
     *
     * @return 성공 여부
     */
    fun hide(): Boolean {
        // 이미 강조 효과가 비활성화된 상태거나 플레이어가 지정되지 않은 경우 false를 반환
        if (protocolListener == null || target == null || watchers.isEmpty()) {
            return false
        }

        return destroy()
    }

    /**
     * 강조된 플레이어를 볼수 있는 플레이어를 추가합니다.
     *
     * @param player 플레이어
     * @return 성공 여부
     */
    fun addWatcher(player: Player): Boolean {
        // 버려진 Player 객체를 제거합니다.
        watchers.removeIf { it.uniqueId == player.uniqueId && it != player }

        // 플레이어가 추가된 상태일 경우
        if (watchers.contains(player)) {
            return false
        }

        watchers.add(player)

        // 강조 효과가 활성화된 상태일 때
        if (protocolListener != null) {
            packetSend(0x40.toByte(), mWatcher = listOf(player))
        }
        return true
    }

    /**
     * 강조된 플레이어를 볼수 있는 플레이어를 제거합니다.
     *
     * @param player 플레이어
     * @return 성공 여부
     */
    fun removeWatcher(player: Player): Boolean {
        // 플레이어가 추가되지 않은 상태일 경우
        if (!watchers.contains(player)) {
            return false
        }

        watchers.remove(player)

        // 강조 효과가 활성화된 상태일 때
        if (protocolListener != null) {
            packetSend(0x00.toByte(), mWatcher = listOf(player))
        }
        return true
    }

    /**
     * 강조 효과를 적용할 플레이어를 설정합니다.
     *
     * @param target 플레이어
     * @return 성공 여부
     */
    fun setTarget(target: Player?): Boolean {
        val oldTarget = this.target
        this.target = target

        // 강조 효과가 활성화된 상태일 때
        if (protocolListener != null) {
            packetSend(0x00.toByte(), mTarget = oldTarget)
            packetSend(0x40.toByte(), mTarget = target)
        }
        return true
    }

    /**
     * 강조 효과를 적용할 플레이어를 반환합니다.
     *
     * @return 플레이어
     */
    fun getTarget(): Player? {
        return target
    }

    /**
     * 강조된 플레이어를 볼수 있는 플레이어 목록을 반환합니다.
     *
     * @return 플레이어 목록
     */
    fun getWatchers(): List<Player> {
        return watchers
    }

    private fun init(): Boolean {
        protocolListener = object : PacketAdapter(
            params()
                .plugin(Main.instance!!)
                .types(PacketType.Play.Server.ENTITY_METADATA)
                .clientSide()
                .listenerPriority(ListenerPriority.NORMAL)
        ) {
            override fun onPacketSending(event: PacketEvent) {
                // watcher가 아닌 경우에는 packet을 보내지 않음
                val packet = event.packet.deepClone()
                var indexZero = 0

                if (watchers.contains(event.player) && event.packet.integers.read(0) == target?.entityId) {
                    val values = mutableListOf<WrappedDataValue>()
                    packet.dataValueCollectionModifier.read(0).forEach {
                        if (it.index == 0 && (it.value as Byte).and(0x40.toByte()) == 0x00.toByte()) {
                            indexZero++
                            values.add(WrappedDataValue(
                                0,
                                WrappedDataWatcher.Registry.get(Byte::class.javaObjectType),
                                (it.value as Byte).plus(0x40.toByte()).toByte()
                            ))
                        } else {
                            values.add(it)
                        }

                        if (it.index == 17 && indexZero == 0) {
                            values.add(WrappedDataValue(
                                0,
                                WrappedDataWatcher.Registry.get(Byte::class.javaObjectType),
                                0x40.toByte()
                            ))
                        }
                        packet.dataValueCollectionModifier.write(0, values)

                        event.packet = packet
                    }

//                    디버깅용 로그 !!!
//                    watchers.forEach { player ->
//                        if (packet.dataValueCollectionModifier.read(0)[0].index != 1) {
//                            player.sendMessage("--------------------")
//                            player.sendMessage("Size : ${packet.dataValueCollectionModifier.read(0).size}")
//                            packet.dataValueCollectionModifier.read(0).forEach { data ->
//                                if (data.index != 1) player.sendMessage("Index : ${data.index}, Value : ${data.value}")
//                            }
//                            player.sendMessage("--------------------")
//                        }
//                    }
                }
            }
        }

        ProtocolLibrary.getProtocolManager().addPacketListener(protocolListener)
        return packetSend(0x40.toByte())
    }
    private fun destroy(): Boolean {
        ProtocolLibrary.getProtocolManager().removePacketListener(protocolListener)
        protocolListener = null

        // glowing effect가 없는 경우에만 packet을 보냄
        if (!target?.hasPotionEffect(PotionEffectType.GLOWING)!!) {
            return packetSend(0x00.toByte())
        }
        return true
    }
    private fun packetSend(packet: Byte, mTarget: Player? = target, mWatcher: List<Player> = watchers): Boolean {
        PacketContainer(PacketType.Play.Server.ENTITY_METADATA).let {
            it.integers.write(0, mTarget?.entityId)
            val serializer = WrappedDataWatcher.Registry.get(Byte::class.javaObjectType)

            val value = WrappedDataValue(0, serializer, packet)
            it.dataValueCollectionModifier.write(0, listOf(value))

            try {
                mWatcher.forEach { player ->
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player, it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
            return true
        }
    }
}