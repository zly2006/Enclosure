package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.block.TntBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TntBlock.class)
public class MixinTntBlock {
    @Inject(method = "primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/LivingEntity;)V", at = @At("HEAD"), cancellable = true)
    private static void onUseInject(World world, BlockPos pos, LivingEntity igniter, CallbackInfo ci) {
        if (igniter instanceof ServerPlayerEntity player) {
            EnclosureArea area = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) world, pos);
            if (area != null && !area.hasPerm(player, Permission.PRIME_TNT)) {
                player.sendMessage(Permission.PRIME_TNT.getNoPermissionMsg(player));
                ci.cancel();
            }
        }
    }
}
