package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.zly2006.enclosure.utils.Permission.CONTAINER;
import static com.github.zly2006.enclosure.utils.Permission.VEHICLE;

@Mixin(ChestBoatEntity.class)
public class MixinChestBoatEntity extends BoatEntity {
    public MixinChestBoatEntity(EntityType<? extends BoatEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "canPlayerUse", at = @At("HEAD"), cancellable = true)
    private void canPlayerUse(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            EnclosureArea area = ServerMain.INSTANCE.getAllEnclosures((ServerWorld) this.getWorld()).getArea(getBlockPos());
            if (area != null && !area.areaOf(getBlockPos()).hasPerm(serverPlayer, Permission.CONTAINER)) {
                serverPlayer.sendMessage(CONTAINER.getNoPermissionMsg(serverPlayer));
                cir.setReturnValue(false);
            }
            if (area != null && !area.areaOf(getBlockPos()).hasPerm(serverPlayer, Permission.VEHICLE)) {
                serverPlayer.sendMessage(VEHICLE.getNoPermissionMsg(serverPlayer));
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void onInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!ServerMain.INSTANCE.checkPermission(getWorld(), getBlockPos(), player, VEHICLE)) {
            player.sendMessage(VEHICLE.getNoPermissionMsg(player));
            cir.setReturnValue(ActionResult.FAIL);
        }
        if (!ServerMain.INSTANCE.checkPermission(getWorld(), getBlockPos(), player, CONTAINER)) {
            player.sendMessage(CONTAINER.getNoPermissionMsg(player));
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
