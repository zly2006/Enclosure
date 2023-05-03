package com.github.zly2006.enclosure.client;

import com.github.zly2006.enclosure.command.ClientSession;
import com.github.zly2006.enclosure.gui.EnclosureScreen;
import com.github.zly2006.enclosure.gui.EnclosureScreenHandler;
import com.github.zly2006.enclosure.network.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class ClientMain implements ClientModInitializer {
    public static ClientConfig config = new ClientConfig();
    public final static KeyBinding openScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "enclosure.key.open_screen",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "enclosure.key._category"
    ));
    public static ClientSession clientSession;
    public static boolean isEnclosureInstalled = false;

    @Override
    public void onInitializeClient() {
        EnclosureScreenHandler.register();
        UUIDCacheS2CPacket.register();
        SyncSelectionS2CPacket.register();
        ConfirmRequestS2CPacket.register();
        HandledScreens.register(EnclosureScreenHandler.ENCLOSURE_SCREEN_HANDLER, EnclosureScreen::new);
        EnclosureWorldRenderer.INSTANCE.register();
        SyncPermissionS2CPacket.register();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            clientSession = new ClientSession();
            EnclosureInstalledC2SPacket.send();
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
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openScreenKey.wasPressed()) {
                RequestOpenScreenC2SPPacket.send(client, "");
            }
        });
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
        });
    }
}
