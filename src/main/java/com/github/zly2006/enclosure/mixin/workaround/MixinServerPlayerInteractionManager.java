package com.github.zly2006.enclosure.mixin.workaround;

import com.github.zly2006.enclosure.access.MiningStatusAccess;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class MixinServerPlayerInteractionManager implements MiningStatusAccess {
    boolean miningSuccess = false;
    @Inject(method = "method_41250", at = @At("HEAD"))
    private void afterBreakingBlock(BlockPos pos, boolean success, int sequence, String reason, CallbackInfo ci) {
        miningSuccess = success;
    }

    @Override
    public boolean success() {
        return miningSuccess;
    }
}
