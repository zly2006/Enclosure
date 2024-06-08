package com.github.zly2006.enclosure.mixin.workaround;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinCPH {
//    @Redirect(
//            method = "onItemPickupAnimation",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lnet/minecraft/client/particle/ParticleManager;addParticle(Lnet/minecraft/client/particle/Particle;)V"
//            )
//    )
//    private void nothing(ParticleManager instance, Particle particle) {
//
//    }
}
