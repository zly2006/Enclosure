package com.github.zly2006.enclosure.client.mixin;

import com.github.zly2006.enclosure.ServerMainKt;
import com.github.zly2006.enclosure.access.ServerMetadataAccess;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(MultiplayerServerListWidget.ServerEntry.class)
public class MixinServerEntry {
    @Shadow @Final private MultiplayerScreen screen;
    @Shadow @Final private ServerInfo server;
    @Shadow @Final private MinecraftClient client;
    Identifier NOTIFY_TEXTURE = new Identifier("realms", "textures/gui/realms/trial_icon.png");
    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        ServerMetadataAccess access = (ServerMetadataAccess) server;
        if (ServerMainKt.MOD_ID.equals(access.getModName())) {
            int offset = 0;
            if (hovered) {
                if (mouseX > x + 24 && mouseX < x + 32 &&
                        mouseY > y + 24 && mouseY < y + 32) {
                    offset = 8;
                    screen.setMultiplayerScreenTooltip(
                            List.of(Text.of("This server has enclosure mod installed, version: " + access.getModVersion().getFriendlyString()))
                    );
                }
            }
            RenderSystem.setShaderTexture(0, NOTIFY_TEXTURE);
            RenderSystem.enableBlend();
            DrawableHelper.drawTexture(matrices, x + 24, y + 24, 0.0F, offset, 8, 8, 8, 16);
            RenderSystem.disableBlend();
        }
    }
}
