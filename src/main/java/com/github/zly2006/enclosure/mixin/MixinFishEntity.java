package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.zly2006.enclosure.ServerMain.Instance;

@Mixin(FishEntity.class)
public abstract class MixinFishEntity extends Entity {
    public MixinFishEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void interact(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (this.isAlive() && player.getStackInHand(hand).getItem()== Items.WATER_BUCKET) {
            if (!Instance.checkPermission(getEntityWorld(), getBlockPos(), player, Permission.FISH)) {
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                serverPlayer.networkHandler.sendPacket(createSpawnPacket());
                serverPlayer.sendMessage(Permission.FISH.getNoPermissionMsg(player),false);
                serverPlayer.refreshScreenHandler(serverPlayer.currentScreenHandler);
                cir.setReturnValue(ActionResult.FAIL);
            }
        }
    }
}
