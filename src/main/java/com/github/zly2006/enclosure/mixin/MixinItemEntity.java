package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.access.PlayerAccess;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class MixinItemEntity extends Entity {
    public MixinItemEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void onPlayerCollision(PlayerEntity player, CallbackInfo ci) {
        if (world instanceof ServerWorld serverWorld) {
            EnclosureArea area = ServerMain.INSTANCE.getAllEnclosures(serverWorld).getArea(getBlockPos());
            if (area != null && !area.areaOf(getBlockPos()).hasPerm((ServerPlayerEntity) player, Permission.PICKUP_ITEM)) {
                ((PlayerAccess) player).sendMessageWithCD(Permission.PICKUP_ITEM::getNoPermissionMsg);
                ci.cancel();
            }
        }
    }
}
