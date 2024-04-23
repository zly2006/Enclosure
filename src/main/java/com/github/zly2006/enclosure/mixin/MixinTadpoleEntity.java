package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.TadpoleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.zly2006.enclosure.utils.Permission.permissions;

@Mixin(TadpoleEntity.class)
public abstract class MixinTadpoleEntity extends Entity {
    public MixinTadpoleEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void interactMob(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (ServerMain.INSTANCE.checkPermission(getWorld(), getBlockPos(), player, permissions.FISH)) {
            return;
        }
        player.sendMessage(permissions.FISH.getNoPermissionMsg(player));
        cir.setReturnValue(ActionResult.FAIL);
    }
}
