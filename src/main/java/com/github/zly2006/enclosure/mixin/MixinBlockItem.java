package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.zly2006.enclosure.utils.Permission.permissions;

@Mixin(BlockItem.class)
public class MixinBlockItem {
    @Inject(method = "canPlace", at = @At("HEAD"), cancellable = true)
    private void canPlace(ItemPlacementContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (context.getPlayer() instanceof ServerPlayerEntity serverPlayer) {
            if (!ServerMain.INSTANCE.checkPermission(serverPlayer, permissions.PLACE_BLOCK, context.getBlockPos())) {
                serverPlayer.sendMessage(permissions.PLACE_BLOCK.getNoPermissionMsg(serverPlayer));
                cir.setReturnValue(false);
            }
        }
    }
}
