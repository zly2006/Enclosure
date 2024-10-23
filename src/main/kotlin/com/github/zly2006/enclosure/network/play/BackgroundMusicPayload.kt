package com.github.zly2006.enclosure.network.play

import com.github.zly2006.enclosure.access.ClientAccess
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.registry.RegistryKeys
import net.minecraft.sound.MusicSound
import net.minecraft.util.Identifier

class BackgroundMusicPayload(
    val enclosure: String?,
    val music: Identifier?
): CustomPayload {
    companion object {
        val ID = CustomPayload.Id<BackgroundMusicPayload>(Identifier.of("enclosure", "background_music"))
        private val CODEC = PacketCodec.of<PacketByteBuf, BackgroundMusicPayload>(
            { value, buf ->
                buf.writeNullable(value!!.enclosure, PacketByteBuf::writeString)
                buf.writeNullable(value.music, PacketByteBuf::writeIdentifier)
            },
            { buf ->
                BackgroundMusicPayload(buf.readNullable(PacketByteBuf::readString), buf.readNullable(PacketByteBuf::readIdentifier))
            }
        )

        fun register() {
            PayloadTypeRegistry.playS2C().register(ID, CODEC)
            PayloadTypeRegistry.playC2S().register(ID, CODEC)

            if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
                ClientPlayNetworking.registerGlobalReceiver(ID) { p, _ ->
                    val mc = MinecraftClient.getInstance()
                    if (p.music == null) {
                        (mc as ClientAccess).bgm = null
                    } else {
                        (mc as ClientAccess).bgm = MusicSound(
                            mc.player!!.registryManager.toImmutable().getOrThrow(RegistryKeys.SOUND_EVENT).getEntry(p.music).orElseThrow(),
                            12000,
                            24000,
                            true
                        )
                    }
                }
            }
        }
    }

    override fun getId() = ID
}
