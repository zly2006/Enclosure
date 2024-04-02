package com.github.zly2006.enclosure.network

import com.github.zly2006.enclosure.EnclosureArea
import com.github.zly2006.enclosure.ServerMain
import com.github.zly2006.enclosure.gui.EnclosureScreenHandler
import com.github.zly2006.enclosure.minecraftServer
import com.github.zly2006.enclosure.utils.TrT
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

class RequestOpenScreenC2SPPacket(
    val name: String,
    val dimId: Identifier,
    val pos: IntArray
) : CustomPayload {
    constructor(buf: PacketByteBuf): this(
        buf.readString(),
        buf.readIdentifier(),
        buf.readIntArray()
    )

    override fun getId(): CustomPayload.Id<out CustomPayload?> {
        return ID
    }

    fun write(buf: PacketByteBuf) {
        buf.writeString(name)
        buf.writeIdentifier(dimId)
        buf.writeIntArray(pos)
    }

    companion object {
        val ID: CustomPayload.Id<RequestOpenScreenC2SPPacket?> = CustomPayload.Id(Identifier("enclosure:open_request"))
        val CODEC: PacketCodec<PacketByteBuf, RequestOpenScreenC2SPPacket?> = CustomPayload.codecOf(
            { obj, buf -> obj!!.write(buf) },
            { buf -> RequestOpenScreenC2SPPacket(buf) })

        @JvmStatic
        fun register() {
            PayloadTypeRegistry.configurationS2C().register(ID, CODEC)
            ServerPlayNetworking.registerGlobalReceiver(ID) { payload: RequestOpenScreenC2SPPacket?, context: ServerPlayNetworking.Context? ->
                val blockPos = BlockPos(payload!!.pos[0], payload.pos[1], payload.pos[2])
                var area: EnclosureArea? = null
                if (EnclosureInstalledC2SPacket.isInstalled(context!!.player())) {
                    if (payload.name.isEmpty()) {
                        val world = minecraftServer.getWorld(RegistryKey.of(RegistryKeys.WORLD, payload.dimId))
                        if (world == null) {
                            context.player().sendMessage(TrT.of("enclosure.message.no_enclosure"))
                            return@registerGlobalReceiver
                        }
                        area = ServerMain.getSmallestEnclosure(world, blockPos)
                    } else {
                        area = ServerMain.getAllEnclosures(context.player().serverWorld).getArea(context.player().blockPos)
                    }
                    if (area == null) {
                        context.player().sendMessage(TrT.of("enclosure.message.no_enclosure"))
                        return@registerGlobalReceiver
                    }
                    EnclosureScreenHandler.open(context.player(), area)
                }
            }
        }

        @JvmStatic
        fun send(client: MinecraftClient, name: String) {
            ClientPlayNetworking.send(RequestOpenScreenC2SPPacket(
                name,
                client.player!!.world.registryKey.value,
                intArrayOf(
                    client.player!!.blockPos.x,
                    client.player!!.blockPos.y,
                    client.player!!.blockPos.z
                )
            ))
        }
    }
}
