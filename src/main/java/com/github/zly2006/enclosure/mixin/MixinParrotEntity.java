package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.zly2006.enclosure.utils.Permission.PARROT_COOKIE;

@Mixin(ParrotEntity.class)
public abstract class MixinParrotEntity extends AnimalEntity {
    @Shadow
    @Final
    private static Item COOKIE;

    protected MixinParrotEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(at = @At("HEAD"), method = "interactMob", cancellable = true)
    private void interactMob(PlayerEntity p, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (p instanceof ServerPlayerEntity player) {
            if (player.getStackInHand(hand).isOf(COOKIE)) {
                EnclosureArea area = ServerMain.INSTANCE.getAllEnclosures((ServerWorld) getWorld()).getArea(getBlockPos());

                if (area != null && !area.areaOf(getBlockPos()).hasPerm(player, PARROT_COOKIE)) {
                    player.sendMessage(PARROT_COOKIE.getNoPermissionMsg(player));
                    cir.setReturnValue(ActionResult.FAIL);
                }
            }
        }
    }
}
