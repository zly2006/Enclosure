package com.github.zly2006.enclosure.network;

import com.github.zly2006.enclosure.command.ConfirmManager;
import com.github.zly2006.enclosure.gui.PermissionScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.UUID;

public class SyncPermissionBiPacket implements CustomPayload {
    public UUID uuid;
    public NbtCompound permission;
    public static final Id<SyncPermissionBiPacket> ID = new Id<>(NetworkChannels.SYNC_PERMISSION);
    public static final PacketCodec<PacketByteBuf, SyncPermissionBiPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeUuid(value.uuid);
                buf.writeNbt(value.permission);
            },
            buf -> {
                SyncPermissionBiPacket packet = new SyncPermissionBiPacket();
                packet.uuid = buf.readUuid();
                packet.permission = buf.readNbt();
                return packet;
            }
    );
    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            if (MinecraftClient.getInstance().currentScreen instanceof PermissionScreen screen) {
                if (screen.uuid.equals(payload.uuid)) {
                    if (payload.permission != null) {
                        screen.syncPermission(payload.permission);
                    }
                }
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            ConfirmManager.INSTANCE.getPendingMap().remove(context.player().getUuid());
        });
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return null;
    }
}
