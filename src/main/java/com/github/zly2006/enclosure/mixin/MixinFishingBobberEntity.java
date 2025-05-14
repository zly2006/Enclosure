package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingBobberEntity.class)
public abstract class MixinFishingBobberEntity extends ProjectileEntity {
    @Shadow @Nullable public abstract PlayerEntity getPlayerOwner();

    @Unique
    private int tick = 0;

    public MixinFishingBobberEntity(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(
            method = "tick",
            at = @At("RETURN")
    )
    public void onTick(CallbackInfo ci) {
        ++tick;
        if (tick % 10 != 0) return;  // 每10tick执行一次，减少判断的压力

        var owner = this.getPlayerOwner();
        if (!(owner instanceof ServerPlayerEntity)) return;
        if (ServerMain.INSTANCE.checkPermission((ServerPlayerEntity) owner, Permission.FISH, getBlockPos())) return;

        this.discard();
        owner.sendMessage(Permission.FISH.getNoPermissionMsg(owner), ServerMain.INSTANCE.getCommonConfig().useActionBarMessage);
    }
}
