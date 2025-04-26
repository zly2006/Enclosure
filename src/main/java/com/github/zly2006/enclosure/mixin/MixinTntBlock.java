package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.block.BlockState;
import net.minecraft.block.TntBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TntBlock.class)
public class MixinTntBlock {
    @Inject(method = "onUseWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/TntBlock;primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/LivingEntity;)V"), cancellable = true)
    private void onUseInject(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            EnclosureArea area = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) world, pos);
            if (area != null && !area.hasPerm(serverPlayer, Permission.PRIME_TNT)) {
                player.sendMessage(Permission.PRIME_TNT.getNoPermissionMsg(player), true);
                cir.setReturnValue(ActionResult.FAIL);
            }
        }
    }
    @Inject(method = "onProjectileHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/TntBlock;primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/LivingEntity;)V"), cancellable = true)
    private void onProjectileHitInject(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile, CallbackInfo ci) {
        EnclosureArea area = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) world, hit.getBlockPos());
        if (projectile.getOwner() instanceof ServerPlayerEntity player) {
            if (area != null && !area.hasPerm(player, Permission.PRIME_TNT)) {
                player.sendMessage(Permission.PRIME_TNT.getNoPermissionMsg(player));
                ci.cancel();
            }
        }
    }
}
