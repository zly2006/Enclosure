package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.EnclosureList;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(Explosion.class)
public abstract class MixinExplosion {
    @Shadow
    @Final
    private ObjectArrayList<BlockPos> affectedBlocks;

    @Shadow
    @Final
    private World world;

    @ModifyVariable(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;<init>(DDD)V", ordinal = 1), method = "collectBlocksAndDamageEntities")
    private List<Entity> protectEntities(List<Entity> list) {
        if (!world.isClient) {
            EnclosureList enclosureList = ServerMain.INSTANCE.getAllEnclosures((ServerWorld) world);
            list.removeIf(e -> {
                assert e != null;
                BlockPos pos = e.getBlockPos();
                EnclosureArea a = enclosureList.getArea(pos);
                return a != null && !a.areaOf(pos).hasPubPerm(Permission.EXPLOSION);
            });
            this.affectedBlocks.removeIf(pos -> {
                EnclosureArea a = enclosureList.getArea(pos);
                return a != null && !a.areaOf(pos).hasPubPerm(Permission.EXPLOSION);
            });
        }
        return list;
    }
}
