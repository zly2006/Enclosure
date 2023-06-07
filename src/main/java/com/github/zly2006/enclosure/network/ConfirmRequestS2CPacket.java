package com.github.zly2006.enclosure.network;

import com.github.zly2006.enclosure.gui.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

public class ConfirmRequestS2CPacket implements ClientPlayNetworking.PlayChannelHandler {
    @Override
    public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        if (client.currentScreen instanceof EnclosureGui) {
            client.execute(() -> {
                responseSender.sendPacket(NetworkChannels.CONFIRM, PacketByteBufs.empty());
                client.setScreen(new ConfirmScreen(client.currentScreen, buf.readText(), () -> {
                    assert client.player != null;
                    client.player.networkHandler.sendCommand("enclosure give $fullName $uuid");
                }));
            });
        }
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(NetworkChannels.CONFIRM, new ConfirmRequestS2CPacket());
    }
}
