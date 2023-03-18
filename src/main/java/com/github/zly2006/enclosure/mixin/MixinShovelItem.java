package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.EnclosureList;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.ShovelItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.zly2006.enclosure.utils.Permission.USE_CAMPFIRE;
import static net.minecraft.block.CampfireBlock.LIT;
import static net.minecraft.block.CampfireBlock.WATERLOGGED;

@Mixin(ShovelItem.class)
public class MixinShovelItem {
    @Inject(method = "useOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/CampfireBlock;extinguish(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V", shift = At.Shift.BEFORE))
    void useOnBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        World world = context.getWorld();
        BlockPos blockPos = context.getBlockPos();
        BlockState blockState = world.getBlockState(blockPos);
        if (context.getWorld() instanceof ServerWorld) {
            EnclosureList list = ServerMain.Instance.getAllEnclosures((ServerWorld) world);
            EnclosureArea area = list.getArea(context.getBlockPos());
            if (area != null && !area.areaOf(context.getBlockPos()).hasPubPerm(Permission.USE_CAMPFIRE)) {
                if (context.getPlayer() instanceof ServerPlayerEntity player) {
                    player.sendMessage(USE_CAMPFIRE.getNoPermissionMsg(player), false);
                }
                world.setBlockState(blockPos, blockState.with(LIT, true), 252);
                cir.setReturnValue(ActionResult.PASS);
            }
        }
    }
}
