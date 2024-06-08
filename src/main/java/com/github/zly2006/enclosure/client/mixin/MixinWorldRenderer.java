package com.github.zly2006.enclosure.client.mixin;

import com.github.zly2006.enclosure.client.ClientMain;
import com.github.zly2006.enclosure.client.EnclosureWorldRenderer;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {
    @Shadow @Final private MinecraftClient client;

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw(Lnet/minecraft/client/render/RenderLayer;)V",
                    shift = At.Shift.AFTER
            ),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/render/RenderLayer;getWaterMask()Lnet/minecraft/client/render/RenderLayer;",
                            ordinal = 0
                    ),
                    to = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw()V",
                            ordinal = 0
                    )
            ),
            allow = 1
    )
    private void onLastRender1(CallbackInfo ci, @Local MatrixStack matrixStack, @Local Vec3d vec3d) {
        render(matrixStack, vec3d);
    }

    @Unique
    private void render(@Local MatrixStack matrixStack, @Local Vec3d vec3d) {
        if (client.options.hudHidden ||true) return;
        var session = ClientMain.Companion.getClientSession();
        if (session == null) {
            return;
        }
        RenderSystem.enableBlend();
        EnclosureWorldRenderer.INSTANCE.drawSessionFaces(matrixStack, session, vec3d);
        RenderSystem.disableBlend();
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;renderLayer(Lnet/minecraft/client/render/RenderLayer;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
                    shift = At.Shift.AFTER
            ),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/render/RenderLayer;getTripwire()Lnet/minecraft/client/render/RenderLayer;",
                            ordinal = 1
                    ),
                    to = @At("TAIL")
            ),
            allow = 1
    )
    private void onLastRender2(CallbackInfo ci, @Local MatrixStack matrixStack, @Local Vec3d vec3d) {
        render(matrixStack, vec3d);
    }
}
