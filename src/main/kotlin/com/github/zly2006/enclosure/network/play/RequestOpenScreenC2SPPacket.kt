package com.github.zly2006.enclosure.network.play

import com.github.zly2006.enclosure.EnclosureArea
import com.github.zly2006.enclosure.ServerMain.getEnclosure
import com.github.zly2006.enclosure.ServerMain.getSmallestEnclosure
import com.github.zly2006.enclosure.gui.EnclosureScreenHandler
import com.github.zly2006.enclosure.minecraftServer
import com.github.zly2006.enclosure.network.NetworkChannels
import com.github.zly2006.enclosure.network.config.EnclosureInstalledC2SPacket
import com.github.zly2006.enclosure.utils.TrT
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

// 请求服务器的领地信息，
class RequestOpenScreenC2SPPacket(
    var name: String,
    var dimId: Identifier,
    var pos: BlockPos
) : CustomPayload {
    override fun getId() = ID

    companion object {
        val ID = CustomPayload.Id<RequestOpenScreenC2SPPacket>(NetworkChannels.OPEN_REQUEST)
        private val CODEC = PacketCodec.of<PacketByteBuf, RequestOpenScreenC2SPPacket>(
            { value, buf ->
                buf.writeString(value.name)
                buf.writeIdentifier(value.dimId)
                buf.writeBlockPos(value.pos)
            },
            { buf ->
                RequestOpenScreenC2SPPacket(
                    buf.readString(),
                    buf.readIdentifier(),
                    buf.readBlockPos()
                )
            }
        )

        fun register() {
            PayloadTypeRegistry.playC2S().register(ID, CODEC)
            ServerPlayNetworking.registerGlobalReceiver(ID) { payload, context: ServerPlayNetworking.Context ->
                val area: EnclosureArea?
                if (EnclosureInstalledC2SPacket.isInstalled(context.player())) {
                    if (payload.name.isEmpty()) {
                        val world = minecraftServer.getWorld(RegistryKey.of(RegistryKeys.WORLD, payload.dimId))
                        if (world == null) {
                            context.player().sendMessage(TrT.of("enclosure.message.no_enclosure"))
                            return@registerGlobalReceiver
                        }
                        area = getSmallestEnclosure(world, payload.pos)
                    } else {
                        area = getEnclosure(payload.name)
                    }
                    if (area == null) {
                        context.player().sendMessage(TrT.of("enclosure.message.no_enclosure"))
                        return@registerGlobalReceiver
                    }
                    EnclosureScreenHandler.open(context.player(), area)
                }
            }
        }
    }
}
