package com.github.zly2006.enclosure.network

import com.github.zly2006.enclosure.gui.PermissionScreen
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import java.util.*

class SyncPermissionS2CPacket(
    val uuid: UUID,
    val permission: NbtCompound
) : CustomPayload {
    constructor(buf: PacketByteBuf): this(
        buf.readUuid(),
        buf.readNbt()!!
    )

    private fun write(buf: PacketByteBuf) {
        buf.writeUuid(uuid)
        buf.writeNbt(permission)
    }

    companion object {
         val ID: CustomPayload.Id<SyncPermissionS2CPacket> = CustomPayload.Id(Identifier("enclosure:open_request"))
        val CODEC: PacketCodec<PacketByteBuf, SyncPermissionS2CPacket?> = CustomPayload.codecOf(
            { obj, buf -> obj!!.write(buf) },
            { buf -> SyncPermissionS2CPacket(buf) })

        @JvmStatic
        fun register() {

            PayloadTypeRegistry.playS2C().register(ID, CODEC)
            ClientPlayNetworking.registerGlobalReceiver(ID) { payload: SyncPermissionS2CPacket, context: ClientPlayNetworking.Context? ->
                val client = MinecraftClient.getInstance()
                val screen = client.currentScreen
                if (screen is PermissionScreen) {
                    if (screen.uuid == payload.uuid) {
                        screen.syncPermission(payload.permission)
                    }
                }
            }
        }
    }

    override fun getId() = ID
}
