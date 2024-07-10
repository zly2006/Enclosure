package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeveledCauldronBlock.class)
public class MixinLeveledCauldronBlock {
    @Inject(method = "onFireCollision", at = @At("HEAD"), cancellable = true)
    private void onFireCollision(BlockState state, World world, BlockPos pos, CallbackInfo ci){
        if (world instanceof ServerWorld serverWorld) {
            EnclosureArea area = ServerMain.INSTANCE.getSmallestEnclosure(serverWorld, pos);
            if (area != null && !area.hasPubPerm(Permission.CONSUMPTIVELY_EXTINGUISH)) {
                ci.cancel();
            }
        }
    }
}
