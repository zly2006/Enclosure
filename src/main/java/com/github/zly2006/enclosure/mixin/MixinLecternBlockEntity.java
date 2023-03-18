package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.access.LecternInventoryAccess;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LecternBlockEntity.class)
public class MixinLecternBlockEntity extends BlockEntity {
    @Final
    @Mutable
    @Shadow
    private Inventory inventory;

    public MixinLecternBlockEntity(BlockEntityType<?> type) {
        super(type);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.inventory = new LecternInventoryAccess(inventory, pos);
    }
}
