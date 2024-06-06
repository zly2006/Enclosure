package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import com.github.zly2006.enclosure.utils.RayCast;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.HopperMinecartEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HopperMinecartEntity.class)
public abstract class MixinHopperMinecartEntity extends AbstractMinecartEntity {
    @Unique Vec3d lastPosition;
    @Unique BlockPos lastBlockPos;
    public MixinHopperMinecartEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onMoveStart(CallbackInfo ci) {
        lastPosition = getPos();
        lastBlockPos = getBlockPos();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void onMoveEnd(CallbackInfo ci) {
        if (getWorld() instanceof ServerWorld world) {
            EnclosureArea a1 = ServerMain.INSTANCE.getSmallestEnclosure(world, lastBlockPos);
            EnclosureArea a2 = ServerMain.INSTANCE.getSmallestEnclosure(world, getBlockPos());
            if (a1 != a2) {
                RayCast rayCast = new RayCast(lastPosition, getPos());
                if (a1 != null && !a1.hasPubPerm(Permission.CONTAINER)) {
                    if (rayCast.intersect(a1.toBox()) != null) {
                        setVelocity(Vec3d.ZERO);
                        refreshPositionAfterTeleport(lastPosition);
                    }
                }
                if (a2 != null && !a2.hasPubPerm(Permission.CONTAINER)) {
                    if (rayCast.intersect(a2.toBox()) != null) {
                        setVelocity(Vec3d.ZERO);
                        refreshPositionAfterTeleport(lastPosition);
                    }
                }
            }
        }
    }
}
