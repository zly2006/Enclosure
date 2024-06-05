package com.github.zly2006.enclosure.network;

import com.github.zly2006.enclosure.client.ClientMain;
import com.github.zly2006.enclosure.command.Session;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class SyncSelectionS2CPacket implements CustomPayload {
    public SyncSelectionS2CPacket(BlockPos pos1, BlockPos pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    BlockPos pos1;
    BlockPos pos2;

    public static final Id<SyncSelectionS2CPacket> ID = new Id<>(NetworkChannels.SYNC_SELECTION);
    public static final PacketCodec<PacketByteBuf, SyncSelectionS2CPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeBlockPos(value.pos1);
                buf.writeBlockPos(value.pos2);
            },
            buf -> new SyncSelectionS2CPacket(buf.readBlockPos(), buf.readBlockPos())
    );

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            if (ClientMain.clientSession == null) {
                ClientMain.clientSession = new Session(null);
            }
            ClientMain.clientSession.setPos1(payload.pos1);
            ClientMain.clientSession.setPos2(payload.pos2);
        });
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
