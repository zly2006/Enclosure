package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import com.github.zly2006.enclosure.utils.Utils;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static com.github.zly2006.enclosure.ServerMain.Instance;

@Mixin(HopperBlockEntity.class)
public class MixinHopperBlockEntity {


    @Inject(method = "extract(Lnet/minecraft/block/entity/Hopper;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/HopperBlockEntity;getAvailableSlots(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/util/math/Direction;)Ljava/util/stream/IntStream;"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private static void onExtract(Hopper hopper, CallbackInfoReturnable<Boolean> cir, Inventory inventory, Direction direction) {
        if (hopper.getWorld().isClient) return;
        BlockPos pos = Utils.toBlockPos(hopper.getHopperX(), hopper.getHopperY(), hopper.getHopperZ());
        BlockPos inventoryPos = pos.offset(Direction.UP);
        if (!ServerMain.checkPermissionInDifferentEnclosure((ServerWorld) hopper.getWorld(), pos, inventoryPos, Permission.CONTAINER)) {
            cir.setReturnValue(false);
        }
    }
    @Inject(method = "extract(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/entity/ItemEntity;)Z", at = @At("HEAD"), cancellable = true)
    private static void onExtract(Inventory inventory, ItemEntity itemEntity, CallbackInfoReturnable<Boolean> cir) {
        if (itemEntity.world.isClient) return;
        EnclosureArea area = Instance.getAllEnclosures((ServerWorld) itemEntity.world).getArea(itemEntity.getBlockPos());
        if (area != null && !area.areaOf(itemEntity.getBlockPos()).hasPubPerm(Permission.PICKUP_ITEM)) {
            cir.setReturnValue(false);
        }
    }
}
