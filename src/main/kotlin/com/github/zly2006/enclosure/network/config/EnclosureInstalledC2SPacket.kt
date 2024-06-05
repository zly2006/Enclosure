package com.github.zly2006.enclosure.network.config

import com.github.zly2006.enclosure.LOGGER
import com.github.zly2006.enclosure.MOD_VERSION
import com.github.zly2006.enclosure.minecraftServer
import com.github.zly2006.enclosure.network.NetworkChannels
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.loader.api.SemanticVersion
import net.fabricmc.loader.api.Version
import net.fabricmc.loader.api.VersionParsingException
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import java.util.*

class EnclosureInstalledC2SPacket(var version: Version?) : CustomPayload {
    override fun getId() = ID

    companion object {
        val installedClientMod: MutableMap<UUID, Version> = HashMap()

        val ID = CustomPayload.Id<EnclosureInstalledC2SPacket>(NetworkChannels.ENCLOSURE_INSTALLED)
        private val CODEC = PacketCodec.of<PacketByteBuf, EnclosureInstalledC2SPacket>(
            { value, buf -> buf.writeString(value!!.version!!.friendlyString) },
            { buf ->
                try {
                    EnclosureInstalledC2SPacket(Version.parse(buf.readString()))
                } catch (e: VersionParsingException) {
                    EnclosureInstalledC2SPacket(null)
                }
            }
        )

        fun isInstalled(player: ServerPlayerEntity?): Boolean {
            if (player == null) return false
            return installedClientMod.containsKey(player.uuid)
        }

        fun clientVersion(connection: ServerPlayerEntity): Version? {
            return installedClientMod[connection.uuid]
        }

        fun register() {
            ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler: ServerPlayNetworkHandler, server: MinecraftServer? ->
                installedClientMod -= handler.player.uuid
            })
            PayloadTypeRegistry.configurationC2S().register(ID, CODEC)
            ServerConfigurationNetworking.registerGlobalReceiver(ID) { payload, context ->
                val version = payload!!.version
                val uuid = context.networkHandler().debugProfile.id
                if (version is SemanticVersion && MOD_VERSION is SemanticVersion
                    && version.getVersionComponent(0) == MOD_VERSION.getVersionComponent(0)
                    && version.getVersionComponent(1) >= MOD_VERSION.getVersionComponent(1)
                ) {
                    LOGGER.info(context.networkHandler().debugProfile.name + " joined with a matching enclosure client.")
                    installedClientMod[uuid] = version

                    context.responseSender().sendPacket(UUIDCacheS2CPacket(minecraftServer.userCache!!))
                } else if (version != null) {
                    context.networkHandler().sendPacket(
                        GameMessageS2CPacket(
                            Text.translatable(
                                "enclosure.message.outdated",
                                MOD_VERSION.friendlyString,
                                version.friendlyString
                            ), false
                        )
                    )
//                    context.player().sendMessage(
//
//                    )
                }
            }
        }
    }
}
