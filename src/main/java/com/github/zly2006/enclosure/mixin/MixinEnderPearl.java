package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderPearlEntity.class)
public abstract class MixinEnderPearl extends ThrownItemEntity {
    public MixinEnderPearl(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void onLand(HitResult hitResult, CallbackInfo ci) {
        if (world instanceof ServerWorld serverWorld && getOwner() instanceof ServerPlayerEntity player) {
            EnclosureArea area = ServerMain.INSTANCE.getSmallestEnclosure(serverWorld, getBlockPos());
            if (area == null)
                return;
            if (!area.hasPerm(player, Permission.TELEPORT)) {
                player.sendMessage(Permission.TELEPORT.getNoPermissionMsg(player));
                ci.cancel();
                discard();
            }
        }
    }
}
