package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.EnclosureList;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(WitherEntity.class)
public abstract class MixinWitherEntity extends Entity {
    public MixinWitherEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "mobTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;breakBlock(Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/entity/Entity;)Z"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void tick(CallbackInfo ci, int i, int j, int k, boolean bl, int l, int m, int n, int o, int p, int q, BlockPos blockPos, BlockState blockState) {
        if (world.isClient) {
            return;
        }
        EnclosureList list = ServerMain.INSTANCE.getAllEnclosures((ServerWorld) world);
        EnclosureArea a = list.getArea(blockPos);
        if (a != null && !a.areaOf(blockPos).hasPubPerm(Permission.WITHER_DESTROY)) {
            // prevent breaking block
            ci.cancel();
        }
        a = list.getArea(getBlockPos());
        if (a != null && !a.areaOf(getBlockPos()).hasPubPerm(Permission.WITHER_ENTER)) {
            // prevent entering enclosure
            discard();
            ci.cancel();
        }
    }
}
