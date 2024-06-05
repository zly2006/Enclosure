package com.github.zly2006.enclosure.network;

import com.github.zly2006.enclosure.gui.ConfirmScreen;
import com.github.zly2006.enclosure.gui.EnclosureGui;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

public class ConfirmRequestS2CPacket implements CustomPayload {
    final Text text;
    public static final Id<ConfirmRequestS2CPacket> ID = new Id<>(NetworkChannels.CONFIRM);
    public static final PacketCodec<PacketByteBuf, ConfirmRequestS2CPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                TextCodecs.PACKET_CODEC.encode(buf, value.text);
            },
            buf -> new ConfirmRequestS2CPacket(TextCodecs.PACKET_CODEC.decode(buf))
    );

    public ConfirmRequestS2CPacket(Text text) {
        this.text = text;
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            var client = MinecraftClient.getInstance();
            if (client.currentScreen instanceof EnclosureGui) {
                Text message = payload.text;
                client.execute(() -> {
                    context.responseSender().sendPacket(new ConfirmRequestS2CPacket(Text.empty()));
                    client.setScreen(new ConfirmScreen(client.currentScreen, message, () -> {
                        assert client.player != null;
                        client.player.networkHandler.sendCommand("enclosure confirm");
                    }));
                });
            }
        });
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
