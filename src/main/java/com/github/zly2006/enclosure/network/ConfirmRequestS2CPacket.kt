package com.github.zly2006.enclosure.network

import com.github.zly2006.enclosure.gui.ConfirmScreen
import com.github.zly2006.enclosure.gui.EnclosureGui
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class ConfirmRequestS2CPacket(
    val message: Text
) : CustomPayload {
    var client = MinecraftClient.getInstance()
    constructor(buf: PacketByteBuf): this(
        Text.Serialization.fromJson(buf.readString(), MinecraftClient.getInstance().world!!.registryManager)!!
    )


    private fun write(buf: PacketByteBuf) {
        buf.writeString(Text.Serialization.toJsonString(message, client.world!!.registryManager))
    }

    override fun getId(): CustomPayload.Id<out CustomPayload?> {
        return ID
    }

    companion object {
        val ID: CustomPayload.Id<ConfirmRequestS2CPacket?> = CustomPayload.Id(Identifier("enclosure:confirm"))
        val CODEC: PacketCodec<PacketByteBuf, ConfirmRequestS2CPacket?> = CustomPayload.codecOf(
            { obj, buf -> obj!!.write(buf) },
            { buf -> ConfirmRequestS2CPacket(buf) })

        @JvmStatic
        fun register() {
            PayloadTypeRegistry.configurationS2C().register(ID, CODEC)
            ClientPlayNetworking.registerGlobalReceiver(ID) { payload: ConfirmRequestS2CPacket?, context: ClientPlayNetworking.Context? ->
                val client = MinecraftClient.getInstance()
                if (client.currentScreen is EnclosureGui) {
                    client.execute {
                        ClientPlayNetworking.send(payload)
                        client.setScreen(ConfirmScreen(client.currentScreen, payload!!.message) {
                            assert(client.player != null)
                            client.player!!.networkHandler.sendCommand("enclosure confirm")
                        })
                    }
                }
            }
        }
    }
}
