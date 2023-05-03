package com.github.zly2006.enclosure.network;

import com.github.zly2006.enclosure.gui.EnclosureScreen;
import com.github.zly2006.enclosure.gui.PermissionScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

public class ConfirmRequestS2CPacket implements ClientPlayNetworking.PlayChannelHandler {
    @Override
    public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        if (client.currentScreen instanceof EnclosureScreen screen) {
            screen.requestConfirm(buf.readText());
        }
        if (client.currentScreen instanceof PermissionScreen screen) {
            screen.requestConfirm(buf.readText());
        }
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(NetworkChannels.CONFIRM, new ConfirmRequestS2CPacket());
    }
}
