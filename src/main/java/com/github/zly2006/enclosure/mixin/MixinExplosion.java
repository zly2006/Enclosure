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
import net.minecraft.world.explosion.ExplosionImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.List;

@Mixin(ExplosionImpl.class)
public abstract class MixinExplosion {
    @Shadow
    @Final
    private ServerWorld world;

    @Redirect(
            method = "getBlocksToDestroy",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/explosion/ExplosionBehavior;canDestroyBlock(Lnet/minecraft/world/explosion/Explosion;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;F)Z"
            )
    )
    private boolean protectBlock(ExplosionBehavior instance, Explosion explosion, BlockView world, BlockPos pos, BlockState state, float power) {
        EnclosureArea area = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) world, pos);
        return area == null || area.hasPubPerm(Permission.EXPLOSION);
    }

    @Inject(
            method = "damageEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            cancellable = true
    )
    private void protectEntities(CallbackInfo ci, float f, int i, int j, int k, int l, int m, int n, List<Object> list, Iterator<Object> var9, Entity entity, double d, double e, double g, double h, double o, boolean bl, float p, float q) {
        BlockPos pos = entity.getBlockPos();
        EnclosureArea area = ServerMain.INSTANCE.getSmallestEnclosure(world, pos);
        if (area != null && !area.hasPubPerm(Permission.EXPLOSION)) {
            ci.cancel();
        }
    }
}
