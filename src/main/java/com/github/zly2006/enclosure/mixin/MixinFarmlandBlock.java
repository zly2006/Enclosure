package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmlandBlock.class)
public class MixinFarmlandBlock {
    @Inject(method = "setToDirt", at = @At("HEAD"), cancellable = true)
    private static void onLandedUpon(Entity entity, BlockState state, World world, BlockPos pos, CallbackInfo ci) {
        if (entity instanceof PlayerEntity player) {
            if (!ServerMain.INSTANCE.checkPermission(world, pos, player, Permission.FARMLAND_DESTROY)) {
                player.sendMessage(Permission.FARMLAND_DESTROY.getNoPermissionMsg(player), ServerMain.INSTANCE.getCommonConfig().useActionBarMessage);
                ci.cancel();
                return;
            }
        }

        EnclosureArea area = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) entity.getWorld(), pos);
        if (area == null) return;
        if (area.hasPubPerm(Permission.FARMLAND_DESTROY)) return;
        ci.cancel();
    }
}
