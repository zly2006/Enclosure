package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.utils.Permission;
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

import static com.github.zly2006.enclosure.utils.Permission.*;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {
    public MixinLivingEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(at = @At("HEAD"), method = "damage", cancellable = true)
    private void damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (getEntityWorld().isClient) {
            return;
        }
        Permission permission = ATTACK_ENTITY;
        if (Utils.isAnimal(this)) {
            permission = ATTACK_ANIMAL;
        } else if (Utils.isMonster(this)) {
            permission = ATTACK_MONSTER;
        } else if (getType() == EntityType.VILLAGER) {
            permission = ATTACK_VILLAGER;
        }

        if (!Utils.commonOnPlayerDamage(source, getBlockPos(), getEntityWorld(), permission)) {
            cir.setReturnValue(false);
        }
    }
}
