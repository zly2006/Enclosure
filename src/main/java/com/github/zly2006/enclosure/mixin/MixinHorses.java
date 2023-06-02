package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import com.github.zly2006.enclosure.utils.UtilsKt;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HorseBaseEntity.class)
public abstract class MixinHorses extends AnimalEntity {
    protected MixinHorses(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "openInventory", at = @At("HEAD"), cancellable = true)
    private void onOpenInventory(PlayerEntity player, CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            EnclosureArea area = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) this.getWorld(), getBlockPos());
            if (area != null && !area.hasPerm(serverPlayer, Permission.CONTAINER)) {
                UtilsKt.sendMessage(serverPlayer, Permission.CONTAINER.getNoPermissionMsg(serverPlayer));
                ci.cancel();
            }
        }
    }
}
