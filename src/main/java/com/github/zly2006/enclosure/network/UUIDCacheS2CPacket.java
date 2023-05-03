package com.github.zly2006.enclosure.network;

import com.github.zly2006.enclosure.client.ClientMain;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.zly2006.enclosure.ServerMainKt.LOGGER;

public class UUIDCacheS2CPacket implements ClientPlayNetworking.PlayChannelHandler {
    public static final Map<UUID, String> uuid2name = new HashMap<>();
    public static String getName(UUID uuid) {
        if (uuid2name.containsKey(uuid)) {
            return uuid2name.get(uuid);
        }
        return uuid.toString();
    }

    private UUIDCacheS2CPacket() {}
    @Override
    public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        NbtCompound compound = buf.readNbt();
        assert compound != null;
        compound.getKeys().forEach(key -> uuid2name.put(compound.getUuid(key), key));
        LOGGER.info("Received UUID cache from server.");
        ClientMain.isEnclosureInstalled = true;
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(NetworkChannels.SYNC_UUID, new UUIDCacheS2CPacket());
    }
}
