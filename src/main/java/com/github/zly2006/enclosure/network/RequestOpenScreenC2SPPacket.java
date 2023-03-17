package com.github.zly2006.enclosure.network;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.events.PaidPartEvents;
import com.github.zly2006.enclosure.utils.TrT;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import static com.github.zly2006.enclosure.ServerMain.Instance;
import static com.github.zly2006.enclosure.ServerMain.minecraftServer;

// 请求服务器的领地信息，
public class RequestOpenScreenC2SPPacket implements ServerPlayNetworking.PlayChannelHandler {
    public static void register() {
        RequestOpenScreenC2SPPacket listener = new RequestOpenScreenC2SPPacket();
        ServerPlayNetworking.registerGlobalReceiver(NetworkChannels.OPEN_REQUEST, listener);
    }

    public static void send(MinecraftClient client, String name) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(name);
        assert client.player != null;
        buf.writeIdentifier(client.player.getWorld().getRegistryKey().getValue());
        buf.writeIntArray(new int[]{
                client.player.getBlockPos().getX(),
                client.player.getBlockPos().getY(),
                client.player.getBlockPos().getZ()
        });
        ClientPlayNetworking.send(NetworkChannels.OPEN_REQUEST, buf);
    }

    @Override
    public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        String name = buf.readString();
        Identifier dimId = buf.readIdentifier();
        int[] pos = buf.readIntArray();
        BlockPos blockPos = new BlockPos(pos[0], pos[1], pos[2]);
        EnclosureArea area;
        if (EnclosureInstalledC2SPacket.isInstalled(player)) {
            if (name.isEmpty()) {
                ServerWorld world = minecraftServer.getWorld(RegistryKey.of(Registry.WORLD_KEY, dimId));
                if (world == null) {
                    player.sendMessage(TrT.of("enclosure.message.no_enclosure"), false);
                    return;
                }
                area = Instance.getAllEnclosures(world).getArea(blockPos);
                if (area == null) {
                    player.sendMessage(TrT.of("enclosure.message.no_enclosure"), false);
                    return;
                }
                area = area.areaOf(blockPos);
            }
            else {
                area = Instance.getEnclosure(name);
                if (area == null) {
                    player.sendMessage(TrT.of("enclosure.message.no_enclosure"), false);
                    return;
                }
            }
            PaidPartEvents.INSTANCE.open(player, area);
        }
    }
}
