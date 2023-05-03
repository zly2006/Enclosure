package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Utils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ChorusFruitItem;
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

@Mixin(ChorusFruitItem.class)
public class MixinChorusFruitItem {
    @Inject(method = "finishUsing", locals = LocalCapture.CAPTURE_FAILSOFT, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;teleport(DDDZ)Z"), cancellable = true)
    private void tp(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir, ItemStack itemStack, double d, double e, double f, int i, double g, double h, double j, Vec3d vec3d) {
        if (user instanceof ServerPlayerEntity player) {
            BlockPos pos = Utils.toBlockPos(g, h, j);
            if (!ServerMain.INSTANCE.checkPermission(player, TELEPORT, pos)) {
                player.sendMessage(TELEPORT.getNoPermissionMsg(player));
                cir.setReturnValue(stack);
            }
        }
    }
}
