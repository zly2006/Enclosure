package com.github.zly2006.enclosure.network;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.gui.EnclosureScreenHandler;
import com.github.zly2006.enclosure.utils.TrT;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import static com.github.zly2006.enclosure.ServerMainKt.minecraftServer;

// 请求服务器的领地信息，
public class RequestOpenScreenC2SPPacket implements CustomPayload {
    String name;
    Identifier dimId;
    int[] pos;

    public static final Id<RequestOpenScreenC2SPPacket> ID = new Id<>(NetworkChannels.OPEN_REQUEST);
    public static final PacketCodec<PacketByteBuf, RequestOpenScreenC2SPPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeString(value.name);
                buf.writeIdentifier(value.dimId);
                buf.writeIntArray(value.pos);
            },
            buf -> {
                RequestOpenScreenC2SPPacket packet = new RequestOpenScreenC2SPPacket();
                packet.name = buf.readString();
                packet.dimId = buf.readIdentifier();
                packet.pos = buf.readIntArray();
                return packet;
            }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            BlockPos blockPos = new BlockPos(payload.pos[0], payload.pos[1], payload.pos[2]);
            EnclosureArea area;
            if (EnclosureInstalledC2SPacket.isInstalled(context.player())) {
                if (payload.name.isEmpty()) {
                    ServerWorld world = minecraftServer.getWorld(RegistryKey.of(RegistryKeys.WORLD, payload.dimId));
                    if (world == null) {
                        context.player().sendMessage(TrT.of("enclosure.message.no_enclosure"));
                        return;
                    }
                    area = ServerMain.INSTANCE.getSmallestEnclosure(world, blockPos);
                }
                else {
                    area = ServerMain.INSTANCE.getEnclosure(payload.name);
                }
                if (area == null) {
                    context.player().sendMessage(TrT.of("enclosure.message.no_enclosure"));
                    return;
                }
                EnclosureScreenHandler.open(context.player(), area);
            }
        });
    }

    public static void send(MinecraftClient client, String name) {
        var packet = new RequestOpenScreenC2SPPacket();
        packet.name = name;
        packet.dimId = client.player.getWorld().getRegistryKey().getValue();
        packet.pos = new int[]{
                client.player.getBlockPos().getX(),
                client.player.getBlockPos().getY(),
                client.player.getBlockPos().getZ()
        };
        ClientPlayNetworking.send(packet);
    }

}
