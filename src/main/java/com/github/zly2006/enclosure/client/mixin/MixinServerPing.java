package com.github.zly2006.enclosure.client.mixin;

import com.github.zly2006.enclosure.access.ServerMetadataAccess;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.listener.ClientQueryPacketListener;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MultiplayerServerListPinger.class)
public class MixinServerPing {
    @Unique ServerInfo entry;

    @Inject(
            method = "add",
            locals = LocalCapture.CAPTURE_FAILSOFT,
            at = @At("HEAD")
    )
    private void captureEntry(ServerInfo entry, Runnable saver, Runnable pingCallback, CallbackInfo ci) {
        this.entry = entry;
    }

    @ModifyArg(
            method = "add",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/ClientConnection;connect(Ljava/lang/String;ILnet/minecraft/network/listener/ClientQueryPacketListener;)V"
            )
    )
    private ClientQueryPacketListener enclosure$modifyServerName(ClientQueryPacketListener pl) {
        ServerInfo finalEntry = entry;
        return new ClientQueryPacketListener() {
            @Override
            public void onPingResult(PingResultS2CPacket packet) {

            }

            @Override
            public void onResponse(QueryResponseS2CPacket packet) {
                pl.onResponse(packet);
                ServerMetadataAccess metadata = (ServerMetadataAccess) packet.metadata();
                ServerMetadataAccess info = (ServerMetadataAccess) finalEntry;
                info.setModName(metadata.getModName());
                info.setModVersion(metadata.getModVersion());
            }

            @Override
            public void onDisconnected(Text reason) {
                pl.onDisconnected(reason);
            }

            @Override
            public boolean isConnectionOpen() {
                return pl.isConnectionOpen();
            }
        };
    }
}
