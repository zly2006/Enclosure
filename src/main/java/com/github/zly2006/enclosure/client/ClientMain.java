package com.github.zly2006.enclosure.client;

import com.github.zly2006.enclosure.command.ClientSession;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class ClientMain implements ClientModInitializer {
    public static ClientSession clientSession;
    public static boolean isEnclosureInstalled = false;

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            clientSession = new ClientSession();
            if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                isEnclosureInstalled = true;
                clientSession.setPos1(new BlockPos(0, 0, 0));
                clientSession.setPos2(new BlockPos(0, 0, 0));
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            isEnclosureInstalled = false;
            clientSession = null;
        });
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
        });
    }
}
