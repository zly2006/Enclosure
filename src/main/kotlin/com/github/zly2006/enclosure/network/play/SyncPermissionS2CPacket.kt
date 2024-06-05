package com.github.zly2006.enclosure.network.play

import com.github.zly2006.enclosure.gui.PermissionScreen
import com.github.zly2006.enclosure.network.NetworkChannels
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import java.util.*

class SyncPermissionS2CPacket(
    var uuid: UUID,
    var permission: NbtCompound
) : CustomPayload {
    override fun getId() = ID

    companion object {
        val ID = CustomPayload.Id<SyncPermissionS2CPacket>(NetworkChannels.SYNC_PERMISSION)
        private val CODEC = PacketCodec.of<PacketByteBuf, SyncPermissionS2CPacket>(
            { value, buf ->
                buf.writeUuid(value.uuid)
                buf.writeNbt(value.permission)
            },
            { buf -> SyncPermissionS2CPacket(buf.readUuid(), buf.readNbt()!!) }
        )

        @JvmStatic
        fun register() {
            PayloadTypeRegistry.playS2C().register(ID, CODEC)
            ClientPlayNetworking.registerGlobalReceiver(ID) { payload, _ ->
                val screen = MinecraftClient.getInstance().currentScreen
                if (screen is PermissionScreen) {
                    if (screen.uuid == payload!!.uuid) {
                        screen.syncPermission(payload.permission)
                    }
                }
            }
        }
    }
}
