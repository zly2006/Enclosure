package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.entity.Bucketable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Bucketable.class)
public interface MixinTadpoleEntity {
    @Inject(method = "tryBucket", at = @At("HEAD"), cancellable = true)
    private static <T extends LivingEntity & Bucketable>  void interactMob(PlayerEntity player, Hand hand, T entity, CallbackInfoReturnable<Optional<ActionResult>> cir) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            if (!ServerMain.INSTANCE.checkPermission(player.getWorld(), entity.getBlockPos(), player, Permission.FISH)) {
                player.sendMessage(Permission.FISH.getNoPermissionMsg(player));
                serverPlayer.networkHandler.sendPacket(entity.createSpawnPacket(serverPlayer.getServerWorld().getChunkManager().chunkLoadingManager.entityTrackers.get(entity.getId()).entry));
                serverPlayer.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(entity.getId(), entity.getDataTracker().getChangedEntries()));
                cir.setReturnValue(Optional.empty());
            }
        }
    }
}
