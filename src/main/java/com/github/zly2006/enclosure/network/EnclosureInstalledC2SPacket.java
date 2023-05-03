package com.github.zly2006.enclosure.network;

import com.github.zly2006.enclosure.ServerMainKt;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.github.zly2006.enclosure.ServerMainKt.MOD_VERSION;

public class EnclosureInstalledC2SPacket implements ServerPlayNetworking.PlayChannelHandler {
    public static final Map<ServerPlayerEntity, Version> installedClientMod = new HashMap<>();

    private EnclosureInstalledC2SPacket() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                installedClientMod.remove(handler.player));
    }

    public static boolean isInstalled(@Nullable ServerPlayerEntity player) {
        if (player == null) return false;
        return installedClientMod.containsKey(player);
    }

    public static Version clientVersion(ServerPlayerEntity connection) {
        return installedClientMod.get(connection);
    }

    public static void register() {
        EnclosureInstalledC2SPacket listener = new EnclosureInstalledC2SPacket();
        ServerPlayNetworking.registerGlobalReceiver(NetworkChannels.ENCLOSURE_INSTALLED, listener);
    }

    public static void send() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(MOD_VERSION.getFriendlyString());
        ClientPlayNetworking.send(NetworkChannels.ENCLOSURE_INSTALLED, buf);
    }

    @Override
    public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, @NotNull PacketByteBuf buf, PacketSender responseSender) {
        Version version;
        try {
            version = Version.parse(buf.readString());
            if (version instanceof SemanticVersion clientVersion && MOD_VERSION instanceof SemanticVersion serverVersion &&
                    clientVersion.getVersionComponent(0) == serverVersion.getVersionComponent(0) &&
                    clientVersion.getVersionComponent(1) == serverVersion.getVersionComponent(1)) {
                ServerMainKt.LOGGER.info(player.getName().getString() + " joined with a matching enclosure client.");
                installedClientMod.put(player, version);

                // send uuid data
                PacketByteBuf buf2 = PacketByteBufs.create();
                NbtCompound compound = new NbtCompound();
                ServerMainKt.byUuid.forEach((uuid, s) -> compound.putUuid(s, uuid));
                buf2.writeNbt(compound);
                compound.put("", new NbtCompound());
                ServerPlayNetworking.send(player, NetworkChannels.SYNC_UUID, buf2);
            } else {
                player.sendMessage(Text.translatable("enclosure.message.outdated", MOD_VERSION.getFriendlyString(), version.getFriendlyString()), false);
            }
        } catch (VersionParsingException ignored) { }
    }
}
