package com.github.zly2006.enclosure.network.play

import com.github.zly2006.enclosure.ServerMain.clientSide
import com.github.zly2006.enclosure.client.ClientMain
import com.github.zly2006.enclosure.command.Session
import com.github.zly2006.enclosure.network.NetworkChannels
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.math.BlockPos

class SyncSelectionS2CPacket(var pos1: BlockPos, var pos2: BlockPos) : CustomPayload {
    override fun getId(): CustomPayload.Id<out CustomPayload?> {
        return ID
    }

    companion object {
        val ID = CustomPayload.Id<SyncSelectionS2CPacket>(NetworkChannels.SYNC_SELECTION)
        private val CODEC = PacketCodec.of<PacketByteBuf, SyncSelectionS2CPacket>(
            { value, buf ->
                buf.writeBlockPos(value!!.pos1)
                buf.writeBlockPos(value.pos2)
            },
            { buf -> SyncSelectionS2CPacket(buf.readBlockPos(), buf.readBlockPos()) }
        )

        @JvmStatic
        fun register() {
            PayloadTypeRegistry.playS2C().register(ID, CODEC)
            if (clientSide) {
                ClientPlayNetworking.registerGlobalReceiver(ID) { payload, _ ->
                    if (ClientMain.clientSession == null) {
                        ClientMain.clientSession = Session(null)
                    }
                    ClientMain.clientSession!!.pos1 = payload!!.pos1
                    ClientMain.clientSession!!.pos2 = payload.pos2
                }
            }
        }
    }
}
