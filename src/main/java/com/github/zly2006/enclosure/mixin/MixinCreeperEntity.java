package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import com.github.zly2006.enclosure.utils.UtilsKt;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
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

@Mixin(CreeperEntity.class)
public class MixinCreeperEntity extends HostileEntity {
    protected MixinCreeperEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "interactMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/CreeperEntity;ignite()V"), cancellable = true)
    private void onUse(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        EnclosureArea area = ServerMain.INSTANCE.getSmallestEnclosure((ServerWorld) world, getBlockPos());
        if (area != null && !area.hasPerm((ServerPlayerEntity) player, Permission.PRIME_TNT)) {
            UtilsKt.sendMessage(player, Permission.PRIME_TNT.getNoPermissionMsg(player));
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
