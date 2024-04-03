package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherEntity.class)
public abstract class MixinWitherEntity extends Entity {
    public MixinWitherEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "mobTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;breakBlock(Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/entity/Entity;)Z"), cancellable = true)
    private void onBreakBlock(CallbackInfo ci, @Local BlockPos blockPos) {
        if (getWorld().isClient) {
            return;
        }
        EnclosureArea a = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) getWorld(), blockPos);
        if (a == null) return;
        if (!a.hasPubPerm(Permission.WITHER_DESTROY)) {
            // prevent breaking block
            ci.cancel();
        }
    }
    @Inject(method = "mobTick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        if (getWorld().isClient) {
            return;
        }
        EnclosureArea a = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) getWorld(), getBlockPos());
        if (a == null) return;
        if (!a.hasPubPerm(Permission.WITHER_ENTER)) {
            // prevent entering enclosure
            discard();
            ci.cancel();
        }
    }
}
