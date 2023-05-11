package com.github.zly2006.enclosure.client.mixin;

import com.github.zly2006.enclosure.access.ServerMetadataAccess;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientQueryPacketListener;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.s2c.query.QueryPongS2CPacket;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.net.InetSocketAddress;
import java.util.Optional;

@Mixin(MultiplayerServerListPinger.class)
public class MixinServerPing {
    ServerInfo entry;

    @Inject(
            method = "add",
            locals = LocalCapture.CAPTURE_FAILSOFT,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/ClientConnection;setPacketListener(Lnet/minecraft/network/listener/PacketListener;)V"
            )
    )
    private void captureEntry(
            ServerInfo entry, Runnable saver, CallbackInfo ci, ServerAddress serverAddress, Optional optional, InetSocketAddress inetSocketAddress, ClientConnection clientConnection) {
        this.entry = entry;
    }

    @ModifyArg(
            method = "add",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/ClientConnection;setPacketListener(Lnet/minecraft/network/listener/PacketListener;)V"
            )
    )
    private PacketListener enclosure$modifyServerName(PacketListener pl) {
        ClientQueryPacketListener listener = (ClientQueryPacketListener) pl;
        ServerInfo finalEntry = entry;
        return new ClientQueryPacketListener() {
            @Override
            public void onResponse(QueryResponseS2CPacket packet) {
                listener.onResponse(packet);
                ServerMetadataAccess metadata = (ServerMetadataAccess) packet.metadata();
                ServerMetadataAccess info = (ServerMetadataAccess) finalEntry;
                info.setModName(metadata.getModName());
                info.setModVersion(metadata.getModVersion());
            }

            @Override
            public void onPong(QueryPongS2CPacket packet) {
                listener.onPong(packet);
            }

            @Override
            public void onDisconnected(Text reason) {
                listener.onDisconnected(reason);
            }

            @Override
            public boolean isConnectionOpen() {
                return listener.isConnectionOpen();
            }
        };
    }
}
