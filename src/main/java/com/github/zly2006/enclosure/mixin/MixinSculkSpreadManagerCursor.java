package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.block.BlockState;
import net.minecraft.block.SculkSpreadable;
import net.minecraft.block.entity.SculkSpreadManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(SculkSpreadManager.Cursor.class)
public class MixinSculkSpreadManagerCursor {
    @Inject(method = "canSpread(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private static void getSpreadPos(WorldAccess world, BlockPos sourcePos, BlockPos targetPos, CallbackInfoReturnable<Boolean> cir) {
        if (world instanceof ServerWorld serverWorld) {
            if (targetPos == null) {
                return;
            }
            EnclosureArea area = ServerMain.INSTANCE.getSmallestEnclosure(serverWorld, targetPos);
            if (area != null && !area.hasPubPerm(Permission.SCULK_SPREAD)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Redirect(method = "spread", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/SculkSpreadable;spread(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Ljava/util/Collection;Z)Z"))
    private boolean spread(SculkSpreadable instance, WorldAccess world, BlockPos pos, BlockState state, Collection<Direction> directions, boolean markForPostProcessing) {
        if (world instanceof ServerWorld serverWorld) {
            EnclosureArea area = ServerMain.INSTANCE.getSmallestEnclosure(serverWorld, pos);
            if (area != null && !area.hasPubPerm(Permission.SCULK_SPREAD)) {
                return false;
            }
        }
        return instance.spread(world, pos, state, directions, markForPostProcessing);
    }
}
