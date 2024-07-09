package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.entity.SculkCatalystBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SculkCatalystBlockEntity.Listener.class)
public class MixinSculkCatalystBlockEntity  {
    @Inject(method = "listen", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/SculkSpreadManager;spread(Lnet/minecraft/util/math/BlockPos;I)V"), cancellable = true)
    private void spread(ServerWorld world, RegistryEntry<GameEvent> event, GameEvent.Emitter emitter, Vec3d emitterPos, CallbackInfoReturnable<Boolean> cir, @Local LivingEntity livingEntity) {
        if (livingEntity.getWorld().isClient) {
            return;
        }
        BlockPos blockPos = BlockPos.ofFloored(emitterPos.offset(Direction.UP, 0.5));
        EnclosureArea area = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) livingEntity.getWorld(), blockPos);
        if (area != null && !area.hasPubPerm(Permission.SCULK_SPREAD)) {
            cir.setReturnValue(false);
        }
    }
}
