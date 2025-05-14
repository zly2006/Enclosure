package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.zly2006.enclosure.utils.Permission.SHOOT;

@Mixin(BowItem.class)
public class MixinBowItem {
    @Inject(at = @At("HEAD"), method = "onStoppedUsing", cancellable = true)
    public void checkBowPermission(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (user instanceof ServerPlayerEntity player) {
            if (!ServerMain.INSTANCE.checkPermission(player, SHOOT, player.getBlockPos())) {
                player.sendMessage(SHOOT.getNoPermissionMsg(player), true);
                player.currentScreenHandler.syncState();  // update player's inventory
                cir.cancel();
            }
        }
    }
}
