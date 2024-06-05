package com.github.zly2006.enclosure.network.config

import com.github.zly2006.enclosure.LOGGER
import com.github.zly2006.enclosure.client.ClientMain
import com.github.zly2006.enclosure.network.NetworkChannels
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.UserCache
import java.util.*

class UUIDCacheS2CPacket : CustomPayload {
    var uuid2name: MutableMap<UUID, String> = HashMap()

    constructor()

    constructor(userCache: UserCache) {
        userCache.byName.forEach { (name, entry) -> uuid2name[entry.getProfile().id] = name }
    }

    override fun getId(): CustomPayload.Id<out CustomPayload?> {
        return ID
    }

    companion object {
        val ID = CustomPayload.Id<UUIDCacheS2CPacket>(NetworkChannels.SYNC_UUID)
        private val CODEC = PacketCodec.of<PacketByteBuf, UUIDCacheS2CPacket>(
            { value, buf ->
                buf.writeMap(value!!.uuid2name, PacketByteBuf::writeUuid, PacketByteBuf::writeString)
            },
            { buf ->
                UUIDCacheS2CPacket().apply {
                    uuid2name = buf.readMap(PacketByteBuf::readUuid, PacketByteBuf::readString)
                }
            }
        )

        @JvmStatic
        fun getName(uuid: UUID): String? {
            if (ClientMain.uuid2name.containsKey(uuid)) {
                return ClientMain.uuid2name[uuid]
            }
            return uuid.toString()
        }

        @JvmStatic
        fun register() {
            PayloadTypeRegistry.configurationS2C().register(ID, CODEC)
            ClientConfigurationNetworking.registerGlobalReceiver(ID) { payload, _ ->
                ClientMain.uuid2name = payload!!.uuid2name
                LOGGER.info("Received UUID cache from server.")
                ClientMain.isEnclosureInstalled = true
            }
        }
    }
}
