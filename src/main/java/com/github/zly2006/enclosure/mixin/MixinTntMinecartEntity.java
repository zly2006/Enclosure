package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.utils.Utils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.zly2006.enclosure.utils.Permission.permissions;

@Mixin(TntMinecartEntity.class)
public abstract class MixinTntMinecartEntity extends AbstractMinecartEntity {
    protected MixinTntMinecartEntity(EntityType<?> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!Utils.commonOnDamage(source, getBlockPos(), getWorld(), permissions.VEHICLE)) {
            cir.setReturnValue(false);
        }
    }
}
