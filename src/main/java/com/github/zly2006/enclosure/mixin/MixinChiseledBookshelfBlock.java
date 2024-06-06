package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChiseledBookshelfBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

import static com.github.zly2006.enclosure.utils.Permission.CONTAINER;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Mixin(ChiseledBookshelfBlock.class)
public class MixinChiseledBookshelfBlock {
    @Inject(
            method = "onUse",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/OptionalInt;isEmpty()Z"
            ),
            cancellable = true
    )
    private void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir, @Local OptionalInt optionalInt) {
        if (optionalInt.isPresent() && !ServerMain.INSTANCE.checkPermission(world, pos, player, Permission.CONTAINER)) {
            player.sendMessage(CONTAINER.getNoPermissionMsg(player));
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(
            method = "onUseWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/OptionalInt;isEmpty()Z"
            ),
            cancellable = true
    )
    private void onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ItemActionResult> cir, @Local OptionalInt optionalInt) {
        if (optionalInt.isPresent() && !ServerMain.INSTANCE.checkPermission(world, pos, player, Permission.CONTAINER)) {
            player.sendMessage(CONTAINER.getNoPermissionMsg(player));
            cir.setReturnValue(ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);
        }
    }
}
