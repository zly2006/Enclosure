package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.events.PlayerUseEntityEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stat;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.zly2006.enclosure.utils.Permission.PLACE_BLOCK;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends LivingEntity {
    @Shadow public abstract boolean isPlayer();

    @Shadow @Final private PlayerInventory inventory;

    @Shadow protected abstract void spawnParticles(ParticleEffect parameters);

    @Shadow public abstract void resetStat(Stat<?> stat);

    @Shadow public abstract void remove(RemovalReason reason);

    public MixinPlayerEntity(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void protectInteract(Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        EntityHitResult hitResult = new EntityHitResult(entity);
        ActionResult result = PlayerUseEntityEvent.onPlayerUseEntity(inventory.player, getWorld(), hand, entity, hitResult);
        if (result != ActionResult.PASS) {
            cir.setReturnValue(result);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "canPlaceOn", at = @At("HEAD"), cancellable = true)
    private void protectPlacing(BlockPos pos, Direction facing, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (((LivingEntity) this) instanceof ServerPlayerEntity serverPlayer) {
            if (!ServerMain.INSTANCE.checkPermission(serverPlayer, PLACE_BLOCK, pos)) {
                serverPlayer.sendMessage(PLACE_BLOCK.getNoPermissionMsg(serverPlayer));
                cir.setReturnValue(false);
            }
        }
    }
}
