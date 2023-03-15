package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.utils.Permission;
import com.github.zly2006.enclosure.utils.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndCrystalEntity.class)
public abstract class MixinEndCrystalEntity extends Entity {
    public MixinEndCrystalEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!Utils.commonOnDamage(source, getBlockPos(), getWorld(), Permission.BREAK_BLOCK)) {
            cir.setReturnValue(false);
        }
    }
}
