package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.utils.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.zly2006.enclosure.utils.Permission.permissions;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {
    public MixinLivingEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(at = @At("HEAD"), method = "damage", cancellable = true)
    private void damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (getWorld().isClient) {
            return;
        }
        if (Utils.isAnimal(this)) {
            if (!Utils.commonOnPlayerDamage(source, getBlockPos(), getWorld(), permissions.ATTACK_ANIMAL)) {
                cir.setReturnValue(false);
            }
        } else if (Utils.isMonster(this)) {
            if (!Utils.commonOnPlayerDamage(source, getBlockPos(), getWorld(), permissions.ATTACK_MONSTER)) {
                cir.setReturnValue(false);
            }
        } else if (getType() == EntityType.VILLAGER) {
            if (!Utils.commonOnPlayerDamage(source, getBlockPos(), getWorld(), permissions.ATTACK_VILLAGER)) {
                cir.setReturnValue(false);
            }
        } else {
            if (!Utils.commonOnPlayerDamage(source, getBlockPos(), getWorld(), permissions.ATTACK_ENTITY)) {
                cir.setReturnValue(false);
            }
        }
    }
}
