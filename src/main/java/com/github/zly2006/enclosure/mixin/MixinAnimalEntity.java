package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.zly2006.enclosure.utils.Permission.FEED_ANIMAL;

@Mixin(AnimalEntity.class)
public abstract class MixinAnimalEntity extends Entity {
    public MixinAnimalEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/passive/AnimalEntity;getBreedingAge()I",
                    shift = At.Shift.BEFORE
            ),
            method = "interactMob",
            cancellable = true
    )
    private void onEating(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (getWorld().isClient) {
            return;
        }
        EnclosureArea area = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) this.getWorld(), getBlockPos());
        if (area != null && !area.hasPerm((ServerPlayerEntity) player, FEED_ANIMAL)) {
            player.sendMessage(FEED_ANIMAL.getNoPermissionMsg(player));
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
