package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.fabricmc.api.EnvType.SERVER;

@Environment(SERVER)
@Mixin(value = FallingBlockEntity.class)
public abstract class MixinFallingBlockEntity extends Entity {
    @Shadow
    @Final
    protected static TrackedData<BlockPos> BLOCK_POS;

    public MixinFallingBlockEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow
    public abstract BlockPos getFallingBlockPos();

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"), method = "tick", cancellable = true)
    private void protectFallingBlocks(CallbackInfo ci) {
        if (world.isClient) {
            return;
        }
        if (ServerMain.INSTANCE.getAllEnclosures((ServerWorld) world).getArea(getBlockPos()) == null) {
            // not in any residence, do nothing
            return;
        }
        if (!ServerMain.INSTANCE.checkPermissionInDifferentEnclosure((ServerWorld) world, getFallingBlockPos(), getBlockPos(), Permission.FALLING_BLOCK)) {
            discard();
            ci.cancel();
        }
    }
}
