package com.github.zly2006.enclosure.network

import com.github.zly2006.enclosure.MOD_VERSION
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.loader.api.Version
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import java.util.*

class EnclosureInstalledC2SPacket(
    val version: Version
) : CustomPayload {
    constructor(buf: PacketByteBuf) : this(Version.parse(buf.readString()))

    private fun write(buf: PacketByteBuf) {
        buf.writeString(version.friendlyString)
    }


    override fun getId() = ID

    companion object {
        val installedClientMod: MutableMap<UUID, Version> = HashMap()
        val ID: CustomPayload.Id<EnclosureInstalledC2SPacket?> = CustomPayload.Id(Identifier("enclosure:installed"))
        val CODEC: PacketCodec<PacketByteBuf, EnclosureInstalledC2SPacket?> = CustomPayload.codecOf(
            { obj, buf -> obj!!.write(buf) },
            { buf -> EnclosureInstalledC2SPacket(buf) })

        @JvmStatic
        fun isInstalled(player: ServerPlayerEntity?): Boolean {
            if (player == null) return false
            return installedClientMod.containsKey(player.uuid)
        }

        fun clientVersion(connection: ServerPlayerEntity): Version? {
            return installedClientMod[connection.uuid]
        }

        fun register() {
            PayloadTypeRegistry.configurationC2S().register(ID, CODEC)
            ServerConfigurationNetworking.registerGlobalReceiver<EnclosureInstalledC2SPacket>(ID) { payload, context ->
                if (payload == null) return@registerGlobalReceiver
                installedClientMod[context.networkHandler().debugProfile.id] = payload.version
            }
            ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler, _ ->
                installedClientMod.remove(handler.player.uuid)
            })
        }

        @JvmStatic
        fun send() {
            ClientPlayNetworking.send(EnclosureInstalledC2SPacket(MOD_VERSION))
        }
    }
}
