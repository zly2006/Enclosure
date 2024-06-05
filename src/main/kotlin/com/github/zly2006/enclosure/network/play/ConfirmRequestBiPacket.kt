package com.github.zly2006.enclosure.network.play

import com.github.zly2006.enclosure.gui.ConfirmScreen
import com.github.zly2006.enclosure.gui.EnclosureGui
import com.github.zly2006.enclosure.network.NetworkChannels
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.text.Text
import net.minecraft.text.TextCodecs

class ConfirmRequestBiPacket(val text: Text) : CustomPayload {
    override fun getId() = ID

    companion object {
        val ID = CustomPayload.Id<ConfirmRequestBiPacket>(NetworkChannels.CONFIRM)
        private val CODEC = PacketCodec.of<PacketByteBuf, ConfirmRequestBiPacket>(
            { value, buf ->
                TextCodecs.PACKET_CODEC.encode(
                    buf,
                    value!!.text
                )
            },
            { buf -> ConfirmRequestBiPacket(TextCodecs.PACKET_CODEC.decode(buf)) }
        )

        @JvmStatic
        fun register() {
            PayloadTypeRegistry.playS2C().register(ID, CODEC)
            PayloadTypeRegistry.playC2S().register(ID, CODEC)
            if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
                ClientPlayNetworking.registerGlobalReceiver(ID) { payload: ConfirmRequestBiPacket?, context: ClientPlayNetworking.Context ->
                    val client = MinecraftClient.getInstance()
                    if (client.currentScreen is EnclosureGui) {
                        val message = payload!!.text
                        client.execute {
                            // mark received
                            context.responseSender().sendPacket(ConfirmRequestBiPacket(Text.empty()))
                            client.setScreen(ConfirmScreen(client.currentScreen, message) {
                                checkNotNull(client.player)
                                client.player!!.networkHandler.sendCommand("enclosure confirm")
                            })
                        }
                    }
                }
            }
        }
    }
}
