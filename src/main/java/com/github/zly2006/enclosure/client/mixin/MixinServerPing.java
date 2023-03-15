package com.github.zly2006.enclosure.client.mixin;

import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.network.listener.ClientQueryPacketListener;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.s2c.query.QueryPongS2CPacket;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(MultiplayerServerListPinger.class)
public class MixinServerPing {
    @ModifyArg(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;setPacketListener(Lnet/minecraft/network/listener/PacketListener;)V"))
    private PacketListener enclosure$modifyServerName(PacketListener pl) {
        ClientQueryPacketListener listener = (ClientQueryPacketListener) pl;
        return new ClientQueryPacketListener() {
            @Override
            public void onResponse(QueryResponseS2CPacket packet) {
                listener.onResponse(packet);
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
