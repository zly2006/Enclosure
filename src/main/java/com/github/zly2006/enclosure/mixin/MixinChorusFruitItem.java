package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Utils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static com.github.zly2006.enclosure.utils.Permission.TELEPORT;

@Mixin(Item.class)
public class MixinChorusFruitItem {
//    @Inject(method = "finishUsing", at = @At("HEAD"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
//    private void tp(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
//        if (user instanceof ServerPlayerEntity player) {
//            BlockPos pos = Utils.toBlockPos(d, e, f);
//            if (!ServerMain.INSTANCE.checkPermission(player, TELEPORT, pos)) {
//                player.sendMessage(TELEPORT.getNoPermissionMsg(player));
//                cir.setReturnValue(stack);
//            }
//        }
//    }
}
