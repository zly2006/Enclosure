package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import net.minecraft.block.ChestBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlock.class)
public class MixinChestBlock {
    @Inject(method = "getNeighborChestDirection", at = @At("HEAD"), cancellable = true)
    private void getNeighborChestDirection(ItemPlacementContext ctx, Direction dir, CallbackInfoReturnable<Direction> cir) {
        EnclosureArea area1 = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) ctx.getWorld(), ctx.getBlockPos());
        EnclosureArea area2 = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) ctx.getWorld(), ctx.getBlockPos().offset(dir));
        if (area1 != area2) {
            cir.setReturnValue(null);
        }
    }
}
