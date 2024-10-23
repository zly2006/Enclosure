package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowableFluid.class)
public abstract class MixinFlowableFluid {
    @Inject(method = "canFlowDownTo", at = @At("HEAD"), cancellable = true)
    private void protectFluid(BlockView world, BlockPos pos, BlockState state, BlockPos fromPos, BlockState fromState, CallbackInfoReturnable<Boolean> cir) {
        if (world instanceof ServerWorld serverWorld) {
            EnclosureArea from = ServerMain.INSTANCE.getSmallestEnclosure(serverWorld, fromPos);
            EnclosureArea to = ServerMain.INSTANCE.getSmallestEnclosure(serverWorld, pos);
            if (to != null && to != from) {
                if (!to.hasPubPerm(Permission.FLUID)) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
