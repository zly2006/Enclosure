package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.EnclosureList;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.fabricmc.api.Environment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static net.fabricmc.api.EnvType.SERVER;

@Environment(SERVER)
@Mixin(EnderDragonEntity.class)
public class MixinEnderDragonEntity extends MobEntity {
    public MixinEnderDragonEntity(EntityType<? extends EnderDragonEntity> entityType, World world) {
        super(entityType, world);
    }

    @Redirect(method = "destroyBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    private boolean dragonProtection(World instance, BlockPos pos, boolean move) {
        if (instance.isClient) {
            return false;
        }
        EnclosureList list = ServerMain.INSTANCE.getAllEnclosures((ServerWorld) world);
        EnclosureArea a = list.getArea(pos);
        if (a != null && !a.areaOf(pos).hasPubPerm(Permission.DRAGON_DESTROY)) {
            return true;
        }
        return this.world.removeBlock(pos, false);
    }
}
