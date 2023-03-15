package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.utils.Permission;
import com.github.zly2006.enclosure.utils.Utils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrameEntity.class)
public abstract class MixinItemFrameEntity extends AbstractDecorationEntity {
    protected MixinItemFrameEntity(EntityType<? extends AbstractDecorationEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!Utils.commonOnDamage(source, getBlockPos(), getWorld(), Permission.BREAK_BLOCK)) {
            cir.setReturnValue(false);
        }
    }
}
