package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ExplosionBehavior.class)
public abstract class MixinExplosion {
    @Inject(
            method = "canDestroyBlock",
            at = @At("HEAD"),
            cancellable = true
    )
    private void canDestroyBlock(Explosion explosion, BlockView world, BlockPos pos, BlockState state, float power, CallbackInfoReturnable<Boolean> cir) {
        if (world instanceof ServerWorld serverWorld) {
            EnclosureArea a = ServerMain.INSTANCE.getSmallestEnclosure(serverWorld, pos);
            if (a != null && !a.hasPubPerm(Permission.EXPLOSION)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(
            method = "shouldDamage",
            at = @At("HEAD"),
            cancellable = true
    )
    private void shouldDamage(Explosion explosion, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity.getWorld() instanceof ServerWorld serverWorld) {
            EnclosureArea a = ServerMain.INSTANCE.getSmallestEnclosure(serverWorld, entity.getBlockPos());
            if (a != null && !a.hasPubPerm(Permission.EXPLOSION)) {
                cir.setReturnValue(false);
            }
        }
    }
}
