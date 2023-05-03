package com.github.zly2006.enclosure.network;

import com.github.zly2006.enclosure.client.ClientMain;
import com.github.zly2006.enclosure.command.Session;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class SyncSelectionS2CPacket implements ClientPlayNetworking.PlayChannelHandler {
    private SyncSelectionS2CPacket() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(NetworkChannels.SYNC_SELECTION, new SyncSelectionS2CPacket());
    }

    @Override
    public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        BlockPos pos1 = buf.readBlockPos();
        BlockPos pos2 = buf.readBlockPos();
        if (ClientMain.clientSession == null) {
            ClientMain.clientSession = new Session(null);
        }
        ClientMain.clientSession.setPos1(pos1);
        ClientMain.clientSession.setPos2(pos2);
    }
}
