package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Shadow public abstract BlockPos getBlockPos();

    @Shadow public abstract World getWorld();

    @SuppressWarnings("UnreachableCode")
    @Inject(
            method = "interact",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Leashable;canLeashAttachTo()Z"
            ),
            cancellable = true
    )
    private void canBeLeashedBy(PlayerEntity pe, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (pe instanceof ServerPlayerEntity player) {
            if (!ServerMain.INSTANCE.checkPermission(getWorld(), getBlockPos(), player, Permission.LEASH)) {
                player.networkHandler.sendPacket(new EntityAttachS2CPacket((Entity) (Object) this, null));
                player.sendMessage(Permission.LEASH.getNoPermissionMsg(player));
                player.currentScreenHandler.syncState();
                cir.setReturnValue(ActionResult.PASS);
            }
        }
    }
}
