package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.zly2006.enclosure.utils.Permission.CONTAINER;

@Mixin(StorageMinecartEntity.class)
public abstract class MixinStorageMinecartEntity extends AbstractMinecartEntity {
    protected MixinStorageMinecartEntity(EntityType<?> entityType, World world) {
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
        }
    }
}
