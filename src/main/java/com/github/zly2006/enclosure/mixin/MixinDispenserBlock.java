package com.github.zly2006.enclosure.mixin;

import net.minecraft.block.DispenserBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DispenserBlock.class)
public class MixinDispenserBlock {
    @Inject(method = "dispense", at = @At("HEAD"))
    private void omDispense(ServerWorld world, BlockPos pos, CallbackInfo ci) {
        // todo
    }
}
