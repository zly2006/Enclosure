package com.github.zly2006.enclosure.network;

import com.github.zly2006.enclosure.client.ClientMain;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.UserCache;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.zly2006.enclosure.ServerMainKt.LOGGER;

public class UUIDCacheS2CPacket implements CustomPayload {
    Map<UUID, String> uuid2name = new HashMap<>();
    public static final Id<UUIDCacheS2CPacket> ID = new Id<>(NetworkChannels.SYNC_UUID);
    public static final PacketCodec<PacketByteBuf, UUIDCacheS2CPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeMap(value.uuid2name, (b, uuid) -> buf.writeUuid(uuid), PacketByteBuf::writeString);
            },
            buf -> {
                UUIDCacheS2CPacket packet = new UUIDCacheS2CPacket();
                packet.uuid2name = buf.readMap(b -> b.readUuid(), PacketByteBuf::readString);
                return packet;
            }
    );

    public UUIDCacheS2CPacket() { }

    public UUIDCacheS2CPacket(UserCache userCache) {
        userCache.byName.forEach((name, entry) -> uuid2name.put(entry.getProfile().getId(), name));
    }

    public static String getName(UUID uuid) {
        if (ClientMain.uuid2name.containsKey(uuid)) {
            return ClientMain.uuid2name.get(uuid);
        }
        return uuid.toString();
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            ClientMain.uuid2name = payload.uuid2name;
            LOGGER.info("Received UUID cache from server.");
            ClientMain.isEnclosureInstalled = true;
        });
    }
}
