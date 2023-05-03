package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.EnclosureList;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.block.WitherSkullBlock;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherSkullBlock.class)
public class MixinWitherSkullBlock {
    @Inject(at = @At("HEAD"), method = "onPlaced(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/SkullBlockEntity;)V", cancellable = true)
    private static void onPlace(World world, BlockPos pos, SkullBlockEntity blockEntity, CallbackInfo ci) {
        if (world.isClient) {
            return;
        }
        EnclosureList list = ServerMain.INSTANCE.getAllEnclosures((ServerWorld) world);
        EnclosureArea a = list.getArea(pos);
        if (a != null && !a.areaOf(pos).hasPubPerm(Permission.WITHER_SPAWN)) {
            ci.cancel();
        }
    }
}
