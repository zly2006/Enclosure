package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FallingBlockEntity.class)
public abstract class MixinFallingBlockEntity extends Entity {
    public MixinFallingBlockEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow
    public abstract BlockPos getFallingBlockPos();

    @Inject(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"
            ),
            method = "tick",
            cancellable = true
    )
    private void protectFallingBlocks(CallbackInfo ci) {
        if (getWorld().isClient) {
            return;
        }
        EnclosureArea currentArea = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) getWorld(), getBlockPos());
        EnclosureArea sourceArea = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) getWorld(), getFallingBlockPos());
        if (currentArea == null || sourceArea == currentArea) {
            return;
        }
        if (!currentArea.hasPubPerm(Permission.FALLING_BLOCK)) {
            discard();
            ci.cancel();
        }
    }
}
