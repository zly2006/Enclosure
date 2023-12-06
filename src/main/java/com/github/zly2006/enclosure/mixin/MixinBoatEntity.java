package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.zly2006.enclosure.utils.Permission.VEHICLE;

@Mixin(BoatEntity.class)
public abstract class MixinBoatEntity extends Entity {
    protected MixinBoatEntity(EntityType<?> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void onInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!ServerMain.INSTANCE.checkPermission(getWorld(), getBlockPos(), player, VEHICLE)) {
            player.sendMessage(VEHICLE.getNoPermissionMsg(player));
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
