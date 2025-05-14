package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Leashable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Shadow public abstract BlockPos getBlockPos();

    @Shadow public abstract World getWorld();

    @Inject(
            method = "interact",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Leashable;getLeashHolder()Lnet/minecraft/entity/Entity;"
            ),
            cancellable = true
    )
    private void canBeDetachLeash(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (this instanceof Leashable) checkLeashPermission(player).ifPresent(cir::setReturnValue);
    }

    @SuppressWarnings("UnreachableCode")
    @Inject(
            method = "interact",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Leashable;canLeashAttachTo()Z"
            ),
            cancellable = true
    )
    private void canBeLeashedBy(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (this instanceof Leashable) checkLeashPermission(player).ifPresent(cir::setReturnValue);
    }

    @Unique
    private Optional<ActionResult> checkLeashPermission(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            if (!ServerMain.INSTANCE.checkPermission(getWorld(), getBlockPos(), serverPlayer, Permission.LEASH)) {
                serverPlayer.networkHandler.sendPacket(new EntityAttachS2CPacket((Entity) (Object) this, null));
                serverPlayer.sendMessage(Permission.LEASH.getNoPermissionMsg(serverPlayer), ServerMain.INSTANCE.getCommonConfig().useActionBarMessage);
                serverPlayer.currentScreenHandler.syncState();
                return Optional.of(ActionResult.PASS);
            }
        }

        return Optional.empty();
    }
}
