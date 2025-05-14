package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.BlockAttachedEntity;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.zly2006.enclosure.utils.Permission.LEASH;

@Mixin(LeashKnotEntity.class)
public abstract class MixinLeashKnotEntity extends BlockAttachedEntity {
    protected MixinLeashKnotEntity(EntityType<? extends BlockAttachedEntity> type, World world, BlockPos attachedBlockPos) {
        super(type, world, attachedBlockPos);
    }

    @Inject(
            method = "interact",
            at = @At("HEAD"),
            cancellable = true
    )
    public void onInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (this.getWorld().isClient) return;

        if (!ServerMain.INSTANCE.checkPermission(this.getWorld(), this.attachedBlockPos, player, LEASH)) {
            player.sendMessage(LEASH.getNoPermissionMsg(player), ServerMain.INSTANCE.getCommonConfig().useActionBarMessage);
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
