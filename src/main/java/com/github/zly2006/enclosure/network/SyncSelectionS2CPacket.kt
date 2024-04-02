package com.github.zly2006.enclosure.network

import com.github.zly2006.enclosure.client.ClientMain
import com.github.zly2006.enclosure.command.Session
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

@Environment(EnvType.CLIENT)
class SyncSelectionS2CPacket(
    val pos1: BlockPos,
    val pos2: BlockPos,
)  : CustomPayload {
    fun receive(
        client: MinecraftClient?,
        handler: ClientPlayNetworkHandler?,
        buf: PacketByteBuf,
        responseSender: PacketSender?
    ) {
        val pos1 = buf.readBlockPos()
        val pos2 = buf.readBlockPos()
    }

    fun write(buf: PacketByteBuf) {
        buf.writeBlockPos(pos1)
        buf.writeBlockPos(pos2)
    }

    override fun getId() = ID

    companion object {
        val ID = CustomPayload.Id<SyncSelectionS2CPacket>(Identifier("enclosure:sync_selection"))
        val CODEC: PacketCodec<PacketByteBuf, SyncSelectionS2CPacket?> = CustomPayload.codecOf(
            { obj, buf -> obj!!.write(buf) },
            { buf -> SyncSelectionS2CPacket(buf.readBlockPos(), buf.readBlockPos()) })
        fun register() {

            PayloadTypeRegistry.configurationS2C().register(ID, CODEC)
            ClientPlayNetworking.registerGlobalReceiver(ID, ) { payload, context ->

                if (ClientMain.clientSession == null) {
                    ClientMain.clientSession = Session(null)
                }
                ClientMain.clientSession.pos1 = payload.pos1
                ClientMain.clientSession.pos2 = payload.pos2
            }
        }
    }
}
