package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.mixinadatper.MixinPistonBlockKt;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.block.Blocks;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PistonBlock.class)
public class MixinPistonBlock extends FacingBlock {
    protected MixinPistonBlock(Settings settings) {
        super(settings);
    }

    @Inject(
            method = "move",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;",
                    remap = false
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            cancellable = true
    )
    private void protectPiston(World world, BlockPos pos, Direction dir, boolean retract, CallbackInfoReturnable<Boolean> cir, BlockPos blockPos, PistonHandler pistonHandler) {
        if (world instanceof ServerWorld serverWorld) {
            MixinPistonBlockKt.protectPiston(serverWorld, pos, dir, retract, cir, pistonHandler);
        }
    }

    // Fix for iron-head bedrock breaking
    @Redirect(method = "onSyncedBlockEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    private boolean protectBlockEvent(World world, BlockPos pos, boolean move) {
        if (world instanceof ServerWorld serverWorld && !serverWorld.getBlockState(pos).isOf(Blocks.PISTON_HEAD)) {
            if (!ServerMain.INSTANCE.checkPermission(serverWorld, pos, null, Permission.BREAK_BLOCK)) {
                serverWorld.getChunkManager().markForUpdate(pos);
                return false;
            }
        }
        return world.removeBlock(pos, move);
    }
}
