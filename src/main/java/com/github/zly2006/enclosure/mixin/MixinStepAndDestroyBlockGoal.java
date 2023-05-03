package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.entity.ai.goal.StepAndDestroyBlockGoal;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StepAndDestroyBlockGoal.class)
public class MixinStepAndDestroyBlockGoal {
    @Inject(method = "tweakToProperPos", at = @At("RETURN"), cancellable = true)
    private void onRemoveBlock(BlockPos blockPos, BlockView world, CallbackInfoReturnable<BlockPos> cir) {
        if (cir.getReturnValue() != null && world instanceof ServerWorld serverWorld) {
            BlockPos pos = cir.getReturnValue();
            EnclosureArea area = ServerMain.INSTANCE.getAllEnclosures(serverWorld).getArea(pos);
            if (area != null && !area.areaOf(pos).hasPubPerm(Permission.BREAK_TURTLE_EGG)) {
                cir.setReturnValue(null);
            }
        }
    }
}
