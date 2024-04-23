package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static com.github.zly2006.enclosure.utils.Permission.permissions;

@Mixin(CrossbowItem.class)
public class MixinCrossBowItem {
    @Inject(method = "use", locals = LocalCapture.CAPTURE_FAILSOFT, at = @At(value = "INVOKE", target = "Lnet/minecraft/item/CrossbowItem;shootAll(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;FF)V"), cancellable = true)
    private void onShoot(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir, ItemStack itemStack) {
        if (user instanceof ServerPlayerEntity player) {
            if (!ServerMain.INSTANCE.checkPermission(player, permissions.SHOOT, player.getBlockPos())) {
                player.sendMessage(permissions.SHOOT.getNoPermissionMsg(player));
                player.currentScreenHandler.syncState();  // update player's inventory
                cir.setReturnValue(TypedActionResult.fail(itemStack));
            }
        }
    }
}
