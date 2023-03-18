package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.access.MiningStatusAccess;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class MixinServerPlayerInteractionManager implements MiningStatusAccess {
    boolean miningSuccess = true;
    @Inject(method = "processBlockBreakingAction", at = @At(value = "INVOKE", shift = At.Shift.AFTER,target = "Lnet/minecraft/network/packet/s2c/play/PlayerActionResponseS2CPacket;<init>(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/network/packet/c2s/play/PlayerActionC2SPacket$Action;ZLjava/lang/String;)V"),locals = LocalCapture.CAPTURE_FAILSOFT)
    private void afterBreakingBlock(BlockPos pos, PlayerActionC2SPacket.Action action, Direction direction, int worldHeight, CallbackInfo ci, BlockPos pos1, BlockState state, PlayerActionC2SPacket.Action action1, boolean approved, String reason) {
        miningSuccess = approved;
    }

    @Override
    public boolean success() {
        return miningSuccess;
    }
}
