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

    fun show(): Boolean {
        if (protocolListener == null) {
            val packet = PacketContainer(PacketType.Play.Server.ENTITY_METADATA)
            packet.integers.write(0, target?.entityId)
            val serializer = WrappedDataWatcher.Registry.get(Byte::class.javaObjectType)

            val value = WrappedDataValue(0, serializer, 0x40.toByte())
            packet.dataValueCollectionModifier.write(0, listOf(value))

            try {
                watchers.forEach {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(it, packet)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
            init()
            return true
        }
        return false
    }

    fun hide(): Boolean {
        if (protocolListener != null) {
            destroy()

            // glowing effect가 없는 경우에만 packet을 보냄
            if (!target?.hasPotionEffect(PotionEffectType.GLOWING)!!) {
                val packet = PacketContainer(PacketType.Play.Server.ENTITY_METADATA)
                packet.integers.write(0, target?.entityId)
                val serializer = WrappedDataWatcher.Registry.get(Byte::class.javaObjectType)

                val value = WrappedDataValue(0, serializer, 0.toByte())
                packet.dataValueCollectionModifier.write(0, listOf(value))

                try {
                    watchers.forEach {
                        ProtocolLibrary.getProtocolManager().sendServerPacket(it, packet)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    return false
                }
            }
            return true
        }
        return false
    }

    fun addWatcher(player: Player) {
        watchers.add(player)
    }

    fun removeWatcher(player: Player) {
        watchers.remove(player)
    }

    fun setTarget(target: Player?) {
        this.target = target
    }

    private fun init() {
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
    }

    private fun destroy() {
        ProtocolLibrary.getProtocolManager().removePacketListener(protocolListener)
        protocolListener = null
    }
}