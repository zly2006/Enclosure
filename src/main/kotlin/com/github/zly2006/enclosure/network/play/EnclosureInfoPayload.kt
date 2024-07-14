package com.github.zly2006.enclosure.network.play

import com.github.zly2006.enclosure.network.NetworkChannels
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

class EnclosureInfoPayload(
    val fullName: String,
    val compound: NbtCompound,
    val worldId: Identifier,
    val fatherFullName: String?,
    val subAreaNames: List<String>,
    val openGui: Boolean = false,
): CustomPayload {
    companion object {
        fun register() {
            PayloadTypeRegistry.playS2C().register(ID, CODEC)
            if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
                ClientPlayNetworking.registerGlobalReceiver(ID) { p, context ->
                    TODO()
                }
            }
        }

        val ID = CustomPayload.Id<EnclosureInfoPayload>(NetworkChannels.ENCLOSURE_INFO)
        private val CODEC = PacketCodec.of<PacketByteBuf, EnclosureInfoPayload>(
            { value, buf ->
                buf.writeString(value.fullName)
                buf.writeNbt(value.compound)
                buf.writeIdentifier(value.worldId)
                buf.writeNullable(value.fatherFullName, PacketByteBuf::writeString)
                buf.writeCollection(value.subAreaNames, PacketByteBuf::writeString)
                buf.writeVarInt(value.subAreaNames.size)
                buf.writeBoolean(value.openGui)
            },
            { buf ->
                EnclosureInfoPayload(
                    buf.readString(),
                    buf.readNbt()!!,
                    buf.readIdentifier(),
                    buf.readNullable(PacketByteBuf::readString),
                    buf.readList(PacketByteBuf::readString),
                    buf.readBoolean()
                )
            }
        )
    }

    override fun getId() = ID
}
