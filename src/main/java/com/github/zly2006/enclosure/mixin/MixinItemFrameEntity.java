package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import com.github.zly2006.enclosure.utils.Utils;
import com.github.zly2006.enclosure.utils.UtilsKt;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrameEntity.class)
public abstract class MixinItemFrameEntity extends AbstractDecorationEntity {
    protected MixinItemFrameEntity(EntityType<? extends AbstractDecorationEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void onUse(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            EnclosureArea area = ServerMain.INSTANCE.getSmallestEnclosure(serverPlayer.getWorld(), getBlockPos());
            if (area != null && !area.areaOf(getBlockPos()).hasPerm(serverPlayer, Permission.ITEM_FRAME)) {
                UtilsKt.sendMessage(player, Permission.ITEM_FRAME.getNoPermissionMsg(serverPlayer));
                cir.setReturnValue(ActionResult.FAIL);
            }
        }
    }

    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/decoration/ItemFrameEntity;dropHeldStack(Lnet/minecraft/entity/Entity;Z)V"), cancellable = true)
    private void onDamages(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!Utils.commonOnDamage(source, getBlockPos(), getWorld(), Permission.ITEM_FRAME)) {
            cir.setReturnValue(false);
        }
    }
}
