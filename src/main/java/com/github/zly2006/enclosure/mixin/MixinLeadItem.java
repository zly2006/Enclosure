package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.LeadItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.zly2006.enclosure.utils.Permission.PLACE_BLOCK;

@Mixin(LeadItem.class)
public class MixinLeadItem {
    @Inject(method = "attachHeldMobsToBlock", at = @At("HEAD"), cancellable = true)
    private static void onAttachHeldMobsToBlock(PlayerEntity player, World world, BlockPos pos, CallbackInfoReturnable<ActionResult> cir) {
        if (!ServerMain.INSTANCE.checkPermission(world, pos, player, PLACE_BLOCK)) {
            player.sendMessage(PLACE_BLOCK.getNoPermissionMsg(player));
            cir.setReturnValue(ActionResult.PASS);
        }
    }
}
