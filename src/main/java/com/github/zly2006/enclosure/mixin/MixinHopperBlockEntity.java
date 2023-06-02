package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.command.EnclosureCommandKt;
import com.github.zly2006.enclosure.mixinadatper.MixinHopperKt;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.UUID;

@Mixin(HopperBlockEntity.class)
public class MixinHopperBlockEntity extends BlockEntity {
    public MixinHopperBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "extract(Lnet/minecraft/block/entity/Hopper;Lnet/minecraft/inventory/Inventory;ILnet/minecraft/util/math/Direction;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/HopperBlockEntity;canExtract(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/item/ItemStack;ILnet/minecraft/util/math/Direction;)Z"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private static void checkCanExtract(Hopper hopper, Inventory inventory, int slot, Direction side, CallbackInfoReturnable<Boolean> cir, ItemStack itemStack) {
        if (!MixinHopperKt.canExtractFromInventory(hopper, inventory)) {
            cir.setReturnValue(false);
        }
    }
    @Inject(method = "extract(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/entity/ItemEntity;)Z", at = @At("HEAD"), cancellable = true)
    private static void onExtract(Inventory inventory, ItemEntity itemEntity, CallbackInfoReturnable<Boolean> cir) {
        UUID owner = itemEntity.owner;
        if (owner != null && !owner.equals(EnclosureCommandKt.CONSOLE)) {
            cir.setReturnValue(false);
        }
    }
}
