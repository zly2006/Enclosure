package com.github.zly2006.enclosure.client.mixin

import com.github.zly2006.enclosure.access.ServerMetadataAccess
import net.minecraft.client.network.MultiplayerServerListPinger
import net.minecraft.client.network.ServerAddress
import net.minecraft.client.network.ServerInfo
import net.minecraft.network.ClientConnection
import net.minecraft.network.listener.ClientQueryPacketListener
import net.minecraft.network.listener.PacketListener
import net.minecraft.network.packet.s2c.query.QueryPongS2CPacket
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket
import net.minecraft.text.Text
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.ModifyArg
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.LocalCapture
import java.net.InetSocketAddress
import java.util.*

@Suppress("NonJavaMixin")
@Mixin(MultiplayerServerListPinger::class)
class MixinServerPing {
    var entry: ServerInfo? = null
    @Inject(
        method = ["add"],
        locals = LocalCapture.CAPTURE_FAILSOFT,
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/ClientConnection;setPacketListener(Lnet/minecraft/network/listener/PacketListener;)V"
        )]
    )
    private fun captureEntry(
        entry: ServerInfo,
        saver: Runnable,
        ci: CallbackInfo,
        serverAddress: ServerAddress,
        optional: Optional<*>,
        inetSocketAddress: InetSocketAddress,
        clientConnection: ClientConnection
    ) {
        this.entry = entry
    }

    @ModifyArg(
        method = ["add"],
        at = At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/ClientConnection;setPacketListener(Lnet/minecraft/network/listener/PacketListener;)V"
        )
    )
    private fun modifyServerHandler(pl: PacketListener): PacketListener {
        val listener = pl as ClientQueryPacketListener
        val finalEntry = entry
        return object : ClientQueryPacketListener {
            override fun onResponse(packet: QueryResponseS2CPacket) {
                listener.onResponse(packet)
                val access = packet.metadata() as ServerMetadataAccess
                val entryAccess = finalEntry as ServerMetadataAccess?
                entryAccess!!.modName = access.modName
                entryAccess.modVersion = access.modVersion
            }

            override fun onPong(packet: QueryPongS2CPacket) {
                listener.onPong(packet)
            }

            override fun onDisconnected(reason: Text) {
                listener.onDisconnected(reason)
            }

            override fun isConnectionOpen(): Boolean {
                return listener.isConnectionOpen
            }
        }
    }
}
