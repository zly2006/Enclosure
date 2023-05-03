package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.EnclosureList;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.zly2006.enclosure.utils.Permission.USE_CAMPFIRE;
import static net.minecraft.block.CampfireBlock.WATERLOGGED;

@Mixin(CampfireBlock.class)
public class MixinCampfireBlock {

    @Shadow @Final public static BooleanProperty LIT;

    @Inject(at = @At("HEAD"), method = "extinguish", cancellable = true)
    private static void onExtinguish(Entity entity, WorldAccess world, BlockPos pos, BlockState state, CallbackInfo ci){
        if(world instanceof ServerWorld){
            EnclosureList list = ServerMain.INSTANCE.getAllEnclosures((ServerWorld) world);
            EnclosureArea area = list.getArea(pos);
            if (area != null && !area.areaOf(pos).hasPubPerm(Permission.USE_CAMPFIRE)) {
                if(entity instanceof ServerPlayerEntity player){
                    player.sendMessage(USE_CAMPFIRE.getNoPermissionMsg(player));
                }

                world.setBlockState(pos, state.with(WATERLOGGED, false).with(LIT, true), 252);
                world.emitGameEvent(entity, GameEvent.BLOCK_CHANGE, pos);

                ci.cancel();
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "tryFillWithFluid", cancellable = true)
    private void onFillWithFluid(WorldAccess world, BlockPos pos, BlockState state, FluidState fluidState, CallbackInfoReturnable<Boolean> cir){
        if(world instanceof ServerWorld){
            EnclosureList list = ServerMain.INSTANCE.getAllEnclosures((ServerWorld) world);
            EnclosureArea area = list.getArea(pos);
            if (area != null && !area.areaOf(pos).hasPubPerm(Permission.USE_CAMPFIRE)) {
                cir.setReturnValue(false);
            }
        }
    }
}
