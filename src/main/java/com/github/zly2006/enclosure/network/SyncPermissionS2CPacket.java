package com.github.zly2006.enclosure.network;

import com.github.zly2006.enclosure.gui.PermissionScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;

import java.util.UUID;

public class SyncPermissionS2CPacket implements ClientPlayNetworking.PlayChannelHandler {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(NetworkChannels.SYNC_PERMISSION, new SyncPermissionS2CPacket());
    }
    @Override
    public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        UUID uuid = buf.readUuid();
        NbtCompound permission = buf.readNbt();
        if (client.currentScreen instanceof PermissionScreen screen) {
            if (screen.uuid.equals(uuid)) {
                screen.syncPermission(permission);
            }
        }
    }
}
