package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.explosion.ExplosionImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ExplosionImpl.class)
public abstract class MixinExplosion {

    @Shadow
    @Final
    private ServerWorld world;

    @ModifyExpressionValue(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/explosion/ExplosionImpl;getBlocksToDestroy()Ljava/util/List;"), method = "explode")
    private List<BlockPos> protectBlocks(List<BlockPos> list) {
        if (!world.isClient) {
            list.removeIf(pos -> {
                EnclosureArea a = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) world, pos);
                return a != null && !a.hasPubPerm(Permission.EXPLOSION);
            });
        }
        return list;
    }

    @ModifyExpressionValue(method = "damageEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;getOtherEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;)Ljava/util/List;"))
    private List<Entity> protectEntities(List<Entity> list) {
        if (!world.isClient) {
            list.removeIf(entity -> {
                assert entity != null;
                BlockPos pos = entity.getBlockPos();
                EnclosureArea a = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) world, pos);
                return a != null && !a.hasPubPerm(Permission.EXPLOSION);
            });
        }
        return list;
    }
}
