package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.EnclosureList;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SculkCatalystBlockEntity;
import net.minecraft.block.entity.SculkSpreadManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SculkCatalystBlockEntity.class)
public class MixinSculkCatalystBlockEntity extends BlockEntity {
    public MixinSculkCatalystBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Redirect(method = "listen", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/SculkSpreadManager;spread(Lnet/minecraft/util/math/BlockPos;I)V"))
    private void spread(SculkSpreadManager instance, BlockPos pos, int charge) {
        if (getWorld() == null || getWorld().isClient) {
            // 爱咋咋地，不改变行为
            instance.spread(pos, charge);
        }
        EnclosureList list = ServerMain.INSTANCE.getAllEnclosures((ServerWorld) getWorld());
        EnclosureArea area = list.getArea(pos);
        if (area == null || area.areaOf(pos).hasPubPerm(Permission.SCULK_SPREAD)) {
            instance.spread(pos, charge);
        }
    }
}
