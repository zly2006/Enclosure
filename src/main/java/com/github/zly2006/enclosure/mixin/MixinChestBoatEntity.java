package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractChestBoatEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

import static com.github.zly2006.enclosure.utils.Permission.CONTAINER;
import static com.github.zly2006.enclosure.utils.Permission.VEHICLE;

@Mixin(AbstractChestBoatEntity.class)
public abstract class MixinChestBoatEntity extends Entity {


    public MixinChestBoatEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "canPlayerUse", at = @At("HEAD"), cancellable = true)
    private void canPlayerUse(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            if (!ServerMain.INSTANCE.checkPermission(getWorld(), getBlockPos(), player, CONTAINER)) {
                serverPlayer.sendMessage(CONTAINER.getNoPermissionMsg(serverPlayer));
                cir.setReturnValue(false);
            } else if (!ServerMain.INSTANCE.checkPermission(getWorld(), getBlockPos(), player, VEHICLE)) {
                serverPlayer.sendMessage(VEHICLE.getNoPermissionMsg(serverPlayer));
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void onInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!ServerMain.INSTANCE.checkPermission(getWorld(), getBlockPos(), player, VEHICLE)) {
            player.sendMessage(VEHICLE.getNoPermissionMsg(player), true);
            cir.setReturnValue(ActionResult.FAIL);
        } else if (!ServerMain.INSTANCE.checkPermission(getWorld(), getBlockPos(), player, CONTAINER)) {
            player.sendMessage(CONTAINER.getNoPermissionMsg(player), true);
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
