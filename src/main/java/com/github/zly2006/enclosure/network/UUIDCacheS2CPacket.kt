package com.github.zly2006.enclosure.network

import com.github.zly2006.enclosure.LOGGER
import com.github.zly2006.enclosure.client.ClientMain
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import java.util.*

class UUIDCacheS2CPacket(
    val cache: NbtCompound
)  : CustomPayload {
    constructor(buf: PacketByteBuf): this(buf.readNbt()!!)

    fun write(buf: PacketByteBuf) {
        buf.writeNbt(cache)
    }

    override fun getId() = ID

    companion object {
        @JvmField
        val uuid2name: MutableMap<UUID, String> = HashMap()
        @JvmStatic
        fun getName(uuid: UUID): String? {
            if (uuid2name.containsKey(uuid)) {
                return uuid2name[uuid]
            }
            return uuid.toString()
        }
        val ID = CustomPayload.Id<UUIDCacheS2CPacket>(Identifier("enclosure:uuid_cache"))
        val CODEC: PacketCodec<PacketByteBuf, UUIDCacheS2CPacket?> = CustomPayload.codecOf(
            { obj, buf -> obj!!.write(buf) },
            { buf -> UUIDCacheS2CPacket(buf) })
        @JvmStatic
        fun register() {
            PayloadTypeRegistry.playS2C().register(ID, CODEC)
            ClientPlayNetworking.registerGlobalReceiver(ID) { payload, _ ->
                uuid2name.clear()
                payload.cache.keys.forEach { key -> uuid2name[payload.cache.getUuid(key)!!] = key }
                LOGGER.info("Received UUID cache from server.")
                ClientMain.isEnclosureInstalled = true
            }
        }
    }
}
