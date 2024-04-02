package com.github.zly2006.enclosure.client.mixin;

import com.github.zly2006.enclosure.ServerMainKt;
import com.github.zly2006.enclosure.access.ServerMetadataAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerServerListWidget.ServerEntry.class)
public class MixinServerEntry {
    @Shadow @Final private MultiplayerScreen screen;
    @Shadow @Final private ServerInfo server;
    @Shadow @Final private MinecraftClient client;
    Identifier NOTIFY_TEXTURE = new Identifier("realms", "textures/gui/realms/trial_icon.png");
    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        ServerMetadataAccess access = (ServerMetadataAccess) server;
        if (ServerMainKt.MOD_ID.equals(access.getModName())) {
            int offset = 0;
            if (hovered) {
                if (mouseX > x + 24 && mouseX < x + 32 &&
                        mouseY > y + 24 && mouseY < y + 32) {
                    offset = 8;
                    screen.setTooltip(
                            Text.of("This server has enclosure mod installed, version: " + access.getModVersion().getFriendlyString())
                    );
                }
            }
            context.drawTexture(NOTIFY_TEXTURE, x + 24, y + 24, 0, offset, 8, 8, 8, 16);
        }
    }
}
