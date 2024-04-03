package com.github.zly2006.enclosure.mixin.workaround.fuckMajong;

import com.github.zly2006.enclosure.network.EnclosureInstalledC2SPacket;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(CustomPayloadC2SPacket.class)
public class MixinC2S {
    @Inject(
            method = "method_58271",
            at = @At("HEAD")
    )
    private static void inject(ArrayList<CustomPayload.Type<?, ?>> types, CallbackInfo ci) {
        types.add(new CustomPayload.Type<>(EnclosureInstalledC2SPacket.Companion.getID(), EnclosureInstalledC2SPacket.Companion.getCODEC()));
    }
}
