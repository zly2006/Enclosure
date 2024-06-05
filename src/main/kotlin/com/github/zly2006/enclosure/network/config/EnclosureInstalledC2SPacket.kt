package com.github.zly2006.enclosure.network;

import com.github.zly2006.enclosure.ServerMainKt;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.github.zly2006.enclosure.ServerMainKt.MOD_VERSION;

public class EnclosureInstalledC2SPacket implements CustomPayload {
    public EnclosureInstalledC2SPacket(Version version) {
        this.version = version;
    }

    Version version;
    public static final Map<ServerPlayerEntity, Version> installedClientMod = new HashMap<>();

    public static final Id<EnclosureInstalledC2SPacket> ID = new Id<>(NetworkChannels.ENCLOSURE_INSTALLED);
    public static final PacketCodec<PacketByteBuf, EnclosureInstalledC2SPacket> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeString(value.version.getFriendlyString()),
            buf -> {
                try {
                    return new EnclosureInstalledC2SPacket(Version.parse(buf.readString()));
                } catch (VersionParsingException e) {
                    return new EnclosureInstalledC2SPacket(null);
                }
            }
    );

    public static boolean isInstalled(@Nullable ServerPlayerEntity player) {
        if (player == null) return false;
        return installedClientMod.containsKey(player);
    }

    public static Version clientVersion(ServerPlayerEntity connection) {
        return installedClientMod.get(connection);
    }

    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                installedClientMod.remove(handler.player));
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            Version version = payload.version;
            if (version instanceof SemanticVersion clientVersion && MOD_VERSION instanceof SemanticVersion serverVersion &&
                    clientVersion.getVersionComponent(0) == serverVersion.getVersionComponent(0) &&
                    clientVersion.getVersionComponent(1) >= serverVersion.getVersionComponent(1)) {
                ServerMainKt.LOGGER.info(context.player().getName().getString() + " joined with a matching enclosure client.");
                installedClientMod.put(context.player(), version);

                ServerPlayNetworking.send(context.player(), new UUIDCacheS2CPacket(ServerMainKt.minecraftServer.getUserCache()));
            } else {
                context.player().sendMessage(Text.translatable("enclosure.message.outdated", MOD_VERSION.getFriendlyString(), version.getFriendlyString()), false);
            }
        });
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
