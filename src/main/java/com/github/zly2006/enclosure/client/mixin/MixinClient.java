package com.github.zly2006.enclosure.client.mixin;

import com.github.zly2006.enclosure.access.ClientAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MusicInstance;
import net.minecraft.sound.MusicSound;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MixinClient implements ClientAccess {
    @Unique @Nullable
    MusicSound musicSound;
    @Inject(
            method = "getMusicInstance",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;player:Lnet/minecraft/client/network/ClientPlayerEntity;", ordinal = 0),
            cancellable = true
    )
    private void modifyBgm(CallbackInfoReturnable<MusicInstance> cir) {
        if (musicSound != null) {
            cir.setReturnValue(new MusicInstance(musicSound));
        }
    }


    @Override
    public void enclosure$setBgm(MusicSound musicSound) {
        this.musicSound = musicSound;
    }

    @Override
    public MusicSound enclosure$getBgm() {
        return musicSound;
    }
}
