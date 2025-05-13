package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.zly2006.enclosure.utils.Permission.SHOOT;

@Mixin(CrossbowItem.class)
public class MixinCrossBowItem {
    @Inject(method = "use", at = @At(value = "INVOKE",target = "Lnet/minecraft/item/CrossbowItem;shootAll(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;FFLnet/minecraft/entity/LivingEntity;)V"), cancellable = true)
    private void onShoot(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir, @Local ItemStack itemStack) {
        if (user instanceof ServerPlayerEntity player) {
            if (!ServerMain.INSTANCE.checkPermission(player, SHOOT, player.getBlockPos())) {
                player.sendMessage(SHOOT.getNoPermissionMsg(player));
                player.currentScreenHandler.syncState();  // update player's inventory
                cir.setReturnValue(ActionResult.FAIL);
            }
        }
    }
}
