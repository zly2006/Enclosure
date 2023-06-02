package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.utils.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingBobberEntity.class)
public abstract class MixinFishingBobber extends ProjectileEntity {
    public MixinFishingBobber(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "pullHookedEntity", at = @At("HEAD"), cancellable = true)
    private void onPullEntity(Entity entity, CallbackInfo ci) {
        System.out.println("pullHookedEntity");
        if (getOwner() instanceof ServerPlayerEntity player) {
            if (!Utils.commonOnDamage(DamageSource.mobProjectile(this, player), entity)) {
                ci.cancel();
            }
        }
    }
}
