package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPointerImpl;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(DispenserBlock.class)
public class MixinDispenserBlock {
    @Inject(method = "dispense", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/dispenser/DispenserBehavior;dispense(Lnet/minecraft/util/math/BlockPointer;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void onDispense(ServerWorld world, BlockPos pos, CallbackInfo ci, BlockPointerImpl blockPointerImpl, DispenserBlockEntity dispenserBlockEntity, int i, ItemStack itemStack, DispenserBehavior dispenserBehavior) {
        Direction facing = blockPointerImpl.getBlockState().get(DispenserBlock.FACING);
        EnclosureArea area = ServerMain.INSTANCE.getSmallestEnclosure(world, pos);
        EnclosureArea area2 = ServerMain.INSTANCE.getSmallestEnclosure(world, pos.offset(facing));
        if (area2 != area && area2 != null &&
                !area2.hasPubPerm(Permission.DISPENSER)) {
            if (dispenserBehavior.getClass() != ItemDispenserBehavior.class) {
                ci.cancel(); // we only allow default ItemDispenserBehavior
            }
        }
    }
}
